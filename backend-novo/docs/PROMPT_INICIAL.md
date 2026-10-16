# Prompt inicial para o Codex — backend-novo do Kaup

Você está no repositório `mackcoder/Kaup`, branch experimental `dede_backend`, criada a partir de `frontend`. Sua missão é desenvolver um **backend novo, real e integrado**, dentro de `backend-novo/`, preservando `backend/` (legado) e o frontend existente.

## Antes de escrever código

1. Leia `backend-novo/docs/AGENTS.md` e **todos os outros .md de `backend-novo/docs/`**.
2. Examine o código presente em `frontend-novo/src/services/api/`, `frontend-novo/src/services/contracts/KaupService.ts`, `frontend-novo/src/types/domain.ts`, `frontend-novo/src/features/chat/` e os testes.
3. Leia `docs/Concepção/Requirements.md`, `docs/Concepção/Visao_e_CasoDeNegocio.md`, jornadas e wireframes relevantes.
4. Inspecione `backend/` e, se disponível localmente, `origin/naoto_back_BD:backend/` (sem merge, sem escrita). Identifique o que vale reaproveitar conceitualmente, mas não copie erros ou código quebrado.
5. Confira `git status`, branch atual, stack e versões instaladas. Verifique divergências entre a documentação e o código atual. **Não assuma que a documentação antiga é uma API já implementada.**

## Entrega desta primeira execução

- Atualize `backend-novo/docs/IMPLEMENTATION_PLAN.md` com um inventário factual, decisões em aberto, ordem de implementação e critérios de aceite verificáveis.
- Registre em `backend-novo/docs/DECISIONS.md` as escolhas iniciais (auth, persistência de arquivos, provider OCR, Gemini, processamento assíncrono e compatibilidade com a API frontend).
- Projete os pacotes e limites de responsabilidade, sem abstrações desnecessárias.
- **Implemente apenas a primeira fatia executável:** esqueleto Spring Boot em `backend-novo/`, Maven Java 17, profiles/configuração via env, `GET /api/analise/health`, PostgreSQL + migração inicial Flyway, Dockerfile e `compose.yaml` de desenvolvimento, tratamento padronizado de erros e primeiros testes. Não precisa integrar Gemini nesta execução.
- Se não houver ferramenta/rede para build, documente exatamente a limitação; nunca afirme que compilou sem executar.
- Não altere `backend/`, `frontend/`, `docs/` nem `docker-compose.yml` raiz. Não edite `frontend-novo/` nesta primeira fase.

## Visão das fases seguintes

Desenvolver, com aprovação de cada marco via testes, autenticação e ownership; CRUD/upload com processamento assíncrono; PDF/OCR; Gemini/análise estruturada; chatbot por `documentId`; perfil, preferências e exclusão; integração com o `frontend-novo/` em modo `api`; testes E2E e revisão de privacidade.

## Requisitos de comunicação

Ao terminar, informe (1) arquivos criados, (2) arquitetura decidida, (3) comandos efetivamente executados e resultados, (4) endpoints reais disponíveis, (5) o que continua mock/pendente, (6) o próximo prompt de `backend-novo/docs/PROMPTS_FASES.md` apropriado. Não chame uma base compilável de backend completo.
