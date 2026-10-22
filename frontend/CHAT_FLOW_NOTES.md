# Restauração do fluxo demonstrativo do chatbot

## Correções

Comparação feita com `frontend/src/src/ContractChatPage.tsx`, mantendo a divisão de responsabilidades da refatoração. A limitação anterior dos uploads foi substituída pela jornada guiada completa: processamento demonstrativo, introdução gradual, resumo, pergunta e opções de renda, confirmação, digitação, resposta específica, aviso e chat livre.

Uploads mantêm File, preview, confirmação, cadastro e contexto. A introdução informa que o conteúdo não foi lido e não houve OCR. O card informa, junto dos valores, que se trata de um contrato fictício de exemplo e que esses números não foram extraídos do arquivo selecionado. Perguntas livres recebem respostas sobre esse exemplo, nunca sobre cláusulas supostamente lidas do upload.

## Estados e sessão

- `intro`: mensagens aparecem gradualmente; texto livre permanece bloqueado desde o início.
- `waiting-income`: selecionar uma opção apenas destaca a escolha. “Enviar resposta” confirma o envio.
- `responding-income`: escolha e composer bloqueados; usuário aparece uma vez, seguido de digitação. A resposta vem após 1,8 s e o aviso após mais 800 ms.
- `free-chat`: alternativas ocultas, histórico mantido e perguntas livres por palavras-chave, com resposta em 1,6 s.

A intenção (`income`/`free-text`) e o `turnId` são explícitos. O fluxo não usa `text.includes('R$')` nem a existência de qualquer mensagem do usuário. “Outra opção” e “Prefiro não informar” permitem continuar sem renda.

A sessão em memória é vinculada à instância de serviço e ao `documentId`. Ela mantém etapa, seleção, rascunho, histórico e tentativa pendente. Consultas de carregamento são canceladas ao sair; um envio já confirmado pode terminar na sessão mesmo fora da tela. Voltar durante ou depois da resposta conserva o estado. Introduções interrompidas são retomadas sem repetir mensagens. Recarregar a aplicação descarta a sessão; não há localStorage, logs ou envio do arquivo ao mock.

O bloqueio é síncrono antes de aguardar a requisição, protegendo cliques repetidos. Retry reutiliza o ID e atualiza a mesma mensagem. O adapter mock guarda o resultado por envio e reconhece uma repetição mesmo quando a primeira resposta se perdeu após a gravação. Resposta e aviso têm IDs estáveis e um único responsável: o serviço retorna o turno; o hook controla sua apresentação.

## API

O roteiro fixo é exclusivo da demonstração. A API continua com os caminhos e JSON propostos existentes; nenhum endpoint foi criado ou alterado. O adapter converte a resposta HTTP em um turno interno sem acrescentar pergunta de renda, resposta fictícia ou aviso jurídico. `ChatSendOptions` é metadado do cliente e não muda o payload HTTP `{message}`.

Na ausência do servidor, a UI exibe a falha e oferece retry, sem retornar mocks. Deduplicação na interface não garante idempotência de gravação no backend real: esse contrato ainda deverá ser definido com a equipe Spring Boot.

## Arquivos modificados

| Arquivos | Responsabilidade |
|---|---|
| `src/pages/ChatPage.tsx` | Composição da tela e ligação aos estados explícitos |
| `src/features/chat/useChat.ts`, novo `session.ts` | Orquestração, apresentação gradual, confirmação, retry, sessão por documento e controle de duplicação |
| `src/components/chat/MessageList.tsx` | Alternativas controladas e bloqueadas durante envio/tentativa pendente |
| `src/components/chat/ChatComposer.tsx` | Bloqueio explícito da entrada e do envio durante o roteiro |
| `src/components/chat/SummaryCard.tsx` | Aviso próximo aos valores do exemplo de upload |
| `src/components/chat/LoadingScreen.tsx`, `TypingBubble.tsx` | Etapas explicitamente simuladas, transparência da borda do spinner restaurada e estado de digitação acessível |
| `src/data/chat.ts` | Conteúdo fictício centralizado, resposta de renda opcional, valores e aviso |
| `src/services/mock/MockService.ts` | Mesma jornada para exemplos/uploads, respostas por intenção e histórico idempotente |
| `src/services/api/ApiService.ts`, `src/services/contracts/KaupService.ts`, `src/types/domain.ts` | Tipos de intenção/turno/etapa e adaptação HTTP sem roteiro local |
| `src/styles/index.css` | Estilo do aviso de origem dos valores; demais medidas/cores preservadas |
| Novo `src/tests/chat.test.tsx`, `src/tests/documents.test.ts` | Jornadas completas, faixas, falhas/retry e transparência do exemplo |
| Novo `scripts/chat-browser-check.mjs`, `scripts/browser-check.mjs`, `package.json` | Comparação focada e atualização da regressão de upload para o roteiro restaurado |
| `README.md`, `MIGRATION_NOTES.md`, este relatório | Documentação atualizada, limitada ao frontend novo |
| `validation/chat-browser-results.json`, nova galeria `validation/chat.html` | Evidências locais; PNGs permanecem ignorados pelo Git |

## Validação

- `npm run lint`: aprovado, sem warnings.
- `npm run test`: **44 testes aprovados**, em quatro arquivos. São 24 anteriores e 20 novos, incluindo as oito alternativas de renda.
- `npm run build`: aprovado com TypeScript estrito e Vite.
- Testes completos sob React StrictMode: seleção sem envio automático, confirmação única, digitação, resposta e aviso gradual, chat livre, upload PDF via confirmação, transparência do exemplo, renda opcional, navegação entre documentos, saída durante envio, introdução interrompida, falha antes de enviar e perda de resposta após gravação.
- API: teste React/HTTP verifica pergunta real do servidor, falha 503, manutenção da pergunta/rascunho e retry sem resposta/aviso/pergunta de renda locais, inclusive quando o usuário escreve “R$”. A regressão HTTP no Chrome também passou (`validation/api-browser-results.json`). Nenhum backend real foi validado.
- Comparação visual focada aprovada no Chrome em 390×844, 430×932 e 1280×800; estilos da alternativa selecionada conferidos e capturas de seleção/resposta inspecionadas lado a lado. Resultado registrado em `validation/chat-browser-results.json` e [galeria do chat](validation/chat.html).

## Diferenças intencionais em relação ao original

Aparência de cabeçalho, resumo, opções, botão, bolhas, composer, anexos e contexto preservada. As opções continuam visíveis e desabilitadas durante a confirmação/espera; o original as escondia imediatamente. A pergunta permanece no histórico e o chat livre só é liberado depois do aviso. Há uma alternativa adicional de renda opcional.

No upload, há texto extra de transparência e aviso junto aos valores do exemplo. As etapas de processamento dizem explicitamente que se trata de simulação. Respostas e resumo continuam identificados como fictícios. Histórico e etapa persistem entre rotas, ao contrário da tela monolítica anterior. A comparação visual usa as mesmas fontes Nunito locais nos dois navegadores, com interceptação apenas de Google Fonts no original, sem alterar seus arquivos.

Permanecem pendentes OCR/IA, autenticação e endpoints reais no Spring Boot. O frontend original, backend, README da raiz e demais arquivos fora de `frontend-novo/` não foram modificados nesta correção.
