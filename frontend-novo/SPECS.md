# SPECS.md — Refatoração do frontend do Kaup

**Status:** especificação para implementação no `frontend-novo/`  
**Objetivo:** refatorar o frontend acadêmico atual sem redesign; obter uma base limpa e preparada para backend Spring Boot, PostgreSQL, OCR e IA.

## 1. Contexto atual

- Repositório: `mackcoder/Kaup`.
- Frontend canônico atual: `../frontend/src/` (projeto Vite/React/TypeScript gerado a partir de Figma Make). Arquivos de tela atuais em `../frontend/src/src/`: `App.tsx`, `CategoryPage.tsx`, `AllDocumentsPage.tsx`, `ContractChatPage.tsx`, `ProfilePage.tsx`.
- Backend em `../backend/`: Java 17 + Spring Boot 3, dependências para JPA/PostgreSQL e Tess4J; serviços OCR/Gemini ainda vazios; a API confirmada é `GET /api/analise/health`.
- A aplicação original usa uma stack interna de telas, dados estáticos espalhados nos componentes, extensos estilos inline e respostas simuladas para chat/avaliação. Isso funciona como protótipo, mas precisa de camadas para evoluir.
- **Atenção:** documentos antigos apresentam propostas Node/Python/Next.js e OpenAI/Tesseract.js; esta refatoração deve se orientar pela stack **efetivamente existente no código** e não migrar o backend.

## 2. Objetivos e não objetivos

### Obrigatórios (P0)

1. UI visualmente idêntica ou extremamente semelhante à existente: manter mobile-first com coluna centrada em desktop, largura máxima ~430 px, fonte Nunito, paleta, espaços, sombras, ícones, conteúdos e animações essenciais.
2. Mesmos caminhos funcionais de demonstração. Nenhuma tela/ação existente pode desaparecer.
3. Fonte dos dados separada da interface. Páginas não importam mocks diretamente; acessam hooks/use cases/serviços.
4. Tipos e contratos de API definidos, com clients e adapters que suportem mocks agora e servidor real depois.
5. Upload de arquivos capturado corretamente, com confirmação/pré-visualização para imagens e feedback; chat demonstrativo utilizável sem backend.
6. Roteamento real e estados loading/error/empty/success; integração futura não exige reescrever páginas inteiras.
7. Qualidade verificável: `npm run build`, `npm run lint` e `npm run test` (mínimo smoke/unitários críticos), `README.md` e `.env.example`.

### Desejáveis (P1, sem comprometer P0)

- Tratamento de perda de conexão, retry/cancelamento e timeout nas operações do adapter API.
- Acessibilidade de teclado e modal, ARIA para erros, testes de navegação/fluxos e verificação visual comparativa.
- `Dockerfile` do frontend e instruções para uso em conjunto com backend existente **sem** modificar o compose da raiz; criar arquivo de exemplo/override se realmente necessário.

### Fora do escopo desta refatoração

- Implementar OCR/Gemini, persistência real, autenticação, banco, migrações JPA, endpoints reais ou novo serviço no backend.
- Substituir a identidade visual, reescrever textos para marketing, redesenhar todas as páginas, transformar em Next.js ou criar app nativo.
- Prometer análise financeira/jurídica real com base em texto ou números fixos.

## 3. Referência visual e funcional — contrato de paridade

Inspecione o original *antes* de escrever. Preserve no mínimo:

| Tela original | Comportamentos e elementos a preservar |
|---|---|
| `App.tsx` / Home | Cabeçalho e saudação, card azul do assistente, ação de enviar documento, modal de foto/arquivo, cards de categorias com contadores, atalho para todos os documentos, dica inferior; layout de 430 px. |
| `CategoryPage.tsx` | Categoria, cores e ícones próprios, lista de documentos, status, ações de seleção/upload e retorno. |
| `AllDocumentsPage.tsx` | Listagem geral, filtros de status, busca textual, contadores, abertura de documento e retorno. |
| `ContractChatPage.tsx` | Loading demonstrativo, mensagens, resumo do contrato, escolha de faixa de renda, respostas demo, avisos, campo de mensagem, indicador de digitação, popup de contexto, menu de anexo, retorno e autoscroll. |
| `ProfilePage.tsx` | Informações do perfil, editar nome/e-mail, preferências de explicação/tamanho da fonte/renda, detalhes dos documentos, confirmações de exclusão/logout, ajuda e simulações de áudio. |

**Regra de ouro:** comparações visuais devem usar as mesmas viewport e o mesmo estado inicial. Preserve layout mobile centrado também quando a janela for desktop, como no original. Não imponha novas margens, cabeçalhos ou estilos genéricos do Otori.

**Melhorias funcionais autorizadas sem redesign:** corrigir inputs sem handler, sincronizar contadores de categorias aos documentos reais/mocks, navegação do browser, gerenciamento do foco, validação e feedback claros; registrar quaisquer pequenas diferenças inevitáveis.

## 4. Arquitetura pretendida

Estrutura sugerida (pode ajustar nomes com justificativa; não crie arquivos vazios só para cumprir a árvore):

```text
frontend-novo/
├── AGENTS.md
├── SPECS.md
├── API_CONTRACT_RASCUNHO.md
├── README.md
├── .env.example
├── package.json
├── package-lock.json
├── index.html
├── tsconfig.json
├── vite.config.ts
├── src/
│   ├── main.tsx
│   ├── app/                 # Router, composição e providers mínimos
│   ├── pages/               # Home, Category, Documents, Chat, Profile, 404
│   ├── components/          # Cards, botões, modais e demais peças reutilizáveis
│   ├── features/
│   │   ├── documents/       # Estado/hook, upload e operações de documentos
│   │   ├── chat/            # Estado/hook e regras da conversa
│   │   └── profile/         # Preferências e estado de perfil
│   ├── types/               # Entidades/DTOs de domínio, independentes de React
│   ├── services/            # Contratos de serviços e adapters mock/api
│   │   ├── api/             # fetch wrapper, configuração, normalização de erros
│   │   ├── mock/            # Implementações demonstrativas
│   │   └── contracts/       # Interfaces e DTOs por domínio
│   ├── data/                # Fixtures estáveis: categorias/documentos/chat
│   ├── hooks/               # Hooks compartilhados, apenas quando necessários
│   ├── styles/              # Tokens, CSS global e estilos de componentes
│   └── tests/               # Testes de unidades e smoke
└── ...
```

### Divisão de responsabilidades

- **Pages:** compõem a tela e coordenam estado; não implementam HTTP ou regras complexas de análise.
- **Components:** elementos visuais, controlados por props; evitar importação de mocks ou acesso direto ao backend.
- **Features/hooks:** gerenciam estados de tela, carregamento, erros e ações dos usuários.
- **Services:** interface única usada pela UI; seleciona adapter `mock` ou `api` por configuração, sem *fallback silencioso*.
- **Data:** apenas fixtures e conteúdo demonstrativo centralizado, com IDs estáveis.
- **Types:** `Document`, `DocumentCategory`, `DocumentAnalysis`, `ChatMessage`, `UserProfile`, `UploadState`, DTOs do servidor; versões explicitas conforme necessário.
- **Styles:** manter a fidelidade pixel/visual da UI original e reduzir repetição de CSS inline. Tokens para cores/espaçamentos; CSS Modules ou estilos escopados são aceitáveis.

**Evite:** camada de banco de dados no React, mocks importados por cada página, chamadas fetch dispersas, domínio representado por props de cor/emoji, funções de 400 linhas, controllers Java no frontend e estados falsos de autenticação apresentados como segurança real.

## 5. Dois modos de dados

### `mock` (padrão)

- Deve funcionar em qualquer máquina com `npm install` e `npm run dev`, **sem backend, PostgreSQL, Gemini ou OCR**.
- Use dados fictícios coerentes, organizados e identificados como demonstração.
- Operações devem refletir o estado da aplicação durante a sessão (ex.: upload entra na listagem; categoria e contadores se atualizam; chat mostra contexto do documento selecionado). Persistência entre reloads não é obrigatória.
- Um upload demonstrativo **não pode fingir leitura real** do PDF/imagem: deixe claro que a análise produzida é ilustrativa. Não invente dados financeiros como se viessem do arquivo.

### `api` (opt-in)

- Selecionado por `VITE_DATA_SOURCE=api`; URL configurável via `VITE_API_BASE_URL` em `.env`, e/ou proxy Vite documentado. `VITE_*` **nunca** deve conter segredos.
- Clients centralizados usando `fetch`, `FormData` para upload e JSON nos endpoints apropriados, tipagem de request/response, tratamento consistente de erros, abort/timeout e estados de loading.
- Não implementar fake server de produção; qualquer endpoint futuro ainda não implementado deve gerar erro explícito e informativo.
- `GET /api/analise/health` é **o único endpoint backend atualmente confirmado**; demais URLs de `API_CONTRACT_RASCUNHO.md` são sugestões sujeitas à equipe backend.
- A UI deve exibir dados reais quando o backend passar a disponibilizar contratos compatíveis; não incorporar nomes/campos de banco diretamente no JSX.

## 6. Requisitos para o fluxo futuro

### 6.1 Documentos, captura e OCR

Fluxo previsto: selecionar/capturar arquivo -> validar tipo/tamanho e permitir pré-visualização/confirmar nitidez (especialmente imagem) -> enviar pelo adapter -> receber `documentId` e status -> acompanhar processamento OCR/análise -> exibir resumo e documentos -> abrir chat associado ao documento.

- Capturar efetivamente `File`; não apenas navegar para chat.
- Validar tipos suportados com base nos formatos reais aceitos pelo backend futuro; se não houver confirmação, adotar configuração documentada e conservadora; não prometer suporte universal a DOCX/TXT.
- Centralizar IDs, status (`pending`, `processing`, `completed`, `failed`) e erros de processamento; mapear separadamente o status visual `ok`, `warning`, `pending` do protótipo.
- Interface para permitir tentativa novamente, cancelamento quando aplicável e mensagem útil em falhas/ilegibilidade.
- Análise deve ter resumo estruturado e pontos de atenção tipados; valores financeiros apenas quando retornados/calculados a partir de dados concretos e verificados pelo backend.

### 6.2 Chat

- Mensagens têm ID, papel (`user`/`assistant`), `documentId`, conteúdo, timestamps opcionais e estado de envio.
- Toda pergunta/resposta pertence a um documento específico e usa o `documentId`/contexto apropriado.
- Em mock, respostas ilustrativas existentes podem ser mantidas, **explicitamente rotuladas**, para fins de paridade visual e demo.
- Em api, não use `getFreeTextResponse` ou respostas de palavras-chave se o servidor falhar. Exiba falha/retry; não fabrique uma resposta da IA.
- Isolar componente de mensagem e composer de entrada da lógica de sessão e transporte. Histórico e stream podem ser acrescentados sem reconstruir o layout; streaming/SSE é opcional e não precisa ser implementado agora.

### 6.3 Perfil, preferências e privacidade

- Modelar `UserProfile` e preferências (nível de explicação, escala de fonte, renda opcional, áudio) sem presumir endpoints funcionais.
- Em mock, preservar telas e interações já existentes; exclusão/logout devem permanecer claramente demonstrativos, sem alegar apagar dados reais em servidor.
- Nunca salvar renda, conteúdo do contrato ou conversa em `localStorage` ou logs no navegador. Evitar enviar dados sensíveis sem autenticação/autorização reais.
- Autenticação real (RF01) é requisito do produto, mas **fora do escopo de implementação**: projetar local adequado para provider/cliente de autenticação e deixar suas dependências documentadas, sem inventar proteção.

### 6.4 Áudio

- Preservar controles existentes sem remover visual. Se áudio estiver apenas simulado no original, rotular como demo. Preparar interface para possível Web Speech API no futuro, sem impor uso agora.

## 7. Roteamento sugerido

- `/` Home
- `/documentos` lista geral, com filtro via query string quando apropriado
- `/categorias/:categoryId` documentos de uma categoria
- `/documentos/:documentId/chat` chat de um documento
- `/perfil` perfil
- `*` página não encontrada (consistente com estilo Kaup)

Rotas devem permitir recarregar/compartilhar URL e botão voltar do browser. IDs devem ser estáveis. Não exigir backend para navegar em modo mock.

## 8. Qualidade, ambiente e documentação

- Preferir **React 19 + TypeScript + Vite**, compatíveis com original; usar `react-router-dom` ou equivalente adequado. Não mudar framework.
- Executar TypeScript como parte do script `build` (`tsc --noEmit` ou build mode equivalente antes de `vite build`). Configurar lint e formato consistentes; usar `npm` + `package-lock.json` (ou documentar claramente escolha alternativa).
- `README.md`: arquitetura, árvore resumida, comandos, modos `mock`/`api`, variável ambiente, endpoints existentes vs propostos, limitações, como comparar visual e como integrar backend.
- `npm run test`: incluir ao menos testes de fonte de dados/contadores, filtro, validação de arquivo e adapter mock; se viável, componentes com React Testing Library. Não criar testes "verdes" irrelevantes.
- Erros remotos: loading, empty, error, retry; evitar `!` após `find()` sem fallback.
- CSS acessível: contraste, `:focus-visible`, labels, toque em mobile, responsividade, sem regressão visual.
- O build novo deve funcionar independentemente do projeto Figma Make. Simplificar Vite para plugins necessários, manter fontes e assets visuais usados no original.
- Docker: se incluído, usar Node compatível e servidor adequado. Nunca repetir Dockerfile original que só roda `tail -f /dev/null`. Configuração de integração com Compose existente pode ser entregue como documentação/arquivo opcional dentro de `frontend-novo/`.

## 9. Critérios de aceite (checklist)

### Paridade

- [ ] Home em 390×844, 430×932 e 1280×800 visualmente fiel; layout mobile centrado no desktop.
- [ ] Category, All Documents, Chat e Profile visualmente fiéis e acessíveis.
- [ ] Textos, ordem de blocos, paleta, tipografia, sombras, badges, ícones, scroll, modais e transições principais preservados.
- [ ] Navegação em todas as telas; botão voltar, filtros, busca, seleção de categoria/documento e perfil funcionando.
- [ ] Upload por arquivo/foto abre seleção; valida e retém `File`; pré-visualização/confirmar imagem; mock rotulado.
- [ ] Chat demo continua funcional; nenhum texto enganoso é apresentado como análise real do arquivo.
- [ ] Preferências/perfil continuam operáveis em modo mock.

### Arquitetura

- [ ] `frontend-novo/` independente, com sua própria configuração e documentação.
- [ ] Componentes pequenos e focados; dados e estilos desacoplados de JSX quando possível.
- [ ] Tipos de domínio e DTOs separados, sem acoplamento entre páginas.
- [ ] Interfaces/implementações mock e api para documentos, análises, chat e perfil, proporcionais ao estado atual do backend.
- [ ] Contrato da API proposto centralizado, não falsamente considerado existente.
- [ ] `VITE_DATA_SOURCE=mock` padrão e `api` opt-in; falhas de api **não** retornam mocks silenciosamente.
- [ ] Nenhum acesso direto ao PostgreSQL/Gemini/Tesseract a partir do browser.

### Execução

- [ ] `npm install`, `npm run dev`, `npm run build`, `npm run lint` e `npm run test` funcionam.
- [ ] `.env.example`, `README.md` e anotações da migração completos.
- [ ] Nenhuma modificação fora de `frontend-novo/`.
- [ ] Relatório final lista validações executadas, diferenças visuais restantes e dependências da equipe backend.

## 10. Entrega final esperada do Codex

Ao concluir, explique: (1) mapa original -> novo, (2) arquitetura e decisões de componentes/estado, (3) como iniciar em mock, (4) como mudar para api, (5) endpoints e capacidades backend ainda pendentes, (6) testes executados/resultados, (7) eventual diferença visual com evidências e (8) próximos passos sugeridos para um PR revisável.
