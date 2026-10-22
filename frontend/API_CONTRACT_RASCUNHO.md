# API_CONTRACT_RASCUNHO.md — Proposta de contrato frontend/backend Kaup

> **Documento de planejamento, NÃO é uma API já existente.** A equipe de backend precisa aprovar nomes, respostas, autenticação, tamanho de arquivo, políticas de armazenamento e como representar OCR/IA. **Não implemente automaticamente estes endpoints em `../backend/` durante a refatoração de `frontend-novo/`.**

## 1. O que existe hoje

`GET /api/analise/health` em `AnaliseController.java` devolve texto simples indicando que o backend iniciou. `OcrService`, `GeminiService` e `AnaliseRepository` estão declarados sem implementação funcional. O backend tem dependências Spring Boot, JPA/PostgreSQL e Tess4J, mas ainda não apresenta API de documentos/chat.

## 2. Proposta de endpoints REST (TODOS abaixo exceto health são futuros)

| Método | Endpoint proposto | Finalidade |
|---|---|---|
| `GET` | `/api/analise/health` | Existente: health de aplicação, **não** valida OCR/IA/DB |
| `POST` | `/api/documentos` | Upload `multipart/form-data`, campo `file`, metadados opcionais; retorna ID e status |
| `GET` | `/api/documentos` | Lista por usuário autenticado; suporta filtros/paginação quando acordado |
| `GET` | `/api/documentos/{id}` | Metadados/status de um documento |
| `GET` | `/api/documentos/{id}/analise` | Resultado OCR/síntese quando disponível |
| `POST` | `/api/documentos/{id}/chat/mensagens` | Enviar pergunta e receber resposta contextualizada |
| `GET` | `/api/documentos/{id}/chat/mensagens` | Histórico, se persistência autorizada |
| `GET` | `/api/perfil` | Dados e preferências do perfil autenticado |
| `PATCH` | `/api/perfil` | Atualiza preferências e campos permitidos |
| `DELETE` | `/api/documentos/{id}` | Exclusão conforme política definida |

**A implementação frontend em modo `api` deve usar um único arquivo de configuração de endpoints/serviços**; caminhos podem ser alterados depois que a equipe definir o contrato real. A resposta HTTP da aplicação pode incluir `202 Accepted` para processamento assíncrono; o frontend não deve supor análise instantânea.

## 3. Modelos exemplificativos (apenas proposta)

```ts
type ProcessingStatus = "pending" | "processing" | "completed" | "failed";

type DocumentDTO = {
  id: string;
  name: string;
  categoryId: string | null;
  processingStatus: ProcessingStatus;
  createdAt: string; // ISO 8601
};

type AnalysisDTO = {
  documentId: string;
  processingStatus: ProcessingStatus;
  summary: string | null;
  keyPoints: Array<{
    id: string;
    title: string;
    description: string;
    severity: "info" | "warning" | "critical";
  }>;
  ocrQualityWarning?: string | null;
};

type ChatRequestDTO = { message: string };

type ChatReplyDTO = {
  id: string;
  documentId: string;
  role: "assistant";
  content: string;
  createdAt: string;
};

type ApiErrorDTO = {
  code: string;
  message: string;
  requestId?: string;
};
```

### Exemplos de payload (propostos)

```http
POST /api/documentos
Content-Type: multipart/form-data

file=<arquivo binário>
```

```json
{
  "id": "doc-123",
  "name": "Contrato de aluguel.pdf",
  "categoryId": null,
  "processingStatus": "pending",
  "createdAt": "2026-10-16T10:00:00Z"
}
```

```http
POST /api/documentos/doc-123/chat/mensagens
Content-Type: application/json

{"message":"Qual é a multa por rescisão?"}
```

## 4. Questões que precisam ser decididas com o backend

1. **Autenticação e autorização:** login próprio, provedor externo, cookie HttpOnly ou bearer; garantir isolamento de documentos por usuário e acesso a cada `documentId`. Sem isso, não afirmar que há privacidade real.
2. **Formato de arquivo:** PDF digital, PDF escaneado, PNG/JPEG e tamanhos máximos; validação de MIME, tipo real e limites no servidor.
3. **Processamento assíncrono:** usar polling de status, notificações ou SSE; códigos HTTP; limites de timeout e retry.
4. **OCR/IA:** interface de entrada/saída, avisos de baixa legibilidade, classificação, resumo, explicação simples, referência a cláusulas e indicadores de incerteza.
5. **Dados pessoais/LGPD:** consentimento, retenção, anonimização antes de APIs externas, exclusão, segurança em trânsito e repouso e logs.
6. **Persistência:** entidades JPA, política para guardar arquivos originais, extração OCR, análises e conversas. Armazenamento binário não deve ser projetado no frontend.
7. **Configuração CORS e origem:** preferir proxy Vite no desenvolvimento; em produção, URLs/origens confiáveis e HTTPS.
8. **Contrato de erros:** esquema de resposta, `401`, `403`, `404`, `413`, `415`, `422`, `429`, `500`, `503`; mensagens úteis e não sensíveis.
9. **Versionamento:** considerar `/api/v1/` antes da primeira integração real se a equipe achar necessário.

## 5. Estratégia de integração

- **Agora:** o frontend utiliza adapters mock para a demonstração completa. O adapter API existe com métodos tipados, validação de estado e mensagens claras quando endpoint real estiver ausente.
- **Primeira integração recomendada:** upload -> ID -> status -> resumo; depois listagem persistida, chat contextualizado, perfil/autenticação.
- **Nunca:** acessar PostgreSQL a partir de código React, enviar `GEMINI_API_KEY` para browser, criar respostas fake em modo `api` após erro, ou tratar health como prova de que a análise está funcionando.
