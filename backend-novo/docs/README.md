# Kaup — backend novo (fases 1–2)

Spring Boot 3.5.16 / Maven / Java 17, PostgreSQL 17.9, Flyway e autenticação por sessão
Spring Session JDBC. Health, cadastro, login, logout, identificação e CSRF são reais. **Não é o backend
completo**: documentos/upload, OCR/Gemini, análise, chat, perfil e exclusão ainda serão implementados.

## Executar localmente

Execute os comandos a partir da raiz do repositório; o código e a configuração continuam em `backend-novo/`.

```bash
cd backend-novo
if [ ! -e .env ]; then
  (umask 077; cp .env.example .env)
fi
# Defina DB_PASSWORD no arquivo local, sem versioná-lo.
docker compose config --quiet
docker compose up --build -d --wait
curl -i http://localhost:8080/api/analise/health
```

Resposta: `200 text/plain`, `Backend do Kaup está rodando.`. Health é liveness; não testa banco/OCR/IA a cada chamada. Startup depende de Flyway e PostgreSQL. Compose expõe apenas loopback: API 8080 e banco 55432, configuráveis, preservando banco legado em 5432. Nenhuma chave externa é necessária. Para testar autenticação, siga o roteiro de CSRF/cookies no runbook.

Para parar, `docker compose down` preserva o volume. Não usar `down -v` em dados existentes. Veja [RUNBOOK.md](RUNBOOK.md) para comandos de teste, configuração dev/prod e inspeção de migrations.

## Conteúdo

Os caminhos de implementação abaixo são relativos a `backend-novo/`; os links Markdown apontam para os documentos nesta pasta.

- `src/main/java/com/kaup/analysis`: health; `security`: recursos fechados e CSRF; `common/error` e `common/web`: erros e correlação.
- `src/main/resources/db/migration/`: V1 users e V2 nome/sessões. Nenhuma conta de exemplo; DDL exclusivamente Flyway.
- `src/main/java/com/kaup/auth`: cadastro/login/me/CSRF e hash; sessão e autorização em `security`.
- `src/test`: unidade, HTTP e integração PostgreSQL real; testes de isolamento usam fixtures exclusivas.
- [OpenAPI](openapi.yaml) e [API_CONTRACT.md](API_CONTRACT.md): endpoints e sequência CSRF/cookie.
- [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md): inventário, aceites e evidências; [DECISIONS.md](DECISIONS.md): escolhas/pendências; [ARCHITECTURE.md](ARCHITECTURE.md): limites dos módulos futuros.
- [API_CONTRACT.md](API_CONTRACT.md), [BACKEND_SPECS.md](BACKEND_SPECS.md), [DATABASE_MODEL.md](DATABASE_MODEL.md), [SECURITY.md](SECURITY.md) e [TEST_PLAN.md](TEST_PLAN.md) descrevem o alvo incremental, além do recorte implementado.
- [PROMPTS_FASES.md](PROMPTS_FASES.md): próximo marco **Fase 3: documentos e upload**. [AGENTS.md](AGENTS.md) rege todas as fases.

Os 13 documentos iniciais foram reunidos em `backend-novo/docs/`. O `backend-novo/AGENTS.md` contém apenas um encaminhamento para as instruções completas daqui.
