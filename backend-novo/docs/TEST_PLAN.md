# TEST_PLAN.md — Estratégia de validação do backend Kaup

## Recorte da fase 2

Resultado comprovado: **42 testes aprovados** no clean verify (26 unidade/HTTP + 16 integração), sem falhas ou ignorados. Compose, reinício com sessão, cookies prod e limite real 429 também verificados; evidências em IMPLEMENTATION_PLAN.md.

Unitários: validação UTF-8/Unicode e não exposição da senha, BCrypt/credenciais apagadas, cadastro
atômico, limites concorrentes, cookies Secure/HttpOnly/SameSite e guard de owner/conta habilitada.
AuthHttpTest cobre JSON/validação, CSRF, 401/403/415/429 e cache de token. AuthFlowIT usa PostgreSQL
real e HTTP com cookies: cadastro sem auto-login, concorrência/normalização, hash, rotação de sessão,
CSRF antigo/cruzado, reautenticação, logout/replay/expiração, conta desabilitada e isolamento A/B.
O recurso/table/controller de ownership existe somente nos testes, não na aplicação ou migrations.
O build não contém essas fixtures. Testes usam custo BCrypt 10; Compose usa o default 12.

## Histórico do recorte da fase 1

`HttpContractTest` usa MockMvc, CSRF e principal sintético apenas nos testes para cobrir health,
401/403, IDs de correlação, 404/405, validação/JSON inválido, 500 e fallback servlet. Rotas `/test/*`
existem somente no código de teste, não no jar publicado. `PostgresMigrationIT` usa PostgreSQL 17.9
real/Testcontainers: migration limpa, validação/reexecução sem mudanças, constraints e HTTP real
após startup. `mvn test` roda HTTP; `mvn verify` também roda IT via Failsafe, sem ignorar falta de Docker.
Na revisão, os IT também passaram a verificar métodos bloqueados pelo firewall (TRACE/INVALID)
com JSON/requestId e HTTP malformado rejeitado sem ecoar conteúdo na resposta ou nos logs.
O slice HTTP desabilita a criação automática de usuário, como a aplicação, e confere correlação
de header/body em dispatch ERROR com os atributos de servlet presentes.
Resultados efetivamente executados e limitações ficam em IMPLEMENTATION_PLAN.md. O restante deste
documento continua sendo plano para fases posteriores.

## 1. Princípios

A aplicação depende de banco, armazenamento, OCR e IA externa. **Testes automatizados determinísticos não devem depender de credenciais nem chamadas reais ao Gemini**. Use fakes controlados nos testes de negócio e inclua testes separados de smoke/integrados habilitados somente por configuração explícita.

Exigir evidência: comandos executados, total de testes aprovados/falhos, limitações e versão da branch. Se não for possível executar, registrar motivo em `IMPLEMENTATION_PLAN.md`.

## 2. Níveis de teste

| Nível | Ferramenta sugerida | Objetivo |
|---|---|---|
| Unitário | JUnit 5 + Mockito/AssertJ | Validadores, services, transições, prompt/context builder, permissão, erros. |
| HTTP/slice | MockMvc + Spring Test | Rotas, JSON, validação Bean Validation, multipart, 401/403/404/413/415/422 e CSRF. |
| Persistência | Testcontainers PostgreSQL + Flyway | Migrations, constraints, transações, owner filters, índices principais, exclusão. |
| Integração interna | SpringBootTest + storage temporário + fake AI/OCR | Upload → status → análise → chat → delete. |
| OCR local | PDFBox/Tess4J com fixture pequena | PDF com texto, imagem legível, imagem ilegível, páginas múltiplas/limites. |
| API contrato | Testes de schema JSON + clientes existentes | Campos/tipos/status esperados por `frontend-novo/src/services/api/dto.ts`. |
| Browser E2E | Playwright/testes do frontend existentes | Autenticação, upload, polling, resultados, chat, histórico, exclusão. |
| Live smoke opcional | Ambiente explicitamente habilitado | Gemini real com dados sintéticos, custos/limites e aviso 'não determinístico'. |

## 3. Casos críticos detalhados

### Autenticação e propriedade

- Cadastro válido e inválido, e-mail duplicado, hash de senha, login/logout e expiração de sessão.
- Sem sessão: documentos, perfil e chat retornam 401; CSRF obrigatório para escrita quando usar cookies.
- Dois usuários A/B: A não lê, altera, exclui ou usa no chat documento de B mesmo conhecendo ID; A não vê dados de B na lista.

### Upload/documentos

- PDF válido, PNG e JPEG válidos; upload multipart em campo `file` com categoria opcional.
- Arquivo vazio, MIME incorreto, assinatura inválida, extensão enganosa, arquivo > limite, PDF corrompido, imagem enorme, nome com `../`.
- `POST /api/documentos` devolve `id`/`createdAt`/`processingStatus` e só registra um documento por upload válido.
- `GET /api/documentos` devolve **array**, não paginação embrulhada, enquanto contrato atual assim exigir.
- Reload/reinício do serviço preserva documento e andamento; deleção limpa dados associados e storage privado.

### Processamento/OCR

- PDF digital extrai texto corretamente e não chama OCR desnecessariamente.
- PNG/JPEG legível aciona extrator, texto vazio resulta em estado/erro apropriado.
- OCR lança exceção ou expira → status persiste como `failed`/retry permitido e não produz análise fictícia.
- Duas execuções concorrentes do mesmo job não criam duas análises nem duplicam efeitos.
- Job `processing` abandonado por reinício é recuperado por mecanismo descrito em arquitetura.

### Análise com IA

- Fake de IA retorna JSON correto com `summary`, `facts`, `keyPoints` e referências compatíveis.
- Resposta malformada, JSON sem campos, severidade inválida, quota/429, timeout e 5xx são tratados.
- Teste de contrato sem informações de multa: saída não pode afirmar valor específico de multa.
- Texto de contrato contendo comando malicioso 'ignore instruções' deve permanecer como dado, não alterar comportamento da API.
- Opcional: fixtures manuais de contratos sintéticos com gabarito para comparar qualidade sem afirmar 100% de acerto.

### Chat

- Pergunta pertence ao documento do usuário; `GET messages` ordenado; resposta `role: assistant` com `documentId` correto.
- Sem análise concluída: definir `409`/status apropriado ou resposta controlada; nunca inventar leitura.
- Falha no provider não salva resposta falsa; retries não duplicam turnos quando idempotência tiver sido implementada.
- Perguntas sobre multa/prazo ausentes no texto retornam incerteza, não percentual fixo.
- Perguntas sobre documentos distintos não misturam contexto ou histórico.

### Perfil e privacidade

- `GET/PATCH perfil` seguem as enumerações TypeScript; valores inválidos são rejeitados.
- Preferência `prefer_not` não força coleta de renda; alterações do usuário B não mudam perfil A.
- Verificar logs e respostas de erro: sem bytes/OCR, pergunta completa, senha ou chave.

## 4. Validação mínima do contrato TypeScript

Criar fixtures de resposta JSON e testes que reproduzam o parsing esperado em `frontend-novo/src/services/api/dto.ts`, especialmente:

- `DocumentDTO` campos obrigatórios, timestamps ISO e `categoryId:null` permitido;
- `AnalysisDTO`: `summary:null` durante `processing`, `keyPoints:[]`, severidades válidas;
- `ChatMessageDTO`: role `assistant|user`, `content`, `createdAt`, `documentId`;
- `UserProfileDTO`: seis campos e enums idênticos;
- `GET /health`: texto simples;
- `DELETE`: 204 sem body;
- `GET /api/documentos` e `GET /chat/mensagens`: arrays puros.

**Atenção ao frontend atual:** `analysisFromDTO()` pode ignorar `facts`, exigindo mudança pontual coordenada para o cartão de dados reais; `ApiService.sendMessage()` já pode devolver `ChatTurnResult` internamente. Inspecione o estado da branch e não adapte backend com base em versão antiga.

## 5. Comandos de verificação (exemplos)

```bash
cd backend-novo
mvn -B test
mvn -B verify
mvn -B package -DskipTests
# Se o ambiente possuir Docker:
docker compose -f compose.yaml up --build -d
docker compose -f compose.yaml ps
curl -i http://localhost:8080/api/analise/health
```

Com frontend afetado:

```bash
cd ../frontend-novo
npm ci
npm run lint
npm run test
npm run build
```

Os comandos só devem ser marcados como executados após execução real. Não criar testes que usam asserts artificiais sobre implementações dummy.

## 6. Critério de aprovação por fase

- Base: Maven compila, migration sobe e health responde.
- Auth: 401/403/CSRF e ownership testados.
- Documents: multipart seguro, CRUD e persistência testados.
- OCR: extração com fixtures, status/restart/falhas.
- Gemini: saída estruturada testada via fake, live smoke marcado como não validado se faltar chave.
- Chat: contexto isolado, histórico e respostas testados.
- E2E: fluxo real de upload/análise/chat/deleção com frontend em `VITE_DATA_SOURCE=api` (quando credenciais e UI de auth existirem).

Na entrega final, incluir matriz 'testado localmente / testado com provider real / ainda não validado'.
