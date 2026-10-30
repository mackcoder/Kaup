#!/usr/bin/env bash
# Liga o Kaup: PostgreSQL + backend (Docker) e o frontend (npm run dev), e abre o navegador.
# Funciona em Linux, WSL, Mac, Windows (Git Bash) e GitHub Codespaces.
# Variáveis: KAUP_NO_BROWSER=1 não abre o navegador.
set -euo pipefail
cd "$(dirname "$0")/.."
source scripts/comum.sh

PORTA=5174
URL=$(link_do_app "$PORTA")
PID=/tmp/kaup-vite.pid
LOG=/tmp/kaup-vite.log
echo "Sistema detectado: $(sistema)"

command -v docker >/dev/null || erro "Docker não está instalado (veja RODAR.md, 'Instalar do zero')."
docker info >/dev/null 2>&1 || erro "O Docker não está rodando. Linux: sudo systemctl start docker. Mac/Windows: abra o Docker Desktop."
command -v npm >/dev/null || erro "Node/npm não está instalado (veja RODAR.md, 'Instalar do zero')."
[ -f .env ] || erro "Falta o arquivo .env. Rode: cp .env.example .env e preencha GEMINI_API_KEY."
grep -Eq '^GEMINI_API_KEY=.+' .env || erro "GEMINI_API_KEY está vazia no .env."

# A porta 8080 precisa ser do nosso backend (outro projeto pode estar usando).
if porta_em_uso 8080 && ! docker ps --format '{{.Names}}' | grep -qx app_backend; then
  erro "A porta 8080 já está em uso por outro programa. Descubra com: docker ps (veja RODAR.md, 'Problemas comuns')"
fi

echo "==> Backend + PostgreSQL (Docker)"
docker compose up -d --build postgres backend
for i in $(seq 1 60); do
  [ "$(curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/api/analise/health)" = 200 ] && break
  [ "$i" = 60 ] && { docker compose logs --tail 40 backend; erro "O backend não respondeu em 2 minutos."; }
  sleep 2
done
echo "    backend ok: http://localhost:8080"

echo "==> Frontend"
cd frontend
[ -f .env ] || printf 'VITE_DATA_SOURCE=api\nVITE_API_BASE_URL=\nVITE_API_TIMEOUT_MS=60000\n' > .env
[ -d node_modules ] || npm install
if [ -f "$PID" ] && kill -0 "$(cat "$PID")" 2>/dev/null; then
  echo "    já estava rodando"
else
  if porta_em_uso "$PORTA"; then
    erro "A porta $PORTA já está em uso (outro npm run dev aberto?). Feche-o ou rode scripts/parar.sh."
  fi
  # Este script liga o backend, então o frontend roda em modo API mesmo que o .env diga "mock".
  export VITE_DATA_SOURCE=api
  # setsid (quando existe) deixa o processo sobreviver ao fechamento deste terminal.
  if command -v setsid >/dev/null; then
    setsid nohup npm run dev -- --port "$PORTA" --strictPort > "$LOG" 2>&1 &
  else
    nohup npm run dev -- --port "$PORTA" --strictPort > "$LOG" 2>&1 &
  fi
  echo $! > "$PID"
  for i in $(seq 1 30); do
    [ "$(curl -s -o /dev/null -w '%{http_code}' "http://localhost:$PORTA")" = 200 ] && break
    [ "$i" = 30 ] && { tail -20 "$LOG"; erro "O frontend não subiu (porta 5174 ocupada?). Log: $LOG"; }
    sleep 1
  done
fi
echo "    frontend ok: $URL"

if [ -z "${KAUP_NO_BROWSER:-}" ]; then
  abrir_navegador "$URL" || echo "    (não abri o navegador sozinho; abra o link manualmente)"
fi
echo
echo "Kaup no ar: $URL"
