# PROMPTS_FASES.md — Pedidos incrementais para o Codex

> Envie **um prompt por vez**, após verificar a entrega anterior. Em todas as fases o Codex deve ler `backend-novo/docs/AGENTS.md`, atualizar `backend-novo/docs/IMPLEMENTATION_PLAN.md`/`backend-novo/docs/DECISIONS.md`, executar testes e informar limites. Estes prompts não autorizam merge/push.

## Prompt — Fase 2: autenticação e autorização

> Leia `backend-novo/docs/AGENTS.md`, `backend-novo/docs/SECURITY.md`, `backend-novo/docs/API_CONTRACT.md` e `backend-novo/docs/IMPLEMENTATION_PLAN.md`. Implemente autenticação/sessão real no novo Spring Boot usando Spring Security e PostgreSQL, com cadastro, login, logout, `/api/auth/me`, hash seguro de senha e CSRF efetivo se usar cookie. Crie testes 401/403, sessão, logout e acesso por usuário. Não introduza bypass público nem alterar o frontend nesta etapa. Execute Maven testes/build, documente como testar via HTTP e só marque a fase como concluída após evidências.

## Prompt — Fase 3: documentos e upload

> Implemente `POST/GET/DELETE /api/documentos` e `GET /api/documentos/{id}` conforme `backend-novo/docs/API_CONTRACT.md`. Upload multipart deve usar campo `file`, aceitar PDF/PNG/JPEG com validação real, guardar bytes em storage privado, persistir `DocumentDTO`, categoria opcional e estado inicial; aplicar autorização por proprietário, limitar tamanho e devolver erros padronizados. Crie migrations e testes com PostgreSQL, arquivos inválidos e acesso cruzado A/B. Não usar OCR/Gemini simulado como se fosse real.

## Prompt — Fase 4: PDF/OCR e processamento durável

> Implemente leitura de PDFs digitais e OCR local em scans/imagens via adapter, com limites/timeout e fixtures sintéticas. Crie processamento durável e recuperável depois de reinício, com `pending/processing/completed/failed` e endpoint GET da análise sempre conforme contrato. Não faça chamadas longas dentro da transação; não exponha texto OCR em logs. Teste arquivos legíveis, ilegíveis, PDF malformado, concorrência e recuperação de job.

## Prompt — Fase 5: Gemini e análise estruturada

> Implemente cliente Gemini server-side com `GEMINI_API_KEY` e modelo configurável, schemas de JSON validados, classificação, resumo, fatos e pontos de atenção com origem/evidência quando possível. Respeite `backend-novo/docs/SECURITY.md` e jamais fabrique dados faltantes. Testes devem simular provider, inclusive timeout/429/schema inválido; smoke ao vivo somente opt-in e com dados sintéticos e credenciais locais. Se o provider não puder ser chamado, informe 'integração implementada mas não validada ao vivo'.

## Prompt — Fase 6: chat contextual e histórico

> Implemente GET/POST `/api/documentos/{id}/chat/mensagens`, com roles `user/assistant`, contexto extraído **somente do documento autenticado**, histórico ordenado, preferência de linguagem simples e resposta de incerteza quando não existe evidência. Não force a pergunta fictícia de renda existente no modo mock. Teste separação por owner/documentId, retry/duplicação, falha Gemini, limite de texto e prompt injection no PDF. Retornar `ChatMessageDTO` unitário no POST e array no GET para o `ApiService` atual.

## Prompt — Fase 7: perfil, anexos e exclusão

> Implemente GET/PATCH `/api/perfil` com enumerações exatas do `frontend-novo/src/types/domain.ts`, validação, consentimento/minimização de renda e exclusão de documentos/dados. Se adicionar anexos de contexto, documente novo contrato, implemente segurança e testes de associação; não presuma que `attachContext()` no frontend já funciona. Teste privacidade e limpeza do storage.

## Prompt — Fase 8: integração real com o frontend

> Após garantir as fases anteriores, configure o `frontend-novo` com `VITE_DATA_SOURCE=api` e Vite proxy para `localhost:8080`. Execute testes de contrato, configure sessão/CSRF e **somente agora** realize mudanças mínimas em `frontend-novo/` necessárias para login e consumo da API, sem redesenho nem remover o modo mock. Resolva diferenças detectadas (incluindo `facts` no parser de análise, se ainda ausente). Teste upload → processamento → resumo → chat → retorno ao histórico → exclusão. Não permitir fallback de API para mock.

## Prompt — Fase 9: hardening e entrega

> Execute build, `mvn verify`, testes de segurança/contrato, Docker Compose, E2E e auditoria de segredos. Verifique status/restart, logs sem PII, health, retenção, timeouts, autorização cruzada e documentação de execução. Faça relatório com features implementadas, métricas e comandos comprovados, pontos não verificados (Gemini real se faltar chave), riscos e arquivos alterados. Não afirmar readiness para produção sem auditoria externa.
