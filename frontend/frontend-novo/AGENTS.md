# AGENTS.md — Kaup / frontend-novo

## Missão

Você é responsável por **refatorar o frontend existente do Kaup** em `frontend-novo/`, preservando com máxima fidelidade a interface e a experiência atuais, mas criando uma base de código organizada, testável e pronta para a integração futura com o backend.

Leia **integralmente** `SPECS.md` antes de implementar. Consulte `API_CONTRACT_RASCUNHO.md` para o desenho *proposto*, ainda não implementado, das APIs. Este `AGENTS.md` estabelece regras de execução; `SPECS.md` contém os requisitos e critérios de aceite.

## Fontes de verdade e limites

1. **Visual, conteúdo e interações atuais:** `../frontend/src/` (atenção: a aplicação original está em `frontend/src`, não em `frontend/design`). Inspecione os cinco componentes de página, CSS, configuração e quaisquer dependências relevantes.
2. **Integração existente e stack real:** `../backend/`, `../docker-compose.yml`, especialmente `../backend/src/main/java/com/kaup/controller/AnaliseController.java`, `../backend/pom.xml` e `../backend/src/main/resources/application.properties`.
3. **Requisitos de produto:** `../docs/Concepção/Requirements.md` e documentação adjacente. Se documentação antiga e código atual divergirem quanto à stack, **o código atual prevalece**: Java 17, Spring Boot, PostgreSQL, Tess4J e integração Gemini planejada.
4. **Referência arquitetural secundária:** o projeto Otori pode inspirar separação de `pages`, `components`, `data`, `hooks` e `styles`, mas **não** deve alterar a identidade visual do Kaup. Não dependa de acesso ao repositório Otori para concluir a tarefa.

**Limite rigoroso de escrita:** crie/edite arquivos apenas dentro de `frontend-novo/`. Não altere, mova, renomeie ou apague nada em `../frontend/`, `../backend/`, documentação existente, `.gitignore` da raiz ou `../docker-compose.yml` sem autorização explícita. Não copie `.git/`, `node_modules/`, `dist/` ou plugins gerados do Figma desnecessários.

## Regras obrigatórias

- **Preservação visual é prioridade P0.** Preserve textos, hierarquia visual, tipografia Nunito, cores, espaçamentos, tamanhos, ícones/emojis, sombras, cartões, menus, transições, scroll, modais e o layout mobile-first centrado com largura máxima de aproximadamente 430 px. Não faça redesign ou mudanças "modernizadoras". Alterações visuais só para corrigir defeitos comprovados e devem ser registradas.
- **Preservação funcional é prioridade P0.** Todas as páginas, filtros, categorias, busca, navegação, seleção/captura de arquivos, chat demonstrativo, perfil, preferências, estados de carregamento, mensagens de confirmação e interações existentes devem continuar demonstráveis. Corrija eventos de upload sem `onChange` e outras inconsistências, preservando a experiência visual.
- **Não invente backend funcional.** Na branch atual, o endpoint existente confirmado é `GET /api/analise/health`; OCR, Gemini e repositório estão esboçados. Não presuma que outros endpoints existem. Não modifique o backend e não apresente integração simulada como real.
- **Mock desacoplado.** O protótipo deve funcionar por padrão em modo `mock`, sem servidor; prepare o modo `api` via serviços/adapters tipados. Não faça *fallback silencioso* de falha da API para dados falsos. Distinga claramente indicadores e respostas demonstrativas.
- **Dados e responsabilidades bem separados.** Componentes não devem importar dados mock de outros componentes nem conhecer URLs HTTP. Tipos/modelos não devem depender de JSX. Separe apresentação, estado/orquestração, domínio/dados e acesso remoto.
- **Acesso ao banco exclusivamente pelo backend.** Nunca conecte PostgreSQL diretamente do navegador, nunca exponha chaves do Gemini, senhas, tokens privilegiados ou segredos em variáveis `VITE_*`.
- **Privacidade.** Contratos e renda podem ser sensíveis. Não grave conteúdo de documentos, mensagens ou renda em `localStorage`, logs ou analytics. Se for necessário preservar dados temporários, use estado em memória e explicite limites. Não envie nada a provedores externos diretamente do browser.
- **Qualidade.** Use React + TypeScript + Vite, componentes funcionais, hooks com responsabilidade única, tipos explícitos, sem `any` injustificado e sem non-null assertions sobre dados remotos. Não adicione Redux, Next.js, microfrontends, bibliotecas grandes ou camadas abstratas sem necessidade.
- **Código limpo, não sobreengenharia.** Extraia componentes quando houver responsabilidade própria ou reutilização real. Prefira CSS organizado (CSS Modules ou estilos escopados) e tokens para evitar blocos enormes de estilos inline. Evite criar dezenas de arquivos vazios ou abstrações artificiais.
- **Build reproduzível.** Scripts `dev`, `build` (com checagem de tipos), `lint`, `test` e `preview`; dependências e lockfile consistentes; `.env.example` sem segredos; README claro. Prefira Node 22 compatível com a versão instalada do Vite.
- **Acessibilidade e UX.** Preserve e melhore apenas quando necessário labels, foco, navegação por teclado, estados disabled, mensagens de erro, feedback de upload, loading e comportamento em telas pequenas.

## Fluxo de execução esperado

1. Faça um **inventário** do original: telas, fluxos, textos, estilos, fontes, componentes e comportamentos; identifique as falhas e dependências técnicas. Registre um plano conciso dentro de `frontend-novo/` (pode ser `MIGRATION_NOTES.md`).
2. Defina a arquitetura e mapeie cada página/função original para o novo local. Confira os requisitos em `SPECS.md`.
3. Construa o novo projeto na pasta atual. Transfira a UI **sem alterar seu visual** e introduza rotas reais. Não use os plugins de deploy/preview específicos do Figma Make.
4. Isole mocks e implemente contratos TypeScript e serviços/adapters em `mock` e `api`. Permita trocar o modo por variável de ambiente documentada, mantendo `mock` como padrão. Os endpoints futuros devem ficar centralizados; não declare disponibilidade de endpoints que não existem.
5. Adicione estados de carregamento, falha, retry e validação de upload. Corrija o fluxo de envio para manipular o `File` de verdade: em mock, simule resultado de forma explícita; em api, construa `FormData` e encaminhe ao adapter (sem enviar ao endpoint health).
6. Execute o projeto e verifique lint, TypeScript/build e testes; se possível, compare **capturas lado a lado** do original e do novo em 390×844 e 430×932 e em viewport desktop (por exemplo 1280×800). Não afirme paridade visual sem a inspeção.
7. Faça uma passagem final de regressão pelas cinco telas e fluxos relevantes; registre diferenças restantes, limitações e contratos ainda aguardando implementação no backend.
8. Ao finalizar, apresente um resumo objetivo dos arquivos criados, decisões, comandos de execução, resultados dos testes, diferenças visuais não resolvidas e próximos endpoints a implementar.

## Critérios para encerrar

**Não considere concluído** se houver tela sumida, comportamento mock quebrado, alteração visual perceptível não documentada, importação de mocks diretamente pelos componentes, rota/endpoint fictício tratado como real, scripts quebrados, build/lint com erros ou mudança involuntária no frontend original/backend.

Se não conseguir validar algo, informe a limitação com precisão; **não declare teste visual, integração real com Gemini/OCR ou banco como aprovados sem evidência**.
