# API_CONTRACT_RASCUNHO.md — Contrato frontend/backend do Kaup

> **Contrato implementado e em uso** (atualizado em 2026-11-01). Fonte da verdade: os controllers em `backend/src/main/java/com/kaup/controller/` e os DTOs em `.../dto/`. O frontend os valida em `frontend/src/services/api/dto.ts`. Se um campo mudar, mudar nos dois lados.
>
> O nome do arquivo ficou de quando era uma proposta; ele continua com esse nome só para não quebrar referências.

## 1. Regras gerais

- **Sessão por cookie** (`JSESSIONID`, HttpOnly, 7 dias, guardada no Postgres). Sem sessão, qualquer rota fora de `/api/auth/*` responde `401`.
- **CSRF:** o backend entrega o cookie `XSRF-TOKEN`; o frontend o lê e o devolve no cabeçalho `X-XSRF-TOKEN` em todo `POST`, `PATCH` e `DELETE`. Isso exige que front e API fiquem no **mesmo domínio** (proxy do Vite em desenvolvimento, nginx no Docker).
- **Dono dos dados:** cada pessoa só acessa os próprios documentos (outro id responde `404`). O papel `ADMIN` lê todos, mas só o dono altera (`ownedByMe`).
- **IDs** são texto (UUID). O frontend rejeita número.
- **Erros** seguem RFC 7807 (`ProblemDetail`): `{ "type", "title", "status", "detail" }`, com `detail` em português. Códigos usados: `400`, `401`, `403`, `404`, `409` (e-mail já cadastrado), `413`, `415`, `429` (muitas tentativas de login), `502/503` (Gemini fora do ar).
- **Limites:** arquivo de até 10 MB; PDF, PNG ou JPEG; no máximo 5 anexos por documento.

## 2. Endpoints

### Autenticação (`/api/auth`) — públicos, exceto `sair` e `eu`
| Método | Rota | Corpo | Resposta |
|---|---|---|---|
| `POST` | `/cadastro` | `{name, email, password, confirmPassword}` | `201` `UsuarioDTO` e já abre a sessão |
| `POST` | `/entrar` | `{email, password}` | `200` `UsuarioDTO` (bloqueio temporário após várias falhas: `429`) |
| `POST` | `/recuperar/solicitar` | `{email}` | `204`; não envia e-mail e responde igual para qualquer endereço |
| `POST` | `/recuperar/redefinir` | `{email, code, password, confirmPassword}` | `204`. O código é fixo (`KAUP_CODIGO_FAKE`, decisão do projeto: sem e-mail real) |
| `POST` | `/sair` | — | `204` |
| `GET` | `/eu` | — | `200` `UsuarioDTO` |

### Documentos (`/api/documentos`)
| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/` | `multipart/form-data`: `file` (obrigatório) e `categoryId` (opcional). **Síncrono:** faz OCR, categoriza e analisa antes de responder (`201` `DocumentoDTO`, já `completed`). Pode levar dezenas de segundos. |
| `GET` | `/` | Lista os documentos da pessoa (o ADMIN vê todos). |
| `GET` | `/{id}` | Um documento. |
| `GET` | `/{id}/analise` | `AnaliseDTO`. |
| `DELETE` | `/{id}` | `204`. Só o dono. Apaga também conversa e anexos. |

### Chat (`/api/documentos/{id}/chat/mensagens`)
| Método | Descrição |
|---|---|
| `GET` | Histórico, do mais antigo ao mais novo. |
| `POST` | Corpo `{message}`. Devolve a resposta da IA (`MensagemChatDTO`, `role: "assistant"`). A pergunta e a resposta ficam gravadas. |

### Anexos de contexto (`/api/documentos/{id}/contexto`)
| Método | Descrição |
|---|---|
| `GET` | Lista os anexos (`AnexoDTO`). |
| `POST` | `multipart/form-data` com `file` (PDF, PNG ou JPEG). OCR e o texto passa a valer nas próximas respostas do chat. `201` `AnexoDTO`. |

### Perfil e conta
| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/api/perfil` | `PerfilDTO`. |
| `PATCH` | `/api/perfil` | Qualquer subconjunto de `name`, `email`, `fontSize`, `explainLevel`, `income`, `audio`, `shareIncomeWithAi`. Valores fora das listas permitidas dão `400`. |
| `GET` | `/api/conta/exportar` | Baixa `kaup-meus-dados.json` com tudo o que o sistema guarda da pessoa. |
| `DELETE` | `/api/conta` | `204`. Apaga a conta, os documentos e as conversas, e encerra a sessão. |

### Depuração
| Método | Rota | Observação |
|---|---|---|
| `GET` | `/api/analise/health` | Texto fixo; **não** prova que banco, OCR ou Gemini funcionam. |
| `POST` | `/api/analise/contrato`, `/api/analise/texto` | **Removidas.** Davam acesso ao Gemini sem gravar nada; o envio de contratos é só por `POST /api/documentos`. |

## 3. Modelos (formato real)

```ts
type ProcessingStatus = "pending" | "processing" | "completed" | "failed";

type UsuarioDTO = { id: string; name: string; email: string; role: "USER" | "ADMIN" };

type DocumentoDTO = {
  id: string;
  name: string;
  categoryId: string | null;     // Carros, Casa, Trabalho, Banco ou Outros
  processingStatus: ProcessingStatus;
  createdAt: string;             // ISO 8601
  description: string | null;    // primeira frase do resumo
  visualStatus: "ok" | "warning"; // "warning" = a IA listou pontos de atenção
  ownedByMe: boolean;            // falso quando o ADMIN olha documento alheio
};

type AnaliseDTO = {
  documentId: string;
  processingStatus: ProcessingStatus;
  summary: string | null;
  keyPoints: Array<{
    id: string;
    title: string;
    description: string;
    severity: "info" | "warning" | "critical"; // o backend hoje só produz info e warning
  }>;
  ocrQualityWarning: string | null; // hoje sempre null (ainda não calculado)
};

type MensagemChatDTO = {
  id: string;
  documentId: string;            // igual ao da URL; o frontend recusa se divergir
  role: "user" | "assistant";
  content: string;
  createdAt: string;
};

type AnexoDTO = { id: string; documentId: string; name: string; mimeType: string; createdAt: string };

type PerfilDTO = {
  name: string;
  email: string;
  fontSize: string;
  explainLevel: string;
  income: string;
  audio: boolean;
  shareIncomeWithAi: boolean; // falso = a renda nunca vai para a IA
  retentionDays: number;      // 0 = nunca apagar
};
```

## 4. Decisões já tomadas

- Autenticação: conta própria (e-mail e senha) com sessão em cookie. Sem Google e **sem verificação de e-mail** nem e-mail real (projeto universitário, ver `CLAUDE.md`).
- Formatos: PDF (digital ou escaneado), PNG e JPEG, até 10 MB; o tipo é conferido no servidor.
- OCR e IA atrás da interface `ServicoDeIA` (Gemini hoje); o OCR é o Tesseract (Tess4J).
- Persistência: texto extraído, análise, conversas e anexos ficam no Postgres; o arquivo original **não** é guardado. Retenção de 180 dias (`KAUP_RETENCAO_DIAS`).
- Exportação e exclusão de dados da conta disponíveis (LGPD).

## 5. Ainda em aberto

- **Upload assíncrono** (`202 Accepted` + consulta de status): ver `docs/planejamento/202.md`. Até lá o `POST /api/documentos` é síncrono.
- `/api/v1/` não foi adotado; sem previsão.
- Aviso de leitura ruim (`ocrQualityWarning`) e `severity: "critical"` ainda não são produzidos.
- Anexo avulso e conversa não podem ser apagados separadamente do documento.
- Cotas de uso e limite de requisições por IP (ver `docs/planejamento/SEGURANCA.md`), que adicionarão respostas `429` a mais rotas.

## 6. Regras de integração

- O frontend usa um único arquivo de serviços (`frontend/src/services/api/`); em modo `api` **nunca** mostra dados fictícios após um erro.
- **Nunca:** acessar o PostgreSQL pelo React, enviar `GEMINI_API_KEY` ao navegador, ou tratar `/health` como prova de que a análise funciona.
