# Operação e testes — fase 1

## Ambiente de desenvolvimento

```bash
cd backend-novo
if [ ! -e .env ]; then
  (umask 077; cp .env.example .env)
fi
# Edite DB_PASSWORD localmente. Docker Compose lê .env; Maven não.
docker compose config --quiet
docker compose up --build -d --wait
docker compose ps
curl -i http://localhost:8080/api/analise/health
curl -i http://localhost:8080/api/perfil
```

Resultados esperados: health 200/texto; perfil 401 JSON, sem endpoint de login ainda. POST sem CSRF retorna 403. Endpoints de documentos/perfil/chat não estão implementados; o filtro protege esses caminhos desde a base. Nada retorna dados fictícios.

Portas no loopback: 8080 (API_PORT) e 55432 (DB_PORT); ajuste .env se ocupadas. Compose usa projeto kaup-backend-novo, rede e volume próprios, sem usar app_postgres em 5432. Não imprimir `docker compose config` completo em relatórios, pois pode incluir a senha; usar --quiet.

```bash
docker compose exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT version, description, success FROM flyway_schema_history"'
docker compose exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT count(*) FROM users"'
docker compose restart api
docker compose up -d --wait
curl -i http://localhost:8080/api/analise/health
docker compose down
```

V1 deve aparecer uma vez, users vazio na instalação limpa. `down` preserva dados; não apagar volume para contornar migração com checksum inválido. Corrigir via nova migration, nunca modificar V1 já aplicada em banco compartilhado.

## Maven com Java 17 local

```bash
mvn -B -ntp test
mvn -B -ntp verify
# Ao executar a API pelo Maven, libere 8080 e mantenha só o banco no Compose.
docker compose stop api
docker compose up -d --wait postgres
export SPRING_PROFILES_ACTIVE=dev
# Exporte DB_PASSWORD com o mesmo valor do .env; não inclua senha no histórico.
# Se alterou DB_NAME, DB_USER ou DB_PORT no .env, exporte esses valores também.
mvn spring-boot:run
```

`test` executa os testes HTTP sem banco. `verify` também empacota e executa *IT com PostgreSQL real/Testcontainers; Docker é obrigatório, testes não são silenciosamente ignorados. O jar é target/kaup-backend-novo.jar. O Dockerfile empacota com -DskipTests porque não recebe socket Docker no build; a evidência de testes vem do verify separado.

## Maven via Docker em Linux (sem instalar JDK/Maven)

Execute da raiz do repositório:

```bash
mkdir -p backend-novo/.m2
docker run --rm --network host \
  --user "$(id -u):$(id -g)" \
  --group-add "$(stat -c '%g' /var/run/docker.sock)" \
  -e MAVEN_CONFIG=/tmp/maven \
  -v "$PWD/backend-novo/.m2:/tmp/maven" \
  -v "$PWD/backend-novo:$PWD/backend-novo" \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -w "$PWD/backend-novo" \
  maven:3.9.11-eclipse-temurin-17 \
  mvn -B -ntp -Dmaven.repo.local=/tmp/maven/repository verify
```

O caminho absoluto igual no host/container permite mounts do Testcontainers. --network host torna acessíveis as portas aleatórias dos containers de teste. Socket permite criar containers: usar apenas em ambiente local confiável. Cache .m2 e target estão ignorados. Não há wrapper Maven nesta entrega.

## Profiles e env

| Configuração | dev | prod ou sem profile |
|---|---|---|
| JDBC URL | localhost:DB_PORT/DB_NAME, default 55432/kaup_novo | SPRING_DATASOURCE_URL obrigatório, formato jdbc:postgresql://... |
| Credenciais | DB_USER (kaup_app), DB_PASSWORD obrigatório | SPRING_DATASOURCE_USERNAME/PASSWORD obrigatórios |
| Override JDBC | SPRING_DATASOURCE_* prevalece (Compose usa hostname postgres) | SPRING_DATASOURCE_* |
| Binding local | SERVER_ADDRESS=127.0.0.1, SERVER_PORT=8080 | Mesmos defaults; container injeta 0.0.0.0 |
| Cookie | HttpOnly, SameSite=Lax, Secure=false no HTTP local | Secure=true |

Outras env: DB_POOL_SIZE=5, MAX_UPLOAD_SIZE=10MB, MAX_REQUEST_SIZE=11MB. São limites de transporte, não validação de uploads implementada. Compose fixa dev para esta demo; não é configuração de produção. IA/OCR/storage ainda não possuem variáveis ativas/adapters. Sem banco válido, startup falha: não há H2 ou fallback.

## Diagnóstico e limites

Erros seguem `{code,message,requestId}`, com X-Request-Id gerado no servidor; detalhes internos/payloads não são ecoados. Health só comprova resposta HTTP após startup e não promete disponibilidade posterior do DB. Inspecione migration e testes para evidência do banco.

Rejeições do firewall Spring Security também usam esse JSON, inclusive métodos bloqueados.
HTTP sintaticamente inválido pode ser rejeitado pelo parser Tomcat antes dos filtros: nesse caso,
a resposta é um 400 genérico do servidor, sem garantia do envelope JSON/requestId. O logger
Http11Processor permanece em ERROR para não gravar URLs/headers recebidos em diagnósticos de parsing.

Sem frontend alterado: o modo padrão continua mock; modo api só pode testar health nesta fase. Vite já encaminha /api para localhost:8080, mas auth/CSRF e o fluxo real pertencem às fases seguintes. Nenhuma chamada Gemini/OCR realizada. Retenção/consentimento e HTTPS exigem decisão antes de publicação/dados pessoais.

Resultados efetivamente obtidos, incluindo limitações e falhas corrigidas: [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md).
