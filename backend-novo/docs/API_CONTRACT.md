# API_CONTRACT.md — Contrato HTTP proposto, orientado ao `frontend-novo/`

**Estado da fase 2:** implementados health e autenticação descrita na seção 4. Documentos,
análise, chat e perfil abaixo continuam sendo contrato-alvo das próximas fases. Erros seguem
`{code,message,requestId}` e header X-Request-Id. Health é liveness, não teste de OCR/IA/DB.

> **Contrato-alvo sujeito a verificação no código atual e aceite da equipe.** Este arquivo espelha as expectativas observadas em `frontend-novo/src/services/api/{endpoints.ts,dto.ts,ApiService.ts}` em 16/10/2026. Alterações no cliente ou backend exigem atualização simultânea. Não é uma relação de endpoints atualmente implementados.

## 1. Convenções gerais

- Base URL: `/api`; localmente Vite usa proxy para `http://localhost:8080`.
- JSON `Content-Type: application/json; charset=utf-8` exceto upload multipart e health texto.
- Identificadores: UUID string opaca, não sequencial; timestamp ISO 8601 UTC com timezone.
- Campo `processingStatus`: `pending`, `processing`, `completed` ou `failed`.
- Categorias inicialmente: `Carros`, `Casa`, `Trabalho`, `Banco`, `Outros` (verificar código atual).
- Usuário autenticado obrigatório em recursos privados. 401/403 coerentes; cookie/CSRF documentados em `SECURITY.md`.
- Respostas de erro consistentes: `{ "code": "INVALID_FILE", "message": "Envie um PDF, PNG ou JPEG válido.", "requestId": "..." }`; nunca stacktrace ou OCR em erro.
- **Acesso ao recurso por ID também deve verificar owner.**

## 2. Endpoints essenciais

| Método | Caminho | Request | Resposta esperada |
|---|---|---|---|
| GET | `/api/analise/health` | — | `200 text/plain`, texto simples. Estado app, não prova dependências. |
| POST | `/api/documentos` | `multipart/form-data`: campo **`file`** e `categoryId` opcional | `202` ou `201` com `DocumentDTO`; processamento pode continuar. |
| GET | `/api/documentos` | — | `200 DocumentDTO[]` (array direto; frontend atual não usa `page.content`). |
| GET | `/api/documentos/{id}` | — | `200 DocumentDTO`. |
| DELETE | `/api/documentos/{id}` | — | `204 No Content`. |
| GET | `/api/documentos/{id}/analise` | — | `200 AnalysisDTO`, mesmo se `pending/processing`; se `failed`, explicitar warning. |
| GET | `/api/documentos/{id}/chat/mensagens` | — | `200 ChatMessageDTO[]` (array direto). |
| POST | `/api/documentos/{id}/chat/mensagens` | `application/json`, `{ "message": "..." }` | `200/201 ChatMessageDTO` de `role: "assistant"`. |
| GET | `/api/perfil` | — | `200 UserProfileDTO`. |
| PATCH | `/api/perfil` | `UserProfileDTO` ou payload compatível acordado | `200 UserProfileDTO`. |

**Observação:** `GET /api/analise/health` é o único endpoint comprovado no backend original, não se deve deduzir daí disponibilidade dos demais. O legado `POST /api/analise/contrato`, se reaproveitado, deve ser um adapter/alias opcional e **não substituir** os endpoints exigidos pelo frontend.

## 3. DTOs e exemplos

### DocumentDTO

```json
{
  "id": "f0bc7655-1bd8-4ef8-920f-92374737e452",
  "name": "Contrato de exemplo.pdf",
  "categoryId": "Casa",
  "processingStatus": "processing",
  "createdAt": "2026-10-16T18:00:00Z",
  "description": "Análise em andamento",
  "visualStatus": "pending"
}
```

**Obrigatórios para o parser existente:** `id:string`, `name:string`, `categoryId:string|null`, `processingStatus:enum`, `createdAt:ISO8601`. `description` opcional; `visualStatus` opcional com `ok|warning|pending`. Não retornar `categoryId` ausente: se não classificado, usar `null`. Responder a upload antes de finalizar OCR preferencialmente `202 Accepted`.

**Exemplo multipart:**

```http
POST /api/documentos HTTP/1.1
Content-Type: multipart/form-data; boundary=...
Cookie: sessão=...
X-CSRF-TOKEN: ...

file=<binário PDF/PNG/JPEG>
categoryId=Casa
```

Não estabelecer `Content-Type` manualmente no `fetch` com `FormData`: browser define o boundary.

### AnalysisDTO

```json
{
  "documentId": "f0bc7655-1bd8-4ef8-920f-92374737e452",
  "processingStatus": "completed",
  "summary": "Contrato de locação residencial com prazo definido.",
  "keyPoints": [
    {
      "id": "p1",
      "title": "Multa por rescisão",
      "description": "O texto menciona multa proporcional ao tempo restante (cláusula 8).",
      "severity": "warning"
    }
  ],
  "ocrQualityWarning": null,
  "facts": [
    { "label": "Prazo", "value": "12 meses" }
  ]
}
```

**Obrigatórios:** `documentId`, `processingStatus`, `summary:string|null`, `keyPoints:array` (pode ser `[]`). `severity`: `info|warning|critical`. `ocrQualityWarning` string ou null opcional. `facts[]` é suportado pelo domínio/UI, **mas em 16/10/2026 o parser `analysisFromDTO()` pode ainda não copiá-lo**: antes de habilitar API com facts, corrigir e testar o parser no `frontend-novo/` sob etapa de integração. Opcionalmente adicionar campo de evidência/rastreabilidade via versão de contrato acordada; não enviar estrutura arbitrária quebrando o parser.

Enquanto `pending`/`processing`: `summary:null`, `keyPoints:[]`, sem inventar texto. Em falhas, status `failed` e mensagem controlada, sem texto interno do provider.

### ChatMessageDTO

```json
{
  "id": "ca2aa72a-7473-48a0-8971-45cf537a2196",
  "documentId": "f0bc7655-1bd8-4ef8-920f-92374737e452",
  "role": "assistant",
  "content": "O contrato informa um prazo de 12 meses na cláusula 3.",
  "createdAt": "2026-10-16T18:03:00Z"
}
```

`role` só `user|assistant` **na API**; o frontend mapeia `assistant` para `ai`. Enviar mensagem:

```http
POST /api/documentos/f0bc7655-1bd8-4ef8-920f-92374737e452/chat/mensagens
Content-Type: application/json

{"message":"Qual é a multa para cancelar?"}
```

Retornar **um ChatMessageDTO**, não `{ reply: ... }`, pois o `ApiService` atual monta internamente `{reply: mensagem, followUps: []}`. `GET` devolve array ordenado incluindo mensagens `user` e `assistant`. Não enviar valores genéricos ou inventados como se fossem extraídos. Opcionalmente adicionar `clientTurnId` ou `Idempotency-Key` numa versão coordenada para retry sem duplicação; hoje `ApiService` não os encaminha automaticamente.

O fluxo mock contém pergunta guiada sobre renda. O modo API **não** deve reproduzir automaticamente essa pergunta sem decisão do backend. Perguntas guiadas futuras exigem contrato de mensagens estruturadas combinado com frontend.

### UserProfileDTO

```json
{
  "name": "Usuário de demonstração",
  "email": "usuario@example.test",
  "fontSize": "large",
  "explainLevel": "simple",
  "income": "prefer_not",
  "audio": false
}
```

`fontSize`: `small|medium|large`; `explainLevel`: `simple|normal|complete`; `income`: `less3|3to5|5to7|7to15|more15|prefer_not`; `audio`: boolean. No frontend atual todos esses campos são esperados; se renda não for salva por padrão, represente `prefer_not` e ofereça consentimento antes de atualizar para outra opção.

## 4. Autenticação — implementada na fase 2

| Método | Caminho | Request | Resposta |
|---|---|---|---|
| GET | `/api/auth/csrf` | público | 200 `{headerName:"X-CSRF-TOKEN",token:"..."}` + cookie de sessão se necessário |
| POST | `/api/auth/register` | JSON `{name,email,password}` + CSRF | 201 `{id,name,email}`, sem login automático |
| POST | `/api/auth/login` | JSON `{email,password}` + CSRF | 200 `{id,name,email}`, sessão autenticada com novo ID |
| GET | `/api/auth/me` | cookie da sessão autenticada | 200 `{id,name,email}` ou 401 |
| POST | `/api/auth/logout` | cookie + CSRF | 204 vazio, invalida sessão e expira cookie |

Sequência para cliente sob mesma origem/proxy Vite:

1. Buscar GET `/api/auth/csrf` e manter o cookie HttpOnly `KAUPSESSION` recebido pelo navegador.
2. Enviar `token` no header indicado (`X-CSRF-TOKEN`) em todas as operações de escrita, inclusive cadastro/login/logout. O token é mascarado; não decodificar ou buscar cookie XSRF.
3. Cadastro exige nome não vazio (até 120 caracteres), email válido (até 254, normalizado) e senha de 12–72 caracteres Unicode, no máximo 72 bytes UTF-8; não remover espaços da senha. Nome/email são normalizados pelo servidor.
4. Login troca o ID da sessão e elimina o CSRF anterior. Buscar GET `/api/auth/csrf` novamente antes da próxima escrita. `/me` é a fonte da identidade autenticada.
5. Logout somente POST: o cookie antigo deixa de autenticar. Para nova sessão, buscar CSRF novamente. GET logout não altera a sessão. Logout com CSRF anônimo válido também é 204 (idempotente).

Cookie: host-only, path `/`, HttpOnly, SameSite=Lax, Secure em prod/sem profile; dev usa HTTP local.
Sessão sem remember-me, inatividade padrão de 30 minutos, persistida em PostgreSQL e recuperável após
reinício. Respostas de auth/CSRF têm Cache-Control no-store. Tokens/senhas nunca vão para localStorage
nem query string. O servidor não libera CORS entre origens; no frontend futuro usar o proxy já existente.

Erros: 400 payload inválido, 401 credencial inválida/conta ausente/desabilitada ou sessão ausente/expirada,
403 CSRF ausente/inválido/antigo, 409 email já cadastrado (inclusive concorrência), 415 Content-Type incorreto,
429 limite de tentativas por IP. Erros de login usam a mesma mensagem para não expor o motivo interno.
Cadastro duplicado retorna conflito sem ecoar o email. O limiter é local ao processo (20 tentativas/60s),
compartilhado por cadastro/login, não confia em X-Forwarded-For. Não é limitação distribuída.

[OpenAPI dos endpoints implementados](openapi.yaml). O frontend ainda não possui login/CSRF; sua
adaptação fica na fase 8. Nenhum contrato TypeScript existente foi alterado nesta fase.

## 5. Contexto adicional — contrato opcional a negociar

O `ApiService.getContext()` atual apenas mostra o arquivo principal e `attachContext()` lança erro explícito. Proposta futura:

- `GET /api/documentos/{id}/contexto`: lista metadados de documentos relacionados.
- `POST /api/documentos/{id}/contexto`: multipart com `file` após autorização.
- `DELETE /api/documentos/{id}/contexto/{contextId}`: exclui associação/arquivo.

Não implementar silenciosamente sem combinar o esquema JSON com `ContextFile` e atualizar o `frontend-novo/`.

## 6. Erros e comportamento HTTP

| Condição | HTTP recomendado | Código de erro |
|---|---|---|
| Requisição inválida | 400 | `VALIDATION_ERROR` |
| Não autenticado | 401 | `UNAUTHORIZED` |
| Não permitido | 403 ou 404 | `FORBIDDEN` / `NOT_FOUND` |
| Documento inexistente/não acessível | 404 | `DOCUMENT_NOT_FOUND` |
| Limite de upload | 413 | `FILE_TOO_LARGE` |
| Tipo inválido | 415 | `UNSUPPORTED_FILE_TYPE` |
| Arquivo ilegível | 422 | `UNREADABLE_DOCUMENT` |
| IA não disponível | 503 | `AI_PROVIDER_UNAVAILABLE` |
| Muitas requisições | 429 | `RATE_LIMITED` |
| Falha inesperada | 500 | `INTERNAL_ERROR` |

Nunca responder 200 com análise inventada após falha; retornar estado persistido/erro real. Padronizar códigos e `requestId` para diagnóstico sem vazamento de texto.

## 7. Mudanças de contrato e versionamento

Antes de finalizar uma mudança: alterar DTO Java, serialização, documentação OpenAPI, testes HTTP, `frontend-novo/src/services/api/*` se necessário e teste end-to-end. Registrar divergência em `DECISIONS.md`. Não mudar `/api` para `/api/v1` sem atualizar proxy e cliente.
