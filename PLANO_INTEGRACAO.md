# Plano de integração Frontend ↔ Backend — Kaup

> Data da análise: 2026-10-29. Documento de planejamento: **nenhum código foi alterado, executado ou testado** para produzi-lo. Todas as conclusões vêm da leitura dos arquivos citados; o que não foi lido está marcado como **não verificado** (seção 8).

---

## 1. Resumo do estado atual

**Backend (`backend/`, Java 17 / Spring Boot / JPA / PostgreSQL / Tess4J / PDFBox)** — existe um único fluxo real, **síncrono e sem estado**:

- `GET /api/analise/health` → texto simples.
- `POST /api/analise/contrato` (multipart, campo **`arquivo`**) → valida tipo (PDF/PNG/JPEG) → `OcrService` (PDFBox para PDF digital, Tesseract para escaneado/imagem) → `GeminiService.interpretar` → devolve `{ textoExtraido, analise }`.
- `POST /api/analise/texto` (JSON `{texto}`) → mesmo fluxo sem OCR.
- `ApiExceptionHandler` devolve `ProblemDetail` (RFC 7807) para 400 / 413 / 502.
- O JSON da IA tem o formato `{ resumo, valores[], prazos[], multasEJuros[], obrigacoes[], pontosDeAtencao[] }` (definido no prompt em `GeminiService.java`).
- `Analise` (entity) e `AnaliseRepository` existem, mas **nada os usa**: `grep` só os encontra na própria definição. Nada é persistido.
- **Não existe**: `Documento`, `Usuario`, `MensagemChat`, autenticação (sem Spring Security no `pom.xml`), CORS, chat, perfil, categorização, listagem, exclusão.
- Testes: 3 testes de controller com OCR/Gemini mockados (`AnaliseControllerTest.java`). Não há teste de ponta a ponta.

**Frontend (`frontend/src/`, React + TS + Vite)** — a UI está completa e roda em **modo mock** (`VITE_DATA_SOURCE=mock`, padrão em `.env.example`). Já existe um `ApiService` (`services/api/ApiService.ts`) que implementa a interface `KaupService` contra endpoints **propostos** em `services/api/endpoints.ts` (`/api/documentos`, `/api/documentos/{id}/analise`, `/api/documentos/{id}/chat/mensagens`, `/api/perfil`). Esses endpoints **não existem no backend**. Só o `health` coincide. O `ApiService` valida o formato das respostas de forma estrita (`services/api/dto.ts`), tem timeout, suporte a `AbortSignal` e a `useChat` já faz polling do status da análise.

**Conclusão central:** o backend tem o *miolo* (OCR + IA) mas expõe um contrato diferente do que o frontend espera, e não tem camada de persistência. O frontend está pronto para falar com um contrato REST de "documentos" que ainda precisa ser construído. A maior parte do trabalho é **backend**; no frontend são ajustes pontuais (tratamento de erro, timeout, mapeamentos).

**Documentação desatualizada:** `frontend/API_CONTRACT_RASCUNHO.md` diz que `OcrService`/`GeminiService` estão "sem implementação funcional". Isso já não é verdade (os dois estão implementados), nem que só o `health` existe (existem `/contrato` e `/texto`). Convém atualizá-lo. (Os relatórios `MIGRATION_NOTES.md`, `CHAT_FLOW_NOTES.md`, `SPECS.md`, a pasta `frontend-novo/` e os scripts de validação no navegador já foram removidos.)

---

## 2. Tabela de funcionalidades e classificação

Legenda: **Pronta** · **Pequenos ajustes** · **Desenvolvimento significativo** · **Inexistente**.
A classificação considera a integração *ponta a ponta* (front + back).

| # | Funcionalidade (requisito) | Classificação | Resumo da evidência |
|---|---|---|---|
| 1 | Health check | **Pronta** | `GET /api/analise/health` existe e `endpoints.health` + `ApiService.health` apontam para ele (texto simples, `text=true`). Só prova que a aplicação subiu, não OCR/IA/BD. |
| 2 | Validação de arquivo (RF03/RF04) — PDF/PNG/JPEG, 10 MiB | **Pronta** | `features/documents/upload.ts` e `application.properties` (10MB) + `AnaliseController.TIPOS_ACEITOS` coincidem. |
| 3 | Pipeline OCR + IA (RF05/RF08) | **Pequenos ajustes** | Backend funciona via `/api/analise/contrato`. Falta apenas expô-lo no contrato que o frontend usa (campo `file` vs `arquivo`, formato da resposta). Ver §4.1. |
| 4 | Upload de documento com ID e status (RF04) | **Desenvolvimento significativo** | `ApiService.upload` espera `POST /api/documentos` → `DocumentDTO` com `id`. Backend não tem entidade `Documento` nem persiste nada; `Analise` é uma base mínima. |
| 5 | Resumo estruturado (RF08) — `GET …/analise` | **Desenvolvimento significativo** | Depende de persistir o resultado. Além disso o JSON da IA (`resumo`, `valores`…) precisa ser mapeado para `AnalysisDTO` (`summary`, `keyPoints`). |
| 6 | Listagem e detalhe de documentos | **Desenvolvimento significativo** | `AppProvider` chama `listDocuments` ao iniciar; `useChat` chama `getDocument`. Sem endpoints nem entidade. Frontend pronto. |
| 7 | Feedback de processamento (RD02) / status | **Pequenos ajustes** | Frontend pronto: `useChat` faz polling a cada 2,5 s se `pending/processing`. Backend precisa devolver `processingStatus`. Se for síncrono, devolve sempre `completed`. |
| 8 | Categorização automática (RF02) | **Inexistente** (back) | Nenhuma classificação no backend (prompt não pede categoria). Frontend já aceita `categoryId: null` e cai em "Outros" (`styles/presenters.ts`). Baixo esforço: um campo no prompt do Gemini. |
| 9 | Chat sobre o contrato (RF06, texto) | **Desenvolvimento significativo** | `ApiService.sendMessage/getMessages` prontos. Backend: `GeminiService` só tem `interpretar` (força `responseMimeType: application/json`); falta endpoint, método de conversa e persistência do histórico. |
| 10 | Histórico de chat persistido | **Inexistente** | Sem entidade/endpoint de mensagens. |
| 11 | Perfil e preferências (`GET/PATCH /api/perfil`) | **Inexistente** (back) | Frontend pronto (`getProfile`/`updateProfile`, validação estrita de enums). Backend sem `Usuario`/`Perfil`. |
| 12 | Preferências influenciando a IA (RF09, RNF01) | **Inexistente** | `GeminiService` não recebe nível de explicação nem renda. |
| 13 | Autenticação/autorização (RF01) | **Inexistente** | Sem Spring Security no `pom.xml`; frontend em modo API desabilita login/logout (`AccountActions.tsx`). Sem isolamento por usuário. |
| 14 | Exclusão de documento (RC02) | **Inexistente** (back) | `ApiService.removeDocument` existe, mas **nenhum componente o chama** (grep). Backend sem `DELETE`. Precisa de botão na UI + endpoint. |
| 15 | Anexar arquivo de contexto no chat | **Inexistente** | `ApiService.attachContext` lança erro de propósito; sem contrato. `getContext` já funciona derivado de `getDocument` (depende do item 6). |
| 16 | Narração em áudio e entrada por voz (RF06/RF07) | **Inexistente** | Frontend: "áudio" é apenas preferência/demonstrativo (`frontend/README.md`: "Áudio, ajuda, exclusão de conta e logout são demonstrativos"). Backend: nada. |
| 17 | Exclusão de conta / exportação de dados (LGPD, RC02) | **Inexistente** | Simulação no frontend (`AccountActions.tsx`: "nenhum dado de servidor será apagado"). |
| 18 | Tratamento de erros da API (RC03, RF-erros) | **Pequenos ajustes** | Backend devolve mensagens úteis em `ProblemDetail.detail`; `http.ts` **ignora o corpo** e mostra mensagem genérica. |
| 19 | Dev/infra: proxy, CORS, Docker, portas | **Pequenos ajustes** | Vite proxy `/api → :8080` já existe; sem CORS no backend (ok enquanto usar proxy). Ver §4.9. |

---

## 3. Arquivos e endpoints envolvidos

### 3.1 Backend (existente)

| Arquivo | Papel |
|---|---|
| `backend/src/main/java/com/kaup/controller/AnaliseController.java` | Único controller: `/api/analise/{health,contrato,texto}` |
| `service/OcrService.java` | OCR local (PDF digital → texto direto; escaneado → render 300 DPI → Tesseract; máx. 20 páginas) |
| `service/GeminiService.java` | Chamada ao Gemini (`interpretar`), prompt de sistema e JSON de saída |
| `entity/Analise.java`, `repository/AnaliseRepository.java` | Base de persistência **não utilizada** |
| `dto/AnaliseContratoResponse.java`, `dto/TextoContratoRequest.java` | DTOs do fluxo atual |
| `exception/ApiExceptionHandler.java` | `ProblemDetail` 400/413/502 |
| `resources/application.properties` | BD, Gemini, OCR, multipart 10MB, `ddl-auto=update` |

### 3.2 Frontend (existente)

| Arquivo | Papel |
|---|---|
| `src/services/contracts/KaupService.ts` | Interface única que mock e API implementam |
| `src/services/api/ApiService.ts` | Implementação HTTP (já escrita) |
| `src/services/api/endpoints.ts` | **Único** lugar com os caminhos |
| `src/services/api/dto.ts` | Validação/mapeamento DTO → domínio (estrito) |
| `src/services/api/http.ts` | `fetch`, timeout, erros |
| `src/services/index.ts` | `createService()` escolhe mock/api por `VITE_DATA_SOURCE` |
| `src/app/AppProvider.tsx` | Carrega documentos e perfil na inicialização |
| `src/features/chat/useChat.ts` | Análise (com polling), histórico, envio de mensagens |
| `src/components/UploadDialog.tsx` | Upload (e `attachContext`) |
| `src/components/chat/SummaryCard.tsx` | Renderiza `summary`, `keyPoints`, `facts` |
| `src/components/profile/AccountActions.tsx` | Logout/exclusão (simulação) |
| `vite.config.ts`, `.env.example` | Proxy dev e variáveis |

### 3.3 Mapa: o que o frontend chama × o que o backend tem

| Chamada do frontend | Método | Existe no backend? | Observação |
|---|---|---|---|
| `/api/analise/health` | GET | ✅ | — |
| `/api/documentos` | POST (multipart `file`, `categoryId`) | ❌ | Backend usa `/api/analise/contrato` com campo `arquivo` |
| `/api/documentos` | GET | ❌ | — |
| `/api/documentos/{id}` | GET / DELETE | ❌ | — |
| `/api/documentos/{id}/analise` | GET | ❌ | — |
| `/api/documentos/{id}/chat/mensagens` | GET / POST `{message}` | ❌ | — |
| `/api/perfil` | GET / PATCH | ❌ | — |

---

## 4. Alterações necessárias por integração

### Decisão de contrato (pré-requisito)

O `API_CONTRACT_RASCUNHO.md` diz que o backend precisa aprovar o contrato. **Recomendação:** adotar os caminhos e DTOs do rascunho como estão, porque o `ApiService` já os implementa e é mais barato implementar o backend do que reescrever o adapter. Manter `/api/analise/contrato` e `/texto` como rotas de depuração (ou remover depois).

### 4.1 Upload + OCR + IA + resumo (itens 3, 4, 5, 7) — **o fatiamento vertical do MVP**

**Backend**
1. Criar entidade `Documento` (id, nome, mimeType, `processingStatus`, `categoryId` nulo por enquanto, `createdAt`) e ligar a `Analise` (já tem `textoExtraido` e `analiseJson`) ou fundir os dois. Não é preciso guardar o arquivo original (decisão de retenção pendente — ver §8).
2. `DocumentoController`: `POST /api/documentos` (multipart, campo **`file`**, `categoryId` opcional) que roda o pipeline existente (`validar` → `OcrService` → `GeminiService`), persiste e devolve o `DocumentDTO`. Reaproveitar `validar()` e as exceções.
3. `GET /api/documentos/{id}/analise` devolvendo `AnalysisDTO`, **mapeando o JSON do Gemini**:
   - `resumo` → `summary`;
   - `pontosDeAtencao[]` → `keyPoints` com `severity: "warning"`;
   - `valores`/`prazos`/`multasEJuros`/`obrigacoes` → `keyPoints` com `severity: "info"` (título = descrição, texto = valor/prazo/detalhe), ou expandir o DTO (ver 4.1-front);
   - cada `keyPoint` precisa de `id` (string) — gerar.
4. Falhas de OCR/IA: marcar `processingStatus = failed` e devolver `ocrQualityWarning` com a mensagem amigável (hoje "Tente uma foto mais nítida").

**Armadilhas de serialização que o `dto.ts` rejeitaria** (validação estrita):
- `id` e `documentId` devem ser **strings** (`string(d.id)`); `Long` do JPA serializa como número e quebraria. Use `String` no DTO.
- `categoryId` deve estar **presente** como `null` ou string (se omitido, `string(undefined)` lança erro).
- `createdAt` deve ser ISO 8601 parseável; prefira `Instant`/`OffsetDateTime`. `Analise.criadoEm` é `LocalDateTime` (sem fuso) — o navegador interpretaria como horário local.
- `processingStatus` ∈ `pending|processing|completed|failed` (minúsculas).

**Síncrono × assíncrono.** O frontend aceita os dois: a `useChat` faz polling enquanto o status for `pending/processing`. Porém `ApiService.upload` aguarda a resposta do POST com **timeout padrão de 20 s** (`VITE_API_TIMEOUT_MS`). OCR de até 20 páginas a 300 DPI + chamada ao Gemini pode passar disso.
- *Decisão (MVP):* síncrono, devolvendo `completed`, com `VITE_API_TIMEOUT_MS` um pouco acima do padrão (ex.: 60000) para absorver oscilações. Com o modelo `gemini-3.5-flash-lite` a Fase 0 mediu ~2 s por análise (§7, "Segunda rodada"); com o modelo anterior eram 70–180 s e o síncrono seria inviável.
- *Pendente de medição:* PDFs escaneados longos. Se o OCR passar de ~15 s, migrar para `202 Accepted` + `@Async`/fila + polling (frontend já pronto: `useChat` consulta a análise a cada 2,5 s; atende RD02/RD03).
- Adicionar timeout próprio no `RestClient` do `GeminiService` (hoje não há) para que um Gemini travado não prenda a requisição.

**Frontend (ajustes)**
- `http.ts`: ler o corpo de erro (`ProblemDetail.detail`) e usá-lo como mensagem (ver 4.8).
- (Opcional) `dto.ts`/`SummaryCard.tsx`: mapear `valores/prazos` para `facts` (o `SummaryCard` já renderiza `facts` com rótulo/valor; hoje `analysisFromDTO` não preenche `facts`, e o `SummaryCard` ignora `severity` ao desenhar os pontos).
- Nenhuma mudança em `UploadDialog` além do timeout.

**Alternativa rápida (não recomendada como destino)**: adaptar só o front, chamando `/api/analise/contrato` direto e guardando o resultado em memória. Dá um demo em horas, mas perde tudo ao recarregar, não tem `id`, e o código seria descartado. Só vale como validação rápida de OCR+IA no navegador.

### 4.2 Listagem e detalhe (item 6)
- Backend: `GET /api/documentos` (lista `DocumentDTO`, mais recente primeiro) e `GET /api/documentos/{id}`; 404 para id inexistente.
- Frontend: nenhum ajuste previsto. `description` e `visualStatus` são opcionais no DTO; sem eles o card mostra "Documento enviado" e status `pending`/`warning`. Para o semáforo (✅/⚠️) ficar significativo, o backend pode devolver `visualStatus` (`ok` se não há pontos de atenção, `warning` caso contrário) e `description` (primeira frase do resumo).

### 4.3 Categorização (item 8)
- Backend: pedir `categoria` ao Gemini no prompt, restrita às chaves **que o frontend conhece**: `Carros`, `Casa`, `Trabalho`, `Banco`, `Outros` (`styles/categories.ts`). Persistir em `categoryId`.
- Atenção: `docs/Concepção/Requirements.md` (RF02) cita "Veículos, Imóveis, Serviços" — nomes diferentes dos do frontend. Alinhar com o time; o contrato real é o do frontend.
- Frontend: nenhum ajuste (o fallback para "Outros" já existe).

### 4.4 Chat (itens 9, 10)
- Backend: novo método em `GeminiService` para conversa (resposta em **texto**, não JSON), com o texto extraído como contexto (tratado como dado, como o prompt atual já faz), histórico recente e a pergunta. Endpoints: `POST` e `GET /api/documentos/{id}/chat/mensagens`. Entidade `MensagemChat` (id, documento, `role` `user|assistant`, `content`, `createdAt`).
- Formato exigido pelo `messageFromDTO`: `{id: string, documentId: string, role: "assistant"|"user", content, createdAt}`. O `POST` devolve **apenas a resposta do assistente** (com `documentId` igual ao da URL, ou `ApiService` lança erro); o `GET` devolve as duas pontas.
- Frontend: sem alterações esperadas no `ApiService`. **Não verificado:** como `features/chat/session.ts` e `ChatPage.tsx` se comportam em modo API com histórico vazio (o código sugere cair em `summary-card` + `stageFromHistory`); testar no primeiro contato.

### 4.5 Perfil e preferências (itens 11, 12)
- Backend: `GET/PATCH /api/perfil` com `{name, email, fontSize, explainLevel, income, audio}`. O `profileFromDTO` exige exatamente: `fontSize ∈ small|medium|large`, `explainLevel ∈ simple|normal|complete`, `income ∈ less3|3to5|5to7|7to15|more15|prefer_not`, `audio: boolean`, e `name`/`email` como **strings não nulas** (devolver `""` por padrão).
- Sem autenticação, só é possível ter **um perfil global**. Para o MVP, isso é aceitável apenas em ambiente de demonstração (ver §6, Fase 4).
- RF09: injetar `explainLevel` (e `income`, se o usuário não escolheu "prefer_not") no prompt de sistema do `GeminiService`.
- Frontend: a falha de perfil não bloqueia documentos (`AppProvider` trata separado, `profileError`), então o perfil pode ficar para depois do núcleo.

### 4.6 Autenticação e isolamento (item 13)
- Backend: Spring Security + estratégia de sessão/token (decisão pendente: JWT, cookie HttpOnly, Google OAuth — RF01), entidade `Usuario`, `usuarioId` em `Documento`/`Perfil`, verificação de dono em cada acesso por `id`.
- Frontend: tela de login/cadastro **não existe** (só `LoggedOut.tsx` demonstrativo); `http.ts` precisa enviar credenciais (cookie `credentials: "include"` ou header `Authorization`) e tratar 401 redirecionando ao login.
- Sem isso, em modo API qualquer pessoa que alcance o servidor vê todos os documentos.

### 4.7 Exclusão (itens 14, 17)
- Backend: `DELETE /api/documentos/{id}` (204), apagando texto extraído, análise e mensagens. Exclusão de conta e exportação: endpoints novos.
- Frontend: `removeDocument` já existe no serviço; falta **botão/ação** na UI (nenhum componente chama). Exclusão de conta hoje é simulação.

### 4.8 Erros (item 18)
- `http.ts` só olha `response.status` e ignora o corpo; mensagens do backend (ex.: "Formato não suportado. Envie PDF, PNG ou JPEG.", "O PDF está protegido por senha.") nunca chegam ao usuário. Ajuste: tentar `response.json()` e usar `detail` (ProblemDetail) ou `message` (`ApiErrorDTO` do rascunho), com fallback na mensagem atual.
- Backend: `ApiExceptionHandler` cobre 400/413/502; faltam 404 (documento inexistente), 401/403 (quando houver auth) e um handler genérico para 500 sem vazar detalhes.
- `404/501` hoje geram "ainda não foi implementado" — após a integração, um 404 real de documento exibiria essa frase enganosa; ajustar.

### 4.9 Infra de desenvolvimento (item 19)
- **CORS:** o backend não configura CORS. Em dev funciona via proxy do Vite (`/api → http://localhost:8080`, `VITE_API_BASE_URL` vazio). Para produção/outra origem, definir CORS ou um proxy reverso.
- **Docker:** `docker-compose.yml` faz `build: ./frontend`, mas **não há `Dockerfile` rastreado em `frontend/`** (lista de arquivos do git) — o serviço `frontend` provavelmente não builda; **não verificado** ao rodar. A porta mapeada é 5173, enquanto o Vite usa 5174 (`vite.config.ts`).
- **Execução local do backend fora do Docker:** `application.properties` tem o host `postgres` fixo; a variável `DATABASE_URL` do compose não é lida por nenhuma propriedade (funciona só porque os valores fixos coincidem). Para rodar o backend na máquina é preciso sobrescrever `spring.datasource.url`.
- **Modelo do Gemini:** o padrão é `gemini-3.8-flash` (`application.properties`). **Não verificado** se esse identificador é válido; se não for, toda análise falha com 502. Confirmar com uma chamada real antes de culpar a integração.
- `GEMINI_API_KEY` vazio faz o backend subir normalmente e falhar só na primeira análise (502).

---

## 5. Funcionalidades ausentes (sem base utilizável hoje)

1. Persistência de documentos, análises e mensagens (entidades, repositórios, serviços).
2. Endpoints `/api/documentos/**`, `/api/perfil`.
3. Chat com a IA e histórico.
4. Perfil/preferências e seu uso no prompt (RF09).
5. Autenticação, cadastro, login social e isolamento por usuário (RF01).
6. Classificação automática por categoria (RF02).
7. Exclusão de documento, de conta e exportação (RC02/LGPD) — inclusive a ação na UI para documentos.
8. Anexar contexto ao chat.
9. Narração em áudio e entrada por voz (RF06/RF07).
10. Política de retenção/consentimento/LGPD e anonimização antes de enviar texto ao Gemini (hoje o texto integral do contrato vai à API externa; o `OcrService` diz que "o arquivo nunca sai do servidor", mas o **texto extraído sai** para o Gemini).
11. Processamento assíncrono (se o MVP síncrono não bastar).
12. Testes de integração reais (hoje só há mocks de OCR/IA no backend; no frontend, testes Vitest com HTTP interceptado em `src/tests/api.test.ts`).

---

## 6. Ordem recomendada de implementação (MVP primeiro)

**MVP = "envio um contrato, vejo o resumo, converso sobre ele, e ele fica salvo."** Auth e áudio ficam fora do MVP, mas ver o aviso da Fase 5 antes de qualquer deploy público.

| Fase | Objetivo | Entregáveis | Itens |
|---|---|---|---|
| **0 — Alinhamento (½ dia)** | Fechar o contrato e desbloquear o ambiente | Aprovar os DTOs do rascunho; decidir sync/async do MVP; subir stack local (Postgres + backend com chave Gemini válida); confirmar modelo Gemini | 19 |
| **1 — Fatia vertical: upload → resumo** | Primeiro fluxo real ponta a ponta | `Documento` + persistência; `POST /api/documentos`; `GET …/{id}` e `GET …/{id}/analise` com mapeamento; `http.ts` lendo `ProblemDetail`; timeout ajustado; testar com `VITE_DATA_SOURCE=api` | 3, 4, 5, 7, 18 |
| **2 — Documentos persistidos** | Tela inicial e listas com dados reais | `GET /api/documentos`; `visualStatus`/`description`; categorização via Gemini | 6, 8 |
| **3 — Chat** | Perguntas sobre o contrato | Método de conversa no `GeminiService`; entidade `MensagemChat`; `POST/GET …/chat/mensagens`; validar o comportamento de `ChatPage` em modo API | 9, 10 |
| **4 — Perfil e personalização** | Preferências valem de verdade | `GET/PATCH /api/perfil`; injeção de `explainLevel`/`income` no prompt. *Sem auth, é um perfil global — só para demo.* | 11, 12 |
| **5 — Conta e privacidade** | Pré-requisito para dados reais de pessoas | Spring Security, `Usuario`, `usuarioId` em tudo, 401/403, UI de login, `DELETE` de documento (+ botão), exclusão de conta, política LGPD. **Não publicar com dados reais antes desta fase.** | 13, 14, 17 |
| **6 — Extras** | Itens de requisito restantes | Assíncrono (202 + polling), anexar contexto, narração/voz, CI com teste de integração | 15, 16 |

Racional: a Fase 1 prova OCR + IA + persistência + contrato, que é o maior risco e destrava tudo o mais. As Fases 2–3 usam a mesma base. Perfil é isolado e não bloqueia documentos. Auth vem depois do MVP funcional, mas antes de qualquer uso real, e mudar para auth depois exige apenas adicionar `usuarioId` e credenciais no `http.ts` (o resto do contrato permanece).

---

## 7. Checklist de tarefas

### Fase 0 — Alinhamento (executada em 2026-10-29)
- [x] Aprovar o contrato de `frontend/API_CONTRACT_RASCUNHO.md` (caminhos, DTOs, nomes de campo) — aprovado
- [x] Decidir: upload síncrono (MVP) ou 202 + polling — **síncrono**. A primeira medição (com `gemini-3.8-flash`) reabriu a dúvida, mas a troca para `gemini-3.5-flash-lite` trouxe os tempos para ~2 s e confirmou o síncrono (ver "Segunda rodada")
- [x] Categorias: usar as que já estão implementadas no frontend (`Carros/Casa/Trabalho/Banco/Outros`)
- [x] Subir Postgres + backend com `GEMINI_API_KEY` válida e chamar `POST /api/analise/contrato` — funcionou (ver resultados)
- [x] Confirmar que `gemini-3.8-flash` responde — **responde** (200 em `/contrato` e `/texto`)
- [ ] Adiado de propósito: host `postgres` fixo e `Dockerfile` ausente do frontend (o front roda no host com `npm run dev`)

**Resultados medidos** (amostras fictícias, backend deste repo no Docker, porta 8080):

| Caso | Resultado | Tempo |
|---|---|---|
| `GET /api/analise/health` | 200; tabela `analise` criada pelo `ddl-auto` | — |
| JPEG (foto) | 200 | 70,6 s |
| PDF digital | 200 | 85,9 s |
| PDF escaneado (sem texto embutido, usa Tesseract) | 200 | 113,2 s |
| PNG | **sem resposta em 120 s** (limite do `curl`) | > 120 s |
| `POST /api/analise/texto` (Gemini sem OCR) | 200 e depois 502 | 65,3 s e 10,9 s |
| `.txt` / arquivo vazio / arquivo de 11 MB | 400 / 400 / 413, com `ProblemDetail.detail` em português | < 0,05 s |

- **Formato do JSON da IA confirmado** nas 3 amostras que responderam: `resumo`, `valores`, `prazos`, `multasEJuros`, `obrigacoes`, `pontosDeAtencao`. O mapeamento da §4.1 vale como está.
- **O tempo é quase todo do Gemini, não do OCR:** só o Gemini levou 65 s; com OCR, 70–113 s. Não há timeout configurado no `RestClient` do `GeminiService` (uma chamada pode ficar pendurada).
- **Gemini devolve 503 "high demand" de forma intermitente** (log do backend); o `ApiExceptionHandler` converte para 502 com a mensagem da exceção.
- **O OCR errou um valor financeiro:** no PDF escaneado, "R$ 1.500,00" foi lido como "R$ 1.506,00" e a IA repetiu o valor errado no resumo. Foi uma página sintética a 200 DPI; fotos reais tendem a piorar. Isso importa para um produto cujo objetivo é mostrar valores corretos (hoje o backend não tem nenhum aviso de baixa confiança do OCR; ver `ocrQualityWarning` na §4.1, ponto 4).

**Primeira rodada — conclusão:** com `gemini-3.8-flash`, 70–113 s por upload (chamadas ao Gemini isoladas chegaram a 151–181 s com outra chave, além de 503 "alta demanda"). O upload síncrono com timeout de 20 s não funcionaria.

**Segunda rodada — troca de modelo (2026-10-30).** Testes diretos na API do Gemini mostraram `gemini-3.5-flash-lite` e `gemini-3.1-flash-lite` respondendo em ~2 s; `gemini-2.5-flash` e `gemini-2.5-flash-lite` retornam 404 ("indisponível para novos usuários"). O padrão do backend passou a ser `gemini-3.5-flash-lite` (`application.properties`) e o `docker-compose.yml` repassa `GEMINI_MODEL` ao container. Reteste completo no backend reconstruído:

| Caso | Resultado | Tempo |
|---|---|---|
| PNG (2 execuções) | 200, valor lido "R$ 1.500,00" | 2,7 s / 2,6 s |
| JPEG | 200, "R$ 1.500,00" | 2,3 s |
| PDF digital | 200, "R$ 1.500,00" | 1,8 s |
| PDF escaneado (2 execuções) | 200, **"R$ 1.506,00"** (erro de OCR, igual nas duas) | 2,2 s / 2,5 s |
| `.txt` / vazio / 11 MB | 400 / 400 / 413 | < 0,05 s |

- **Upload síncrono viabilizado:** ~2 s de análise cabe com folga nos 20 s do frontend, para amostras de 1 página. **Não medido:** PDFs escaneados longos (até 20 páginas, 300 DPI, uma página por vez no Tesseract), onde o OCR pode passar a dominar o tempo. Medir antes de fechar o timeout; se passar de ~15 s, voltar a considerar `202` + polling.
- **O PNG que não respondia na primeira rodada** respondeu em 2,7 s; a causa era o modelo lento, não o OCR de PNG.
- **O erro de OCR ficou restrito ao caminho do PDF escaneado:** o mesmo conteúdo como PNG ou JPEG leu "R$ 1.500,00". A amostra de PDF escaneado foi gerada rasterizando um PDF digital a 200 DPI e o backend a re-renderiza a 300 DPI, então pode ser artefato do teste; **não confirmado** com um PDF escaneado de verdade. Mesmo assim, o risco de erro silencioso em valores justifica um aviso de baixa confiança (`ocrQualityWarning`, §4.1 ponto 4).
- **Ressalva:** uma chamada por modelo no teste direto e poucas no backend; os modelos "lite" podem ser mais fracos em contratos longos ou cláusulas complicadas (qualidade do resumo **não avaliada**).

### Fase 1 — Upload → resumo
- [ ] Backend: entidade `Documento` (+ relação com `Analise`) e repositório
- [ ] Backend: `POST /api/documentos` (campo `file`, `categoryId` opcional) reaproveitando `validar`, `OcrService`, `GeminiService`
- [ ] Backend: DTOs com `id` String, `categoryId` explícito (null), `createdAt` ISO com fuso, `processingStatus` minúsculo
- [ ] Backend: `GET /api/documentos/{id}` e `GET /api/documentos/{id}/analise` com mapeamento `resumo/pontosDeAtencao/valores/prazos/multasEJuros/obrigacoes → summary/keyPoints`
- [ ] Backend: falha de OCR/IA → `processingStatus=failed` + `ocrQualityWarning`
- [ ] Backend: handler 404 e handler genérico 500
- [ ] Frontend: `http.ts` usar `ProblemDetail.detail`/`message`; ajustar mensagem de 404
- [ ] Frontend: configurar `VITE_DATA_SOURCE=api` e `VITE_API_TIMEOUT_MS` adequado
- [ ] (Opcional) Frontend: mapear `valores/prazos` para `facts` e usar `severity` no `SummaryCard`
- [ ] Teste manual: PDF digital, PDF escaneado, foto JPEG, arquivo `.txt` (deve falhar com mensagem do backend), arquivo >10 MiB (413)
- [ ] Teste backend: controller com serviços mockados para o novo fluxo

### Fase 2 — Documentos persistidos
- [ ] `GET /api/documentos` ordenado por data
- [ ] Devolver `visualStatus` e `description`
- [ ] Pedir `categoria` ao Gemini e persistir em `categoryId`
- [ ] Conferir Home, Categoria e Todos os Documentos com dados reais

### Fase 3 — Chat
- [ ] `GeminiService`: método de conversa (texto livre) com texto do contrato + histórico + pergunta
- [ ] Entidade `MensagemChat` + repositório
- [ ] `POST /api/documentos/{id}/chat/mensagens` (devolve só a resposta do assistente) e `GET` (histórico completo)
- [ ] Verificar `ChatPage`/`session.ts` em modo API (histórico vazio, estágio inicial, retry de envio)
- [ ] Testar falha 502 do Gemini: pergunta preservada e botão de retry (RC01)

### Fase 4 — Perfil
- [ ] Entidade de perfil com defaults compatíveis com `profileFromDTO`
- [ ] `GET/PATCH /api/perfil` (validar enums no servidor)
- [ ] Passar `explainLevel` (e `income` quando informado) ao prompt do `GeminiService`
- [ ] Testar edição de perfil na `ProfilePage` em modo API

### Fase 5 — Conta e privacidade
- [ ] Escolher estratégia de auth e adicionar Spring Security
- [ ] `Usuario` + `usuarioId` em documentos/perfil/mensagens; checagem de dono em todo acesso por id
- [ ] Frontend: telas de cadastro/login, envio de credenciais no `http.ts`, tratamento de 401
- [ ] `DELETE /api/documentos/{id}` + ação na UI (hoje inexistente)
- [ ] Exclusão de conta e exportação de dados
- [ ] Definir retenção do texto extraído e consentimento (LGPD); avaliar anonimização antes do Gemini
- [ ] CORS/HTTPS para o ambiente de deploy (RNF02)

### Fase 6 — Extras
- [ ] Upload assíncrono (`202`) com executor próprio
- [ ] Anexar contexto (`attachContext`) — definir contrato antes
- [ ] Narração (RF07) e entrada por voz (RF06)
- [ ] CI com teste de integração (backend + Postgres) e e2e do frontend em modo API
- [ ] Atualizar `frontend/API_CONTRACT_RASCUNHO.md` com o contrato final

---

## 8. Não verificado (fora do escopo da leitura ou sem evidência)

- **Verificado na Fase 0 (2026-10-29):** o backend sobe no Docker, cria a tabela `analise`, e `/api/analise/contrato` funciona com JPEG, PDF digital e PDF escaneado; o modelo `gemini-3.8-flash` responde; erros 400/413 saem com mensagem em português. Detalhes e tempos em §7. **Continua não verificado:** os testes unitários (`mvn verify`), o PNG (estourou 120 s, sem resposta) e qualquer fluxo do frontend contra o backend.
- Latência: ~2 s por análise com `gemini-3.5-flash-lite` (6 execuções + 3 do teste direto), contra 65–180 s com `gemini-3.8-flash`. Amostra pequena; o Gemini oscila (houve 503 intermitente no modelo antigo) e não sei se o "lite" mantém a latência em horários de maior demanda.
- Qualidade do resumo do modelo "lite" em contratos reais/longos, e OCR de PDFs escaneados de verdade ou de várias páginas.
- `features/chat/session.ts`, `pages/ChatPage.tsx`, `components/UploadDialog.tsx` (lidos só por `grep`): comportamento exato em modo API, incluindo `stageFromHistory` e o ramo `service.mode`.
- Conteúdo de `backend/config/init.sql` (a leitura não mostrou conteúdo relevante; não confirmado), `Tess4jConfig.java`, workflow de CI (`ACTIONS`) e `Dockerfile` do backend.
- `docs/Arquitetura_Software/Projeto_Arquitetural.md` (só `grep`), `UseCasesExplicacao.md`, `WireFrame.md`, jornadas e personas — não lidos; a lista de requisitos veio de `Requirements.md`.
- Se o serviço `frontend` do `docker-compose.yml` consegue buildar (sem `Dockerfile` rastreado em `frontend/`).
- Qualquer decisão de produto pendente: política de retenção, anonimização, provedor de auth, formato final dos DTOs além do rascunho.
