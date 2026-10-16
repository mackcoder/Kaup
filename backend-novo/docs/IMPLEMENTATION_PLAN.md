# Plano de implementação — Kaup

Inventário em **16/10/2026**, branch `dede_backend`, HEAD `32172a3`. Esta execução autoriza somente descoberta e fase 1. Backend completo permanece pendente.

## Inventário factual

- Git inicial: somente `?? backend-novo/`. `frontend` e HEAD em `32172a3`; sem divergência local nessa base, merge ou fetch.
- As 13 especificações Markdown e `.env.example` estavam em `backend-novo/backend-novo/`. Lidas e movidas para `backend-novo/`, preservando instruções e corrigindo caminhos relativos. Não havia implementação nova.
- Lidos contratos, DTOs, HTTP, chat, testes Vitest e scripts de navegador de `frontend-novo/`. React 19, Vite 7, TypeScript 5, Vitest 5 declarados. Node instalado 22.23.3 e npm 10.9.9. Fonte padrão mock; Vite 5174 faz proxy /api para localhost:8080.
- Upload envia file e categoryId opcional; listas são arrays diretos. POST chat envia {message} e recebe mensagem assistant, convertida para ai. Status pending/processing/completed/failed; categorias Carros/Casa/Trabalho/Banco/Outros.
- analysisFromDTO realmente descarta facts. HTTP não lê JSON de erro/requestId nem envia CSRF; credenciais cross-origin não estão configuradas (mesma origem usa padrão fetch). turnId existe só localmente/mock, sem idempotência HTTP. API não força pergunta de renda. attachContext lança erro sem upload.
- Testes do frontend usam mocks/stubs e interceptação Playwright: não provam integração real. Sem alterações ou testes de frontend nesta fase.
- Lidos Requirements, Visão/Caso de Negócio, casos de uso, três jornadas/personas, WireFrame.md e imagem de chat. Lote/calendário/exportação/comparação entre documentos estão além do MVP inicial. Visão cita Node/Python/Tesseract.js e anonimização/exclusão imediatas: intenções antigas, não implementação nem garantias cumpridas. Java segue a instrução atual; retenção/consentimento exigem acordo antes de dados reais.
- backend/: Boot 3.2.0, Java 17 declarado, JPA/Postgres/Tess4J; apenas health textual, OCR/Gemini vazios, repository sem operações, ddl-auto=update e credenciais fixas. Não há análise real.
- origin/naoto_back_BD local em 647fefa inspecionada via git show/diff, sem escrita: DTOs/exceptions referenciados ausentes, sintaxe/imports inválidos, multipart arquivo incompatível, tentativa de OCR remoto, modelo sem validação e Gemini vazio. Reaproveitar só separação conceitual OCR/IA e validação de entrada.
- Host: Java/javac 25 Oracle; Maven, psql e JDK 17 ausentes. Docker Engine 29.1.3 e Compose 2.40.3 disponíveis. Outro Postgres em 5432 (app_postgres): preservar. Projeto Compose isolado usa 55432 opcional para acesso local.
- Boot 3.5.16/BOM consultados em fontes oficiais: Java mínimo 17, Maven mínimo 3.6.3; Flyway 11.7.2, JDBC PostgreSQL 42.7.11, Security 6.5.11 e Testcontainers 1.21.4 gerenciados. Build/testes usarão Maven/Temurin 17 em Docker.

## Ordem e critérios de aceite

| Fase | Entrega | Critério verificável | Estado |
|---|---|---|---|
| 0 | Descoberta | Fontes lidas, divergências e decisões registradas | Concluída |
| 1 | Base executável | Maven test/verify; Postgres real/Flyway; imagem não-root Java 17; Compose saudável; curl health; erros sanitizados; V1 não reaplicada após reinício | Concluída e verificada |
| 2 | Auth e isolamento | Cadastro/login/logout/me, sessão JDBC, hash, CSRF, testes 401/403 e A/B, sem editar frontend | Pendente |
| 3 | CRUD/upload | Owner obrigatório, PDF/PNG/JPEG validados, storage privado, 202/DTO, migrations e testes negativos | Pendente |
| 4 | PDF/OCR e jobs | Extração real, limites/lease, concorrência, recuperação após reinício e falhas persistidas | Pendente |
| 5 | Gemini/análise | JSON validado, evidências, timeout/429/schema inválido; fake explícito nos testes, live opt-in separado | Pendente |
| 6 | Chat | Contexto/histórico isolados, incerteza e retries sem duplicação acordados | Pendente |
| 7 | Perfil/privacidade | Enums, renda opcional, consentimento e exclusão relacional/física; anexos só após contrato | Pendente |
| 8 | Integração frontend | Login/CSRF, parser facts, erros/idempotência; fluxo real upload→análise→chat→exclusão | Pendente |
| 9 | Revisão final | Verify/E2E, privacidade, segredos, retenção, falhas/restart e limites de deploy | Pendente |

Cada fase precisa de testes e revisão do marco antes da seguinte. Não criar endpoints de negócio vazios.

## Recorte da fase 1

Pacotes presentes: bootstrap, analysis (health), security (proteger recursos sem login provisório), common.error e common.web. Futuros auth/documents/chat/profile e adapters só nas fases correspondentes.

V1 cria somente a base users, sem seeds, CRUD ou autenticação. JPA inicializa com ddl-auto=validate; constraints são verificadas diretamente em Postgres, sem entidade de auth antecipada. Profiles dev/prod e env, sem chave IA. Compose separado com portas no loopback. Nenhum adapter OCR/IA/storage vazio.

## Questões abertas

1. Retenção de binários/texto/análises/backups versus exclusão imediata antiga; consentimento e eventual anonimização sem prometer garantia automática.
2. Modelo Gemini habilitado, versão de API, quotas/custos; confirmar na fase 5.
3. Contrato de idempotência para chat/upload; cliente não envia turnId.
4. UX login, renovação de CSRF e apresentação de erro/requestId na fase 8.
5. Anexos, reprocessamento, retry e exclusão concorrente.
6. Limites medidos de páginas/resolução/tempo OCR e contexto IA.

## Evidências da execução

Pré-checagens: git status/branch/log/merge-base/diff/show/ls-tree, rg/cat, java/javac -version, mvn -version (ausente), docker version/compose version, node/npm --version e psql --version (ausente). Resultados de build/testes/smoke constam abaixo.

Fontes: [requisitos Boot](https://docs.spring.io/spring-boot/3.5/system-requirements.html), [BOM 3.5.16](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/3.5.16/spring-boot-dependencies-3.5.16.pom).


### Resultado da fase 1 — 16/10/2026

**Base executável concluída e verificada; backend completo e integração de negócio continuam pendentes.** Todos os arquivos novos/alterados estão em backend-novo/. Nenhum commit, push ou merge realizado.

| Comando efetivamente executado | Resultado observado |
|---|---|
| docker pull maven:3.9.11-eclipse-temurin-17 / postgres:17.9-alpine | Imagens obtidas; rede disponível |
| Maven via docker run conforme RUNBOOK, `mvn -B -ntp -Dmaven.repo.local=/tmp/maven/repository verify` | BUILD SUCCESS; 10 testes HTTP + 4 IT, 0 falhas/erros/ignorados; compila com release 17 e empacota jar |
| docker compose -f backend-novo/compose.yaml config --quiet | Configuração válida; senha local gerada em .env ignorado/permissão 0600, sem exibição do valor |
| docker compose -f backend-novo/compose.yaml build api | Imagem criada; Maven package -DskipTests dentro do build. Testes executados separadamente no verify, não no Dockerfile |
| docker compose -f backend-novo/compose.yaml up -d --wait --wait-timeout 60 e ps | API e Postgres healthy; apenas 127.0.0.1:8080 e :55432 expostos |
| curl -i GET localhost:8080/api/analise/health | 200 text/plain UTF-8, Backend do Kaup está rodando., X-Request-Id UUID |
| curl -i GET localhost:8080/api/perfil | 401 JSON UNAUTHORIZED com requestId; não é endpoint de perfil implementado |
| curl -i -X POST localhost:8080/api/documentos | 403 JSON FORBIDDEN por falta de CSRF; não é upload implementado |
| docker compose exec postgres psql: consultas flyway_schema_history/users | V1 create users com success=true, somente 1 migration e 0 usuários |
| docker compose exec api id / java -version | UID/GID 10001, Temurin 17.0.20.1 no runtime; build/testes com Temurin 17.0.17 |
| docker compose restart api, up -d --wait, curl e consulta Flyway | Health 200 após reinício, banco preservado, V1 continua única; log No migration necessary |
| docker run --rm com profile prod, JDBC localhost:1 e credenciais sintéticas | Exit 1 esperado; conexão recusada impede startup/Flyway. Não inicia API fingindo DB disponível |
| git diff --exit-code -- backend frontend frontend-novo docs docker-compose.yml | Sem alterações nos caminhos protegidos; app_postgres legado continua ativo na porta 5432 |
| git check-ignore .env/.m2/target e inspeção de whitespace dos 32 arquivos | Segredos/cache/artefatos ignorados; nenhum whitespace inválido encontrado |

Primeira tentativa de verify: 3 testes HTTP falharam porque a fixture de controller de teste não foi registrada no slice (404 em vez dos erros esperados). Corrigido com import explícito, sem adicionar rotas de teste ao aplicativo. Verify seguinte passou. Na revisão, respostas 500 também receberam Content-Type JSON explícito para Accept HTML; o verify final foi repetido e passou com os mesmos **14 testes**, em 41,072 s. Imagem reconstruída com a fonte final.

Evidências locais geradas: target/surefire-reports/com.kaup.HttpContractTest.txt, target/failsafe-reports/com.kaup.PostgresMigrationIT.txt e target/kaup-backend-novo.jar (ignorados pelo Git). Logs operacionais em /tmp/kaup-verify.log, /tmp/kaup-docker-build.log e /tmp/kaup-startup-invalid-db.log; nenhum segredo foi incluído neste relatório.

**Estado para revisão:** Compose local ativo; health em http://localhost:8080/api/analise/health. Para parar preservando dados: docker compose -f backend-novo/compose.yaml down. Banco legado não foi modificado. O health não executa query de DB a cada request: a prova de persistência é separada, por Flyway/SQL/IT. O profile prod teve apenas smoke negativo de banco indisponível; não houve deploy público nem auditoria de produção.

**Continua mock/pendente:** frontend padrão mock preservado; frontend modo api não possui login/CSRF/fluxo de negócio integrado. Auth/sessão JDBC/ownership, documentos, storage privado, jobs, PDF/OCR, Gemini, análise, chat, perfil, anexos, exclusão e E2E real ainda não implementados. Nenhuma integração externa de IA/OCR foi chamada. Não alegar anonimização, conformidade LGPD ou prontidão de produção.

**Próximo marco autorizado por novo prompt:** PROMPTS_FASES.md → “Prompt — Fase 2: autenticação e autorização”. Ler também SECURITY.md e API_CONTRACT.md; implementar e testar auth antes de dados de documentos, sem alterar frontend ainda.


### Revisão solicitada — 16/10/2026

Revisão limitada à primeira fatia. Probes HTTP reais reproduziram uma lacuna: métodos bloqueados pelo firewall (INVALID e TRACE) retornam 400 sem corpo JSON; TRACE também perde o header de correlação. O caminho sendError → /error volta a passar pelo firewall com o método original. O teste existente de dispatch ERROR verificava somente a existência de requestId no corpo, sem validar o header.

Plano de correção: testar essas requisições no servidor real; responder rejeições do firewall diretamente no contrato ApiError; garantir header/body coerentes no fallback servlet; executar verify, reconstruir imagem e repetir smoke. Nenhuma fase funcional nova será iniciada.

Na inspeção dos logs, uma requisição HTTP sintaticamente inválida reproduziu outro problema: o parser Tomcat registrou o alvo completo da requisição e stacktrace em INFO, incluindo um marcador sintético enviado na URL. A revisão também deve impedir esse log de conteúdo recebido e testar a rejeição sem eco do conteúdo. Erros rejeitados no parser, antes da aplicação, continuam fora do envelope JSON da API.

Correções aplicadas: handler direto de RequestRejectedException em SecurityConfig; header explícito no ApiErrorController; logger Http11Processor em ERROR; fixture ERROR completa no teste e exclusão de usuário automático no slice HTTP, alinhada à aplicação. README/RUNBOOK agora preservam .env existente, criam arquivo novo com 0600 e explicam como liberar 8080/manter o banco ao alternar Compose por Maven.

Regressões confirmadas antes das correções: 2 casos HTTP reais (TRACE/INVALID) falharam por ausência de Content-Type/JSON; o teste de dispatch ERROR com ERROR_REQUEST_URI falhou pela ausência de header; o teste de parser malformado falhou porque o marcador sintético apareceu nos logs.

Verificação final: `mvn -B -ntp -Dmaven.repo.local=/tmp/maven/repository clean verify` via Maven/Temurin 17 em Docker: **BUILD SUCCESS, 17 testes (10 HTTP + 7 integração), zero falhas/erros/ignorados**, 14,607 s. `docker compose config --quiet` e `docker compose build api` passaram. O jar foi inspecionado sem classes de teste, ErrorFixture ou .env. O comando de preparo do .env foi executado duas vezes em diretório temporário: confirmou modo 0600 e preservação do conteúdo existente.

Logs desta revisão: /tmp/kaup-review-regression.log, /tmp/kaup-review-methods-regression.log e /tmp/kaup-review-parser-regression.log documentam as falhas reproduzidas; /tmp/kaup-review-verify.log e /tmp/kaup-review-docker-build.log registram as verificações aprovadas. Relatórios Maven atualizados em target/surefire-reports e target/failsafe-reports. Nenhuma mudança em migrations ou diretórios protegidos; nenhum avanço para fase 2.

Smoke da imagem final após `docker compose up -d --wait --wait-timeout 60`: health 200, perfil 401, POST documentos 403 e TRACE/INVALID/caminho bloqueado 400; os erros da aplicação retornaram JSON com requestId igual ao header. Requisição HTTP malformada retornou 400 genérico sem marcador/stacktrace na resposta e sem marcador nos logs. API/Postgres healthy, migration V1 única e 0 usuários preservados; Compose local continua ativo.

### Organização da documentação — 16/10/2026

Os 13 Markdown iniciais foram movidos para `backend-novo/docs/`, por solicitação
do usuário. O AGENTS.md da raiz contém somente encaminhamento para as instruções completas.
Atualizados os caminhos relativos, os prompts e a estrutura descrita na documentação.
Esta alteração é documental; código, configuração, migrations e arquivos de ambiente não mudam.
