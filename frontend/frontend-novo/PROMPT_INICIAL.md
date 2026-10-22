# Prompt inicial para o Codex — copie o conteúdo abaixo

Quero que você **refatore integralmente o frontend atual do Kaup**, construindo a nova aplicação **exclusivamente em `frontend-novo/`**, sem alterar ou apagar o original.

**Antes de editar qualquer arquivo:**

1. Leia `frontend-novo/AGENTS.md`, `frontend-novo/SPECS.md` e `frontend-novo/API_CONTRACT_RASCUNHO.md` por completo.
2. Inspecione o frontend original em `frontend/src/` — especialmente `src/App.tsx`, `src/CategoryPage.tsx`, `src/AllDocumentsPage.tsx`, `src/ContractChatPage.tsx`, `src/ProfilePage.tsx`, `src/index.css`, `vite.config.ts` e os assets usados.
3. Inspecione o backend em `backend/`, especialmente `pom.xml`, `controller`, `service`, `repository`, `application.properties`, além dos requisitos em `docs/Concepção/Requirements.md`.
4. Antes de implementar, faça um plano com o mapa de telas e responsabilidades e registre-o em `frontend-novo/MIGRATION_NOTES.md`. Em seguida, **execute o plano completo**, sem parar após apenas criar a estrutura de pastas.

**Prioridade máxima: fidelidade visual e funcional.** Quero que o novo frontend seja **idêntico, ou o mais parecido possível, ao atual** em telas, estilos, fontes, cores, espaçamentos, cartões, interações, textos, modais, mensagens, animações e layout mobile-first com largura máxima de ~430 px. Isto é **refatoração, NÃO redesign**. Preserve e reaproveite o que for necessário; não substitua a estética por um template genérico nem pelo design do Otori.

Reestruture o código profissionalmente, com componentes reutilizáveis, páginas separadas, tipos, mocks, hooks, services e adapters. Use React + TypeScript + Vite, roteamento real, lint, TypeScript no build, testes úteis e documentação. O modo `mock` deve funcionar integralmente sem backend. O modo `api` deve possuir as integrações HTTP **preparadas** e configuráveis, mas não finja que endpoints inexistentes já funcionam. O frontend deve ficar preparado para PostgreSQL **via Spring Boot**, upload, processamento OCR, análise de contratos e chatbot por documento. Nunca conecte banco ou Gemini direto do React.

Corrija problemas existentes, como upload que ignora o arquivo, input de upload sem evento, contadores estáticos inconsistentes, navegação manual e estilos duplicados, **sem alterar desnecessariamente o resultado visual**. Os dados demonstrativos devem estar claramente identificados como fictícios; falha do modo `api` não pode cair silenciosamente em respostas mock.

**Restrição absoluta:** só modifique `frontend-novo/`. Não altere `frontend/`, `backend/`, Docker Compose raiz nem outros caminhos sem minha autorização. Crie README, `.env.example`, package.json e todos os arquivos necessários dentro da pasta nova.

Após implementar, rode `npm install`, `npm run lint`, `npm run test` e `npm run build` e, quando possível, compare a aplicação antiga e a nova lado a lado em resoluções mobile (390×844 e 430×932) e desktop (1280×800). Se não houver navegador automatizado disponível, forneça checklist manual de comparação e não declare inspeção visual como concluída.

**Entrega final:** código novo executável, instruções claras de execução, arquitetura/documentação, resumo do que mudou, resultados dos testes, eventuais divergências visuais e lista de pendências para a equipe backend. Não encerre após gerar somente um esqueleto: migre **todas as telas e fluxos existentes**.
