# ARCHITECTURE.md — Arquitetura proposta para o novo backend

## Recorte implementado em 16/10/2026 (fase 1)

Existem apenas `com.kaup.KaupApplication`, `analysis.HealthController`, `security.SecurityConfig`,
`common.web.RequestIdFilter` e `common.error` (DTO, advice e fallback servlet). Health não recebe services
OCR/IA vazios. Security mantém CSRF e exige autenticação fora do GET de health, sem usuário/senha gerados
ou fluxo provisório de login. Auth/sessão/ownership reais são a próxima fase.

Flyway V1 cria somente `users`, sem seeds. JPA inicializa com validate/open-in-view=false; não há entidades
de negócio ainda, portanto a integridade dessa tabela é verificada por testes SQL em PostgreSQL real.
Profiles dev/prod e Compose estão descritos no RUNBOOK. Sem Actuator ou diagnóstico de dependências público.

O desenho abaixo é o **alvo das próximas fases**, não um inventário de classes já presentes. Somente
criar services, interfaces e adapters quando houver caso de uso executável. Regras específicas de cada
funcionalidade ficam no seu pacote; persistência não acessa providers; DTOs não serializam entidades.
Workers usarão casos de uso de analysis/documents, com transações curtas e I/O externo fora da transação.

## 1. Visão geral

**Monólito modular Java/Spring Boot**, em `backend-novo/`, que implementa API REST para o SPA React. PostgreSQL gerencia metadados, sessão/contas, análise e chat. Storage guarda arquivos fora da raiz pública. OCR extrai texto; Gemini recebe somente o contexto permitido, por adapter server-side.

```text
frontend-novo/ (React + Vite)
       │ HTTPS/JSON + multipart
       ▼
backend-novo/ (Spring Boot API)
       ├── auth + security + profile ── PostgreSQL
       ├── documents (upload, status, CRUD) ── PostgreSQL + storage privado
       ├── analysis (workflow) ── text extraction/OCR ── AI adapter Gemini
       └── chat (documento + histórico + IA) ── PostgreSQL + AI adapter
```

## 2. Organização de pacotes sugerida

```text
backend-novo/
├── pom.xml
├── Dockerfile
├── compose.yaml
├── .env.example
└── src/
    ├── main/
    │   ├── java/com/kaup/
    │   │   ├── KaupApplication.java
    │   │   ├── auth/           # controller, service, DTO, usuário/sessão
    │   │   ├── documents/      # controller, service, DTO, entity, repository
    │   │   ├── analysis/       # pipeline, entity, DTO, status e erro
    │   │   ├── chat/           # controller, history, context builder, AI chat
    │   │   ├── profile/        # preferências do usuário
    │   │   ├── ai/             # interface, Gemini adapter, schemas/prompts
    │   │   ├── ocr/            # interface + PDF/Tesseract adapters
    │   │   ├── storage/        # interface + armazenamento local privado
    │   │   ├── security/       # auth config, filtros, acesso por owner
    │   │   └── common/         # erros padronizados, clock, requests IDs
    │   └── resources/
    │       ├── application.yml
    │       └── db/migration/  # Flyway SQL versionado
    └── test/
        ├── java/com/kaup/
        └── resources/fixtures/  # somente contratos de exemplo sintéticos
```

Reorganize se melhorar o projeto, mas registre em `DECISIONS.md`. Evite arquiteturas cerimoniais: não criar DTO/service/interface para operações triviais sem benefício testável.

## 3. Responsabilidades claras

- **Controller:** rota HTTP, Bean Validation, status/cabeçalhos, chamar caso de uso e mapear resposta. Nunca chamar Gemini ou Tesseract diretamente.
- **Service/use case:** validar permissões e executar regra de negócio/transições de status; manter idempotência e consistência.
- **Repository JPA:** persistência e consultas filtradas por usuário/documento. Entidades não são serializadas diretamente no JSON.
- **DTO/mapper:** tradução estável HTTP ↔ domínio; DTO não expõe senhas, nomes de paths, texto OCR completo ou segredo.
- **Storage:** bytes fora do banco por padrão, identificadores opacos, limite e exclusão física.
- **TextExtractor:** texto de PDF digital e OCR controlado com timeout/limites, retornando texto + avisos/metadados de qualidade.
- **ContractAiClient:** IA de análise e de perguntas separadas, parâmetros configuráveis, timeout, limites de contexto, resultados validados. Modelo e provider substituíveis.
- **ChatContextBuilder:** buscar documento/análise/trechos e preferências do usuário, limitar tamanho e preservar evidência por cláusula/página.

## 4. Fluxo de upload assíncrono

```text
POST /api/documentos (autenticado)
    -> validar tamanho/formato/conteúdo
    -> armazenar arquivo com chave opaca
    -> persistir Document(status=pending)
    -> devolver 202 + DocumentDTO
    -> executor limitado/agendador/worker interno (durável)
         -> reivindicar job status=processing
         -> extrair PDF/OCR
         -> validar resultado não vazio
         -> solicitar análise estruturada ao Gemini
         -> validar resposta estruturada; armazenar análise
         -> atualizar Document(status=completed ou failed)
GET /api/documentos/{id}/analise
    -> verificar owner
    -> devolver status + análise ou aviso
```

**Durabilidade:** `@Async` puro com tarefa apenas em memória perde trabalho na reinicialização. Preferir fila transacional simples em tabela/estado e executor limitado que recupera tarefas `pending`/`processing` abandonadas, com lease/retry criterioso; para MVP não é necessário RabbitMQ/Kafka. Um único worker configurável basta. Não chamar provider com transação longa aberta. Evitar múltiplos processamentos simultâneos do mesmo documento.

**Requisições HTTP:** devolver status rapidamente; frontend consulta a cada intervalo. Garantir que `GET /analise` retorne formato válido durante pending/processing/failed. Em cancelamento/exclusão, impedir job órfão de ressuscitar documento.

## 5. Fluxo de chat

```text
POST /api/documentos/{id}/chat/mensagens
    -> autenticação + autorização de acesso ao documentId
    -> validar tamanho da pergunta / quota
    -> obter texto OCR/análise + trechos relevantes + preferências autorizadas
    -> preparar contexto não confiável delimitado (sem obedecer instruções do contrato)
    -> enviar ao Gemini via adapter
    -> verificar resposta plausível e armazenar turno em ordem
    -> devolver ChatMessageDTO role=assistant
```

Não enviar conteúdo de outro documento/usuário. Para contratos grandes, seleção/chunking simples e rastreabilidade por página; banco vetorial só se necessidade medida. Mensagens do usuário/assistente devem ser gravadas como um turno coerente e identificável; retries não devem duplicar desnecessariamente a mensagem. Caso o modelo não encontre a cláusula, a resposta deve declarar ausência de evidência. Não interpretar output da IA como comando de infraestrutura.

## 6. Infraestrutura e configuração

- Desenvolvimento: `backend-novo/compose.yaml` sobe PostgreSQL + API, portas 5432 restrita ao loopback se exposta, 8080 para frontend; serviço frontend inicia separadamente com Vite 5174.
- Docker multi-stage Maven/JRE Java 17; processo não-root, imagens e dependências versionadas, health probes, volumes persistentes com permissões apropriadas.
- `application.yml` com `SPRING_DATASOURCE_URL`, user/password, `GEMINI_API_KEY`, `GEMINI_MODEL`, limites de upload e paths. `.env.example` sem segredos.
- Flyway como única fonte de esquema. Em ambiente compartilhado, `ddl-auto=validate`, não `update`; migrations cumulativas, rollback documentado.
- Não usar `DATABASE_URL=postgres://...` sem adaptador explícito se Spring espera JDBC `jdbc:postgresql://...`.
- OpenAPI/Swagger útil em desenvolvimento, proteger ou desabilitar em produção quando necessário.

## 7. Limites de escopo

A branch do colega `naoto_back_BD` iniciou um `AnaliseController`, uso externo de OCR e configuração Tess4J. São referências opcionais, **não módulos comprovadamente funcionais**. Não copiar o controller com erros, nem depender de DTOs ainda ausentes.

**Critério de design:** cada decisão deve resultar em código mais simples de testar, depurar e integrar ao `frontend-novo/` sem efeitos colaterais escondidos.
