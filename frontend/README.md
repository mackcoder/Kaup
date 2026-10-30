# Kaup — frontend refatorado

Aplicação com as cinco telas do protótipo, aparência original, React 19, TypeScript estrito, Vite e React Router.

## Executar

Requer Node **22.12+** (validado com 22.23.3) e npm.

```bash
cd frontend
npm install
npm run dev
```

Abra **http://localhost:5174**. O modo padrão é **mock**, sem backend. Não é necessário criar `.env` para demonstrar todos os fluxos.

```bash
npm run lint
npm run test
npm run build
npm run preview
```

`build` executa `tsc --noEmit` antes do Vite; `preview` serve `dist/`. `npm ci` reproduz o lockfile. `npm run format` formata o código. Nenhum plugin de deploy/preview do Figma Make é necessário. Nunito é servida localmente, inclusive sem conexão com Google Fonts.

## Telas e fluxos

| Endereço | Conteúdo |
|---|---|
| `/` | Home original, assistente azul, categorias, contadores e envio |
| `/categorias/Carros` | Categoria e documentos; também Casa, Trabalho, Banco e Outros |
| `/documentos` | Lista agrupada, busca e filtros; parâmetros `filtro=warning&busca=gol` |
| `/documentos/carros-1/chat` | Contexto, resumo, renda opcional, mensagens e anexos do documento |
| `/perfil` | Edição de nome/e-mail, explicações, letras, renda, privacidade e ajuda |
| `*` | Estado de página/categoria não encontrada |

Rotas funcionam por URL, reload e histórico do navegador. Os 25 exemplos iniciais têm IDs estáveis. Uploads e alterações ficam **somente na memória da sessão**: recarregar a página os descarta. Uma URL de documento recém-enviado só existe enquanto essa sessão estiver aberta. Em hospedagem estática, configurar fallback de rotas para `index.html`, preservando `/api` como backend.

O upload usa o `File` selecionado, valida PDF/PNG/JPEG de até **10 MiB**, permite prévia de imagem, confirmar nitidez, refazer seleção, cancelar e tentar novamente. A câmera usa `capture="environment"`; sua disponibilidade depende do navegador/dispositivo. Estes formatos e limite são uma decisão conservadora do frontend, **ainda não uma capacidade confirmada do backend**. Extensão e MIME são conferidos, mas a validação do conteúdo real deverá ser feita no servidor.

Em mock, os exemplos financeiros do protótipo são fictícios. Para arquivos enviados, nenhum OCR é realizado: a introdução informa que o conteúdo não foi lido, e o card identifica explicitamente os valores como pertencentes a um contrato fictício de exemplo, sem extraí-los do arquivo. A mesma jornada de renda opcional e perguntas livres fica disponível para exemplos existentes e novos uploads. O arquivo não sai do navegador. Renda é opcional; há “Prefiro não informar”. Históricos e arquivos de contexto ficam separados por `documentId`. Áudio, ajuda, exclusão de conta e logout são demonstrativos. A exclusão da conta não apaga dados do servidor. Exportação de informações informa sua indisponibilidade.

## Arquitetura

```text
src/
  app/                   rotas, provider e estado da sessão
  pages/                 composição das cinco telas
  components/            modal, upload e estados de recurso
    chat/                resumo, mensagens, contexto, composer e animações
    profile/             identidade, preferências, resumo e confirmações
  features/
    documents/           validação, filtros e contadores
    chat/                carregamento, polling, sessão, envio e retry
    profile/             estado de edição e ações do perfil
  types/                 domínio sem JSX
  data/                  fixtures fictícias, consumidas pelo adapter mock
  services/
    contracts/           interface KaupService comum aos dois modos
    mock/                estado demonstrativo em memória
    api/                 HTTP, DTOs, validação e endpoints centralizados
  styles/                configuração visual, tokens e CSS extraído do original
  tests/                 domínio, adapters HTTP e fluxos React
```

Páginas não importam fixtures nem conhecem URLs HTTP. O provider recebe um serviço e projeta os documentos para os componentes visuais. Categorias têm configuração visual própria; domínio e DTOs não carregam cores. `original.css` reúne 256 regras estáticas deduplicadas do JSX original; os identificadores escopados `kaup-*` preservam as declarações originais. Estilos condicionais e escala do perfil permanecem dinâmicos. Tailwind 4 é usado para preservar o reset original; não há template novo.

Estado sensível não é salvo em `localStorage`, logs ou analytics. Nenhuma chave de IA, acesso a PostgreSQL ou chamada Gemini existe no navegador. A etapa guiada usa estados `intro`, `waiting-income`, `responding-income` e `free-chat`, com metadados explícitos e IDs por envio; nunca deduz renda pelo texto ou por qualquer mensagem do usuário. Em modo API a autenticação é real (ver a seção abaixo); no modo demonstração não há contas. As preferências persistem entre telas durante a sessão; a escala de fonte mantém o comportamento do original no perfil.

## Modo API e Spring Boot

```bash
cp .env.example .env
# Edite .env:
# VITE_DATA_SOURCE=api
# VITE_API_BASE_URL=
# VITE_API_TIMEOUT_MS=60000
npm run dev
```

Com URL vazia, `/api` usa o proxy Vite para `http://localhost:8080`, e os cookies de sessão seguem sozinhos (mesma origem). Com `VITE_API_BASE_URL=http://localhost:8080` o frontend envia os cookies com `credentials: "include"`, e o backend precisa liberar a origem no CORS (ainda não configurado). A URL é a **origem**, sem sufixo `/api`. Em produção o proxy de desenvolvimento não existe: configure um reverse proxy `/api` com HTTPS. `VITE_API_TIMEOUT_MS` limita cada requisição (o padrão de 20000 ms é curto para o envio de um contrato; use 60000). Variáveis `VITE_*` entram no bundle: nunca coloque segredos nelas.

**Endpoints usados** (todos em `src/services/api/endpoints.ts`; formatos e validadores em `dto.ts` e `types/domain.ts`):

| Operação | Endpoint |
|---|---|
| Descobrir quem está logado | `GET /api/auth/eu` |
| Criar conta (já entra) / entrar / sair | `POST /api/auth/cadastro`, `/entrar`, `/sair` |
| Esqueci minha senha (simulado: sem e-mail, código fixo) | `POST /api/auth/recuperar/solicitar`, `/recuperar/redefinir` |
| Apagar a conta (e documentos, conversas) | `DELETE /api/conta` |
| Baixar meus dados (JSON) | `GET /api/conta/exportar` |
| Listar / enviar documento | `GET` / `POST /api/documentos` (envio: FormData com `file` e `categoryId` opcional) |
| Ver / excluir documento | `GET` / `DELETE /api/documentos/{id}` |
| Análise do documento | `GET /api/documentos/{id}/analise` |
| Histórico e pergunta ao chat | `GET` / `POST /api/documentos/{id}/chat/mensagens` |
| Perfil e preferências | `GET` / `PATCH /api/perfil` (o PATCH leva só o que mudou) |

**Sessão e segurança.** Sem login só aparecem as telas de entrar e criar conta, e o endereço pedido é mantido (depois de entrar, a pessoa continua onde queria). A sessão é um cookie que o JavaScript não lê; o backend entrega um token CSRF no cookie `XSRF-TOKEN`, que o `http.ts` devolve no cabeçalho `X-XSRF-TOKEN` em toda requisição que altera dados. Um 401 fora das telas de login (sessão expirada) leva de volta para o login; falha de rede ao descobrir a sessão mostra o erro, não a tela de entrar. Ao entrar ou sair, documentos, perfil e conversas da pessoa anterior são descartados. O modo demonstração (mock) continua sem login.

**Mensagens de erro.** O `http.ts` mostra o texto que o backend escreveu para o usuário (`detail` do RFC 7807). Um 5xx sem corpo (o proxy sem backend por trás) orienta a conferir se o backend está rodando; um 500 real do servidor mantém a mensagem genérica. Não há fallback para mocks em modo API.

**Limite de tentativas e recuperação de senha.** Depois de 5 senhas erradas seguidas o backend bloqueia aquele e-mail por 10 segundos (HTTP 429); a tela mostra a mensagem do servidor e a pessoa tenta de novo. A tela `/esqueci-senha` tem dois passos (e-mail; depois código e nova senha) e não deixa a pessoa logada: leva para entrar. Enquanto não há e-mail real, o código é sempre o do backend (`KAUP_CODIGO_FAKE`, padrão 676869); a variável opcional `VITE_FAKE_RESET_CODE` só faz a tela lembrar esse código em ambiente de teste (deixe vazia em produção). A conta de administrador nunca tem a senha alterada por aqui.

**Meus dados e prazo.** Em Configurações, "Baixar minhas informações" salva `kaup-meus-dados.json` (conta, preferências, documentos, análises e conversas, sem a senha) e um aviso informa depois de quantos dias os documentos são apagados (`retentionDays`, vindo de `GET /api/perfil`; sem o campo, o aviso some).

**Administrador.** A conta de administrador vê os documentos de todos. Ao abrir o de outra pessoa, o chat mostra um aviso de somente leitura e esconde o botão de excluir (o backend também recusa).

**Renda e IA.** Em Configurações, o interruptor "Deixar a IA usar minha renda" (ligado por padrão) controla se a faixa de renda é enviada à IA, só no chat. Desligado, ela nunca é enviada, mesmo preenchida. Nome e e-mail nunca vão para a IA.

## Verificação

Os testes automatizados (`npm run test`, Vitest + Testing Library) cobrem contadores/filtros, validação, upload e cancelamento, contexto/histórico por documento, perfil, multipart/202, timeout, erros/schema remoto, navegação e o fluxo do chat. O modo API é testado com respostas HTTP interceptadas no próprio teste; isso não substitui testes contra o backend real.

Checklist manual complementar:

- Conferir as cinco telas e scroll nas três resoluções; voltar/avançar do navegador e abrir URLs diretamente.
- Testar câmera em Android/iOS físico, foto escura/cortada, refazer e cancelar; hardware não é emulado pelo seletor automatizado.
- Testar foco, Tab/Shift+Tab/Escape em modais e leitor de tela no dispositivo desejado.
- Conferir filtros, busca vazia, renda opcional, tamanho de letras e preferências após trocar de tela.
- Em API real futura, confirmar CORS/autorização, tamanho de arquivos, respostas 401/403/413/415/422/429 e análise OCR ilegível.

## Diferenças intencionais e backend pendente

A estética original foi mantida. Diferenças documentadas: indicador discreto de dados fictícios; contadores corretos (4/5/6/4/6, total 25; status 15/6/4); análise e respostas explicitamente ilustrativas; contrato fictício de exemplo explicitamente identificado para upload; confirmação/preview/limites; opção de não informar renda; contexto alinhado à coluna no desktop; campos de perfil validados e modais com foco/Escape; erros HTTP visíveis. O chat usa altura dinâmica da viewport e rolagem interna para manter o composer acessível.

Pendências que continuam abertas (ver `../PLANO_INTEGRACAO.md`): anexar contexto ao chat, narração e entrada por voz, exportação dos dados da pessoa, política de retenção (LGPD), limite de tentativas de login, CORS e HTTPS para publicação, e o processamento assíncrono do envio.

## Restauração do chatbot demonstrativo

A jornada de renda pertence exclusivamente ao modo mock. Selecionar uma alternativa exige confirmação; durante o envio, alternativas e composer ficam bloqueados. Falhas preservam o envio e permitem retry com o mesmo ID, sem duplicar a mensagem, resposta ou aviso. O mock também reconhece envios já concluídos quando a resposta se perde. “Outra opção” e “Prefiro não informar” não exigem divulgar renda. Etapa, seleção, histórico e rascunho ficam na memória, separados por serviço/sessão e `documentId`.

A introdução revela mensagens em 0/600/1000/1400 ms; uploads têm processamento demonstrativo de 3,2 s e pausa inicial de 300 ms. A resposta de renda leva 1,8 s e o aviso aparece 800 ms depois. O modo API usa as mensagens efetivamente retornadas pelo servidor, sem acrescentar pergunta de renda, respostas fictícias ou aviso por detectar “R$” no texto. Idempotência de gravação no backend real ainda precisa de contrato próprio; a proteção atual do retry remoto evita duplicação na interface.
