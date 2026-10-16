# AGENTS.md — Instruções para agentes de código | Kaup Backend

> Leia este documento **antes de qualquer edição**. Estas instruções se aplicam ao trabalho iniciado em `backend-novo/` na branch experimental `dede_backend`.

## 1. Missão

Construir um **MVP de backend real, executável, seguro em nível adequado ao contexto acadêmico e integrado** ao `frontend-novo/` do Kaup, substituindo os mocks quando a opção de API estiver ativada. Implementar Java/Spring Boot, PostgreSQL, leitura/OCR, análise com Gemini, chat contextualizado, documentos e perfil, com testes automatizados. Não afirmar que é um produto pronto para produção.

O projeto atende pessoas com dificuldade de entender contratos e termos jurídicos. Respostas devem usar português brasileiro claro e nunca se apresentar como aconselhamento jurídico definitivo.

## 2. Fontes obrigatórias — ordem de prioridade

1. **Código funcional atual:** `../frontend-novo/src/services/contracts/KaupService.ts`, `../frontend-novo/src/services/api/{ApiService.ts,endpoints.ts,dto.ts,http.ts}`, `../frontend-novo/src/types/domain.ts`, `../frontend-novo/src/features/chat/` e `../frontend-novo/vite.config.ts`. **Leia o estado presente na branch**, pois pode ter mudado depois de gerar estes documentos.
2. **Requisitos do produto:** `../docs/Concepção/Requirements.md`, `../docs/Concepção/Visao_e_CasoDeNegocio.md`, jornadas/personas e wireframes em `../docs/`.
3. **Especificações locais:** `BACKEND_SPECS.md`, `API_CONTRACT.md`, `ARCHITECTURE.md`, `DATABASE_MODEL.md`, `SECURITY.md`, `TEST_PLAN.md`, `IMPLEMENTATION_PLAN.md` e `DECISIONS.md`.
4. **Referências legadas, somente leitura:** `../backend/` e a branch remota `origin/naoto_back_BD` (não é `naoto_back_DB`). Investigue os commits e compare ideias, não presuma que o código compila nem faça merge automático.

Em contradições, **código atualmente consumido pelo frontend prevalece para compatibilidade de wire format**, salvo correção explicitada em `DECISIONS.md` e acompanhada de alteração/testes no cliente. Segurança e proteção de dados prevalecem sobre compatibilidade inadequada.

## 3. Escopo e limites de escrita

- **Implementação principal exclusivamente em `backend-novo/`.** Crie ali `pom.xml`, `src/`, migrations, testes, `Dockerfile`, `compose.yaml`, configuração e documentação.
- **Não editar `../backend/`, `../frontend/`, `../docs/`, `.git/`, arquivos de terceiros ou código da branch do colega.** Não fazer merge automático de `naoto_back_BD`.
- Alterações pontuais em **`../frontend-novo/` somente na etapa de integração**, quando necessárias e justificadas para autenticação/contrato real; documente cada arquivo, preserve a UI, execute testes e evite redesign. Não altere as versões mock nem o fluxo demonstrativo desnecessariamente.
- Não editar o `docker-compose.yml` da raiz sem autorização. Prefira `backend-novo/compose.yaml` autocontido para desenvolvimento; documente execução do Vite separadamente.
- Não enviar commits/push nem abrir PR sem solicitação. Antes de ações destrutivas, preserve dados e informe riscos; não apagar volumes ou histórico.
- **Nome correto para instruções automáticas do Codex: `AGENTS.md`**, não `AGENTES.md`.

## 4. Diretrizes de arquitetura

- Stack: **Java 17 + Spring Boot 3.x compatível + Maven + PostgreSQL + Flyway + JPA**. Verifique compatibilidade real de versões/dependências; não copie automaticamente versões obsoletas dos exemplos.
- Pacotes por **funcionalidade** (auth, documents, analysis, chat, profile) e componentes compartilhados mínimos (security, storage, ocr, ai, error). Controllers finos; serviços com casos de uso; repositórios JPA sem regras de negócio; DTOs separados de entidades.
- **Frontend nunca conecta ao PostgreSQL nem chama Gemini/OCR diretamente**. API REST JSON para comandos/consultas e multipart para upload.
- Arquivos binários armazenados em abstração `FileStorage`, não em paths montados pelo cliente ou colunas de banco por padrão; armazenamento local privado no desenvolvimento e substituível depois.
- OCR sob interface `TextExtractor`, IA sob interface `ContractAiClient` (nomes podem variar); evite dependência direta de provider em controllers. Extração de PDF textual preferencialmente local; OCR de imagem/PDF escaneado quando necessário, documentando limitations.
- Pipeline com estados `pending | processing | completed | failed`, durável e consultável por documento; falha externa não pode retornar sucesso falso. Sem *fallback silencioso* da API para mock.
- Não criar microserviços, filas externas ou arquitetura hexagonal excessiva se não houver necessidade clara. Um monólito modular é suficiente.

## 5. Segurança e dados sensíveis (obrigatório)

- Contratos, renda, CPF/RG, endereços e mensagens podem conter dados pessoais. Não publicar dados reais em logs, traces, erros, fixtures, README ou prompts. Nunca commitar `.env`, secrets, tokens, credenciais ou arquivos enviados.
- Toda rota privada exige autenticação e **autorização por proprietário do documento/conta**; não confiar apenas no `documentId`. Para o desenvolvimento, uma exceção local controlada é permitida somente com flag explícita, vinculada ao loopback e **desativada em ambientes públicos**.
- Preferir sessões/cookies seguros ou mecanismo equivalente adequado à SPA; decidir concretamente e implementar proteção CSRF onde aplicável. Senhas com hash seguro; não implementar autenticação caseira insegura.
- Validar tamanho, extensão, MIME e assinatura/conteúdo dos arquivos; limitar formatos, páginas, tempo e consumo de recursos; impedir traversal e execução de conteúdos carregados. Rejeitar arquivos suspeitos de forma segura.
- Definir políticas explícitas de armazenamento, retenção, exclusão e consentimento para envio à IA externa. Não usar documentos reais de terceiros em testes.
- Mitigar *prompt injection* em contratos: texto OCR é **entrada não confiável**, não instruções de sistema; não executar comandos ou ferramentas com conteúdo do arquivo.
- IA não deve inventar cláusulas, valores ou referências. Em incerteza, dizer que não encontrou evidência. Informar que respostas não substituem profissional qualificado.

## 6. Contrato e integração

- **Primeiro implemente o contrato já esperado no frontend** e descrito em `API_CONTRACT.md`: `/api/documentos`, `/api/documentos/{id}/analise`, `/api/documentos/{id}/chat/mensagens`, `/api/perfil`, health. O endpoint legado `/api/analise/contrato` é referência/possível compatibilidade opcional, não contrato principal.
- Campo do upload `file`, categoria opcional `categoryId`. DTO de documento com `id`, `name`, `categoryId`, `processingStatus`, `createdAt`; formatos e papéis do chat exatamente como combinados. Nenhum segredo em `VITE_*`.
- Prefira processamento assíncrono para OCR+Gemini; devolver ID e status rapidamente. O frontend já sabe consultar periodicamente o status. Nunca manter uma requisição de upload aberta por todo o tempo de inferência sem necessidade.
- Preservar claramente duas experiências: `mock` funciona offline; `api` exige backend e retorna erros reais quando indisponível.
- Caso sejam indispensáveis mudanças de contrato, registrar em `DECISIONS.md`, atualizar `API_CONTRACT.md`, backend, frontend e testes no mesmo incremento.

## 7. Qualidade e método de execução

1. Inventariar o repositório e confirmar branch, estado de Git, fontes e dependências.
2. Atualizar `IMPLEMENTATION_PLAN.md` com passos verificáveis e pendências reais antes de implementar.
3. Implementar **por fatias verticais pequenas**, cada uma compilando e com testes. Não produzir 40 classes vazias apenas para preencher arquitetura.
4. Criar testes de serviço, integração HTTP e persistência; Testcontainers quando disponível ou teste equivalente explicitamente documentado. Simular provider Gemini/OCR nos testes determinísticos; live smoke tests somente opcionais e opt-in.
5. Executar, conforme a fase, `./mvnw test` (se wrapper existir) ou `mvn test`, `mvn verify`, build de Docker e testes do frontend quando alterado. Validar o fluxo completo em condições reais quando credenciais e rede permitirem.
6. Atualizar `DECISIONS.md` ao resolver ambiguidades e `IMPLEMENTATION_PLAN.md` após cada fase. Entregar resumo objetivo do que funciona, limitações e evidência dos testes.

**Proibido:** fabricar saída de testes; chamar um stub de integração real; registrar `GET /health` como prova de banco/Gemini/OCR; esconder erro usando resposta fake; criar bypass público permanente de autenticação; afirmar funcionamento ao vivo com credenciais não fornecidas.

## 8. Critérios finais

A meta de MVP somente estará atendida quando houver: banco migrado, login e isolamento por usuário, upload validado, documento persistido, extração real, análise Gemini real (se credenciais disponíveis), resumo estruturado consultável, chat ancorado no documento, histórico, preferências, exclusão, API compatível, inicialização documentada, testes e demonstração `frontend-novo` ↔ `backend-novo`.

Se uma integração externa não puder ser validada por falta de credencial/acesso, implemente o adapter e testes locais com *fake explícito*, documente **não verificado em ambiente real** e não marque o critério de aceite externo como concluído.
