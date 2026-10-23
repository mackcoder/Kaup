# BACKEND_SPECS.md — Especificação de produto e backend Kaup

## 1. Contexto

O Kaup ajuda pessoas com dificuldade de entender contratos. Seu frontend React, em `../../frontend-novo/`, demonstra documentos por categoria, status, resumo de cláusulas, chatbot por contrato, contexto/anexos e preferências. O backend deve tornar esses recursos reais sem alterar a experiência visual. Comunicação por API REST, nunca acesso direto do React ao banco ou ao provedor de IA.

**Produto-alvo:** MVP acadêmico integrado e demonstrável; não presumir robustez ou conformidade jurídica de um serviço bancário/jurídico em produção.

## 2. Stack e restrições

- Java 17 e Spring Boot 3.x, Maven, Spring Web, Validation, Security, Data JPA, PostgreSQL, Flyway, testes Spring/JUnit. Inspecionar versões e compatibilidade antes de instalar.
- OCR: extração local de PDF digital (PDFBox ou similar), extração de imagem/PDF escaneado por Tesseract/Tess4J ou adapter aprovado. Processamento externo somente se necessário e com consentimento/privacidade adequados.
- IA: adapter para Google Gemini via integração oficial ou HTTP estável com timeout, validação de schema, cotas e tratamento de erros. Modelo configurável; não assumir que o identificador legado é válido.
- HTTP em `/api`, backend na porta 8080 local; frontend Vite em 5174 já tem proxy `/api → localhost:8080`. SQL/migrations apenas no backend.
- Não criar endpoints que mudam a UI sem sincronizar contrato TypeScript. Não persistir informações financeiras desnecessárias.

## 3. Requisitos funcionais em ordem de entrega

### RF01 — Conta e sessão (obrigatório antes de dados pessoais reais)

Cadastro, login, logout, recuperação de sessão, identificação de usuário atual e perfil. Hash de senha robusto; cookies de sessão e CSRF ou alternativa segura documentada. Sem login via Google obrigatório para o MVP, desde que documentado como extensão futura. Sem bypass de auth em publicação.

### RF02/RF04 — Documentos, categorias e upload

Cada documento pertence a um usuário e recebe identificador estável. A API lista, consulta e exclui documentos; aceita upload real de PDF, PNG ou JPEG com limite configurável; valida extensão, MIME e assinatura binária, não confia em nome de arquivo. Admite `categoryId` opcional e categorização automática posterior. Categorias existentes no frontend: `Carros`, `Casa`, `Trabalho`, `Banco`, `Outros`; verificar constantes atuais antes de implementar. Não salvar nome/bytes do arquivo em logs.

### RF03 — Confirmação de foto

O frontend já é responsável pela pré-visualização e confirmação. O backend recebe apenas o arquivo confirmado, valida e detecta legibilidade insuficiente; retorna erro/alerta explícito quando apropriado. Não alegar que o backend controla a qualidade antes do envio.

### RF05 — Texto e OCR

Primeiro tentar ler texto de PDF digital; quando insuficiente, aplicar OCR a páginas/imagens com limites de tamanho, páginas, memória e tempo. Idioma prioritário português. Persistir status e indicadores de falha/baixa confiança; não converter uma saída vazia em análise inventada.

### RF08 — Análise estruturada

Após extração: classificar contrato, produzir resumo em português simples, identificar valores, parcelas, prazos, obrigações e cláusulas de risco **somente quando presentes no texto**. Retornar `summary`, `facts[]`, `keyPoints[]`, `ocrQualityWarning`; usar severidades `info|warning|critical` com justificativa baseada em evidências, sem qualificações jurídicas absolutas. Quando dado não existir, omitir ou sinalizar desconhecido. **Não reutilizar valores fictícios do mock.**

### RF06 — Chat por contrato

Perguntas/ respostas pertencem a um `documentId` e usuário autenticado. Recuperar contexto daquele contrato e anexos autorizados, histórico ordenado, tratar timeout/provider indisponível. Perguntas podem abordar multas, juros, rescisão e prazos, mas a IA deve referenciar somente fatos localizados ou reconhecer falta de evidência. O roteiro fixo de pergunta sobre renda pertence ao modo **mock**; em API, perguntas guiadas só devem aparecer se o servidor as fornecer de fato. Não coletar renda sem necessidade e escolha explícita.

### RF09 — Perfil e preferências

`GET/PATCH /api/perfil`: nome, email, `fontSize`, `explainLevel`, `income` opcional/consentida, `audio`. Validação exata das enumerações esperadas no frontend. A IA respeita nível de explicação quando disponível, sem vazar preferências pessoais para outros usuários.

### RF07 — Voz e narração

O frontend tem recursos demonstrativos. Não tratar narração/STT como já implementados no backend. Para MVP, preservar o fluxo textual end-to-end; analisar Web Speech API no cliente em etapa separada, com permissão e acessibilidade. Caso voz seja implementada, definir endpoints/contratos e testes específicos antes de anunciar RF07 como completo.

### Contexto e anexos

O frontend atualmente consegue listar contexto básico, mas `attachContext` do modo API pode ainda devolver erro por não existir contrato. Projetar upload de documentos relacionados com associação e autorização. Não bloquear a implementação do fluxo principal por esse recurso; registrar como pendência até integração do cliente.

## 4. Requisitos não funcionais

- **Segurança:** login, controle por proprietário, validação, restrição de tamanho e tipos, secrets fora do código, proteção de uploads, logs sem conteúdo sensível, política de exclusão e retenção.
- **Confiabilidade:** timeout por chamada externa, retries somente seguros e limitados, status persistido, falha visível, não perder estado em reinício. Evitar consumo excessivo de memória/processos para PDFs grandes.
- **Performance:** responder upload com ID/status sem aguardar a inferência inteira; permitir ao frontend mostrar carregamento imediatamente. Medir latência real, sem prometer 4 segundos em rede/IA externa.
- **Usabilidade:** erros em português compreensível; explicar documentos ilegíveis; retornar dados tipados e estáveis.
- **Auditabilidade:** IDs de correlação e logs estruturados com operação/status/duração, nunca texto do contrato ou a pergunta completa.
- **Testabilidade:** serviços reais behind ports; testes determinísticos com mocks/stubs de OCR/IA; integração com PostgreSQL real em ambiente de teste.

## 5. Cenários de aceitação

**Cenário A — Documento digital:** usuário autenticado envia `contrato_exemplo.pdf` com cláusulas conhecidas → recebe ID/status → consulta `/analise` até `completed` → valores e referências conferem com fixture → pode perguntar sobre prazo e obter resposta que não extrapola o conteúdo.

**Cenário B — Imagem:** usuário envia JPEG/PNG de contrato legível → OCR executa → análise aparece; imagem ilegível ou vazia termina em erro/aviso claro, sem resumo fictício.

**Cenário C — Dados isolados:** usuário A envia documento e conversa; usuário B tenta GET/chat/delete pelo ID A → retorna 404 ou 403 consistente, sem revelar existência/dados.

**Cenário D — Falha externa:** Gemini indisponível → documento fica `failed` ou análise é marcada indisponível segundo estratégia registrada → cliente exibe erro e retry apropriado, nunca resposta mock escondida.

**Cenário E — Retorno ao chat:** usuário reabre `/documentos/{id}/chat` → histórico da própria conversa reaparece ordenado e associado ao documento correto.

**Cenário F — Exclusão:** usuário exclui documento → desaparece da listagem, histórico/contexto associado é eliminado ou tornado irrecuperável conforme política; armazenamento físico limpo em prazo documentado.

## 6. Fora do escopo do MVP inicial

Treinamento de modelo próprio, recomendações jurídicas vinculantes, assinatura digital, pagamento, processamento de lotes muito grandes, dashboards comerciais, Google OAuth, aplicativos móveis nativos, múltiplos agentes IA, RAG vetorial obrigatório e narração de voz totalmente integrada. Podem ser evoluções posteriores, não justificativa para adiar upload/análise/chat.

## 7. Regra de verdade da integração

Mantenha três estados claramente distintos: **implementado e testado localmente**, **implementado mas requer credencial/provedor e não testado ao vivo**, **planejado/não implementado**. Nunca agrupar esses estados sob '100% concluído'.
