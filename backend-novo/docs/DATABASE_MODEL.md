# DATABASE_MODEL.md — Modelo relacional proposto (PostgreSQL)

**Esquema implementado:** V1 cria users; V2 adiciona name e as tabelas Spring Session JDBC
(spring_session e spring_session_attributes), com índices de sessão única/expiração/principal e
FK de atributos ON DELETE CASCADE. Nenhuma conta é criada pela migration. Linhas prévias de users
recebem nome neutro Usuário; novos cadastros devem informar nome. V1 não foi alterada.
JPA valida users; Spring Session usa SQL próprio e initialize-schema=never. O principal é o UUID
da conta; futuras exclusões de conta deverão invalidar sessões por esse índice. Documentos, perfil,
análises, jobs e chat continuam ausentes. O sequenciamento V3+ seguirá os respectivos marcos.

> Especificação de domínio para orientar entidades JPA e migrações Flyway. Nomes e constraints finais serão confirmados após inventário e registrados em `DECISIONS.md`. Não execute este documento como SQL.

## 1. Entidades principais

| Entidade/tabela sugerida | Campos relevantes | Regras |
|---|---|---|
| `users` | `id UUID PK`, `email` único normalizado, `password_hash`, `created_at`, `enabled` | sem senha em claro; senhas sempre hash; e-mail não vai a logs |
| `user_profiles` | `user_id PK/FK`, `name`, `font_size`, `explain_level`, `income_range`, `audio`, `updated_at` | renda opcional; default `prefer_not`; consentimento quando exigido |
| `documents` | `id UUID PK`, `owner_id FK`, `original_name`, `storage_key`, `mime_type`, `size_bytes`, `category_id`, `visual_status`, `processing_status`, `created_at`, `updated_at` | índices por owner/status; storage_key privado; nome sanitizado |
| `document_texts` | `document_id PK/FK`, `extracted_text`, `method`, `page_count`, `quality_warning`, `extracted_at` | altamente sensível; retenção e acesso restritos; sem indexar texto livre indiscriminadamente |
| `document_analyses` | `id UUID PK`, `document_id UNIQUE FK`, `summary`, `model_id`, `schema_version`, `created_at` | análise versionada; sem sucesso falso |
| `analysis_points` | `id UUID PK`, `analysis_id FK`, `title`, `description`, `severity`, `source_location`, `sort_order` | evidência por página/cláusula quando disponível |
| `analysis_facts` | `id UUID PK`, `analysis_id FK`, `label`, `value`, `source_location`, `sort_order` | fatos derivados de texto, nunca constantes de demo |
| `chat_messages` | `id UUID PK`, `document_id FK`, `role`, `content`, `created_at`, `turn_id`, `sequence` | roles `user|assistant`; histórico ordenado; ownership via documento |
| `processing_jobs` (se necessário) | `id`, `document_id`, `status`, `attempts`, `lease_until`, `last_error_code`, `created_at` | processamento assíncrono recuperável após reinício |
| `document_context_files` (futuro) | `id`, `document_id`, `storage_key`, `name`, `mime_type`, `uploaded_at` | upload de apoio com mesmo owner; associação explícita |

Para autenticação por sessão, considerar tabelas de sessão Spring Session JDBC ou mecanismo equivalente testado. Não inventar coluna de token reutilizável em claro.

## 2. Relacionamentos

```text
users 1 ─── 1 user_profiles
users 1 ─── N documents
                  ├── 1 ── 0..1 document_texts
                  ├── 1 ── 0..N processing_jobs
                  ├── 1 ── 0..1 document_analyses
                  │                   ├── N analysis_points
                  │                   └── N analysis_facts
                  ├── 1 ── N chat_messages
                  └── 1 ── N document_context_files (futuro)
```

## 3. Regras de integridade

- IDs UUID gerados pelo servidor, não derivados do nome do arquivo.
- `documents.owner_id NOT NULL`; consultas sempre filtradas pelo usuário autenticado. FK sozinha **não** substitui autorização.
- `category_id` validada por conjunto fechado ou tabela `categories`; integrar aos IDs do frontend (`Carros`, `Casa`, `Trabalho`, `Banco`, `Outros`). Categorização automática pode atualizar sem alterar id.
- `processing_status` é enum lógico `pending|processing|completed|failed`, com check constraint ou enum mapeado; timestamps em UTC.
- `document_analyses.document_id UNIQUE` caso exista só análise atual; se auditoria de versões for necessária, introduzir `analysis_version`, mantendo consulta da versão corrente definida.
- Deleção do documento: limpar análise, mensagens, anexos e storage de forma consistente/assíncrona verificável. Nunca deixar conteúdo pessoal acessível após exclusão confirmada.
- Índices sugeridos: `users(lower(email)) UNIQUE`, `documents(owner_id, created_at DESC)`, `documents(owner_id, processing_status)`, `chat_messages(document_id, sequence/created_at, id)`, `processing_jobs(status, lease_until)`.

## 4. Conteúdo do arquivo e segurança

Não colocar binários grandes na base por padrão. Guardar fora de diretórios públicos, nome gerado aleatoriamente, path nunca construído a partir do filename recebido. `storage_key` só para o backend. Em desenvolvimento, storage local em volume privado com permissão restrita; para produção, bucket privado com controle de acesso e criptografia. Para texto OCR armazenado, planejar criptografia em repouso/backups e política de retenção; nenhum endpoint deve expor OCR integral por padrão.

## 5. Migrations Flyway

- `V1__create_users.sql`: aplicada na fase 1, imutável.
- `V2__add_auth_and_sessions.sql`: aplicada na fase 2, nome e sessões JDBC.
- V3+: documentos/constraints/índices na fase 3; extração/análises/jobs e chat nas fases respectivas.
- Perfil e anexos terão migrations próprias quando seus contratos forem implementados.

Os nomes são ilustrativos; criar migrations só quando a etapa for implementada. **Nunca editar migration já executada em banco compartilhado**: criar nova migration de alteração. Spring `ddl-auto=validate` após migrar. Testar criação limpa e atualização entre versões.

## 6. Persistência e idempotência

- Upload validado deve resultar em um documento com status persistido. Falha na gravação do binário não pode deixar documento utilizável sem arquivo; tratar compensação/limpeza.
- Jobs longos não devem manter transação de banco aberta durante chamadas externas.
- Chat: considerar `turn_id` único por documento para evitar duplicidade em retry; adicionar ao contrato/client apenas em mudança coordenada.
- Transições de status com controle de concorrência (optimistic locking ou update condicional) para evitar dois workers concluírem o mesmo job.

## 7. Retenção/exclusão

Definir período e base legal de retenção com o grupo. Para protótipo local, usar fixtures sintéticas; para dados reais, exigir autenticação, consentimento onde necessário, exclusão integral verificável e política de logs/backups. Excluir conta inclui documentos, análises, chat, arquivos e sessões associadas. Não prometer remoção instantânea de backups sem processo definido.
