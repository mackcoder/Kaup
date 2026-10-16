# Plano e inventário da migração Kaup

## Leitura prévia (16/10/2026)

Lidos integralmente os documentos fornecidos em `frontend-novo/frontend-novo/` (vieram em uma pasta duplicada), as cinco telas, `index.css`, `main.tsx`, package/tsconfig/Vite do original, controller/services/repository/configuração/pom do backend, compose e requisitos. A aplicação nova ficará diretamente em `frontend-novo/`. Os documentos recebidos serão preservados.

O original usa apenas emojis e formas CSS; não há imagens importadas. Nunito 400/600/700/800/900 vem do Google Fonts; será servida localmente. Layout de 430 px, fundo #f5f7fa, margens de 18–20 px, cartões arredondados, gradientes por categoria e feedback de toque serão reaproveitados. Não usar Otori nem plugins Figma Make.

## Mapa e responsabilidades (antes da implementação)

| Original | Novo | Responsabilidade |
|---|---|---|
| App / home + stack | pages/HomePage + app/App | Home visual preservada; BrowserRouter com histórico real |
| CategoryPage | pages/CategoryPage | Hero por categoria, estatísticas e documentos; IDs válidos ou 404 |
| AllDocumentsPage | pages/DocumentsPage | Busca, filtros em query string, grupos, estado vazio |
| ContractChatPage | pages/ChatPage + components/chat + features/chat | Cabeçalho/composer/mensagens/contexto separados de transporte e sessão |
| ProfilePage | pages/ProfilePage + features/profile + components/profile | Perfil, preferências em memória, edição, áudio/ajuda/logout/exclusão explicitamente demonstrativos |
| Upload repetido/incompleto | components/UploadDialog + features/documents | Seleção/câmera reais, File validado, preview, confirmação, cancelamento, erro/retry |
| Constantes dispersas | data + types + services/mock | 25 documentos fictícios com IDs estáveis, histórico por documento |
| CSS/inline | styles + configuração visual de categorias | Extrair declarações estáticas sem alterar medidas; manter somente estilos dinâmicos necessários |
| Sem transporte | services/contracts + services/api | fetch tipado/validação, timeout/AbortSignal, multipart, endpoints propostos centralizados |

Rotas: `/`, `/documentos?filtro=...&busca=...`, `/categorias/:categoryId`, `/documentos/:documentId/chat`, `/perfil`, `*`.

## Etapas autorizadas

1. Criar React 19 + TS estrito + Vite independente, npm/lockfile, ESLint e Vitest/Testing Library.
2. Transferir o JSX/estilo original para preservar hierarquia, medidas, textos, transições e todos os blocos de perfil/chat. Unificar upload e estados de recurso.
3. Introduzir domínio e adapters mock/api. Provider de sessão em memória; contadores derivados de uma única lista. Sem localStorage para conteúdo, renda ou mensagens.
4. Completar upload real e preview; mock nunca lê OCR nem atribui números financeiros ao arquivo. Chat/contexto por ID; falhas HTTP não retornam fixtures.
5. Completar edição/preferências, busca/filtros e navegação; acessibilidade e validações proporcionais.
6. Executar npm install, lint, testes e build. Usar Chrome/Playwright se disponível para regressão funcional e capturas comparativas em 390×844, 430×932 e 1280×800. Não declarar inspeção sem executá-la.
7. Registrar resultados, diferenças intencionais e pendências backend no README e neste relatório.

## Correções visuais/funcionais previstas

- Contadores iniciais passam de 2/1/3/1/4 para 4/5/6/4/6; perfil acompanha 15 aprovados, 6 em atenção e 4 aguardando (contagem confirmada por teste).
- Pequeno indicador de demonstração/dados fictícios; números do resumo original apenas nos exemplos fictícios. Upload tem confirmação nova exigida por RF03 e limites conservadores PDF/JPEG/PNG de 10 MiB.
- Menus de anexo/contexto passam a selecionar arquivos reais. Em API, anexar ao contexto informa dependência de contrato ainda ausente, sem inventar associação.
- Corrigir popup de contexto preso à borda direita do desktop, mantendo-o dentro da coluna.
- Health só prova disponibilidade da aplicação; não prova OCR/Gemini/PostgreSQL.

## Backend confirmado e pendências

Java 17 / Spring Boot 3.2 / JPA / PostgreSQL / Tess4J. Somente GET /api/analise/health existe. Implementar documentos/upload/status/análise/histórico/chat/perfil/exclusão, autenticação/autorização, retenção/LGPD, CORS e limites de arquivo. Rascunho é proposta; nenhum arquivo backend será alterado.

## Validação e resultado

Implementação executada integralmente: cinco páginas, rotas reais, hooks de chat/perfil, upload compartilhado, domínio/DTOs, adapters mock/api e 256 regras de CSS extraídas e deduplicadas. As fontes Nunito estão no próprio bundle. Documentos de entrada preservados na pasta duplicada e copiados para a raiz do projeto novo.

- `npm install`: concluído; `package-lock.json` incluído. Auditoria final: 0 vulnerabilidades conhecidas (`validation/npm-audit.json`).
- `npm run lint`: aprovado, sem warnings.
- `npm run test`: **24 testes aprovados**, em 3 arquivos, incluindo sessão, contadores/filtros, validação/cancelamento de arquivo, contexto/histórico, multipart/202, schema, timeout/rede, navegação, edição e conservação do formulário em erro.
- `npm run build`: aprovado, TypeScript estrito e Vite. Bundle JS ~331 kB (~103 kB gzip), fontes servidas localmente.
- Chrome/Playwright: ver relatórios `validation/browser-results.json`, `validation/api-browser-results.json` e comparações em `validation/index.html`. Rodada final aprovada: 3 viewports, cinco telas + contexto, quatro grupos de fluxos mock e quatro verificações HTTP em modo API; sem erros JavaScript nos dois relatórios. Capturas lado a lado inspecionadas, incluindo Home, categoria, lista, chat, perfil e posição do contexto no desktop.
- `git diff --name-only` vazio e `git status --short` mostra apenas `?? frontend-novo/`: nenhum arquivo rastreado do original/backend/compose foi alterado.

## Diferenças finais e limites

A hierarquia, espaçamentos e medidas originais foram mantidos. Foram verificados lado a lado Home, Categoria, Documentos, Chat, Perfil e Contexto nas três resoluções solicitadas. Home, categorias, lista e perfil conservaram a altura total do original nas capturas. O chat foi comparado com scroll no início da conversa para evitar comparar posições diferentes.

Diferenças intencionais: aviso discreto de demonstração; contadores derivados; rótulos ilustrativos em resumo/respostas; números financeiros somente nos fixtures; renda opcional; preview/validação/confirmação do arquivo; feedback HTTP/timeout/retry; popup de contexto alinhado à coluna desktop; foco/Escape em confirmações; fonte local; mensagens de privacidade e logout/exclusão explicitamente demonstrativas. O chat usa `100dvh`, composer acessível e rolagem interna. A opção adicional “Prefiro não informar” aumenta a lista de renda do chat.

A referência automatizada foi o dist já existente do original, servido somente para leitura. A inspeção prévia do JSX confirmou os mesmos blocos e conteúdo; não houve build/escrita na pasta original. Comparação visual não equivale a prova de identidade pixel a pixel em todos os browsers. Emojis dependem do sistema operacional.

Limites restantes: câmera em dispositivo físico, VoiceOver/TalkBack e integração com backend real não foram testados. Áudio e ajuda continuam demonstrativos, assim como no protótipo. Auth, exportação e exclusão real de conta não estão implementados; anexar contexto na API depende de contrato ainda ausente. Há timeout por requisição; o polling segue enquanto o backend reportar processamento pendente, até sair da tela ou falhar.

O modo API foi verificado com respostas HTTP interceptadas **apenas no teste**: endpoint ausente falha sem mock; File multipart; HTTP 202 e polling; resumo retornado; chat 503 mantém pergunta e retry; perfil GET/PATCH. Isso não declara endpoints reais implementados.

## Pendências para a equipe backend

1. Implementar e aprovar contratos de upload, documentos, status/análise, histórico/chat, perfil e exclusão por documento.
2. Concluir OCR/Tess4J, classificação, resumo estruturado e Gemini no Spring; retornar avisos de legibilidade/incerteza e valores fundamentados no documento.
3. Definir autenticação/autorização e isolamento por usuário/documento; integrar credenciais HTTP ao adapter.
4. Definir armazenamento, retenção/consentimento/LGPD, exportação e exclusão de conta/documentos.
5. Aprovar formatos/MIME, validação real do arquivo, limites, CORS, erros e estratégia assíncrona. Decidir contrato de anexos e narração/áudio.
6. Manter PostgreSQL e segredos exclusivamente no backend; health não comprova funcionamento desses serviços.
