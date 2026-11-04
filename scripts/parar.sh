#!/usr/bin/env bash
# Desliga o Kaup: para o npm run dev e o backend + PostgreSQL. Os dados do banco são mantidos.
set -uo pipefail
cd "$(dirname "$0")/.."
source scripts/comum.sh

PID=/tmp/kaup-vite.pid
echo "==> Frontend"
if [ -f "$PID" ] && kill -0 "$(cat "$PID")" 2>/dev/null; then
  kill -- -"$(cat "$PID")" 2>/dev/null   # grupo do setsid (npm e vite juntos), se existir
  matar_arvore "$(cat "$PID")"           # garante nos sistemas sem setsid (Mac)
  echo "    parado"
else
  echo "    não estava rodando"
fi
rm -f "$PID"
for i in 1 2 3 4 5 6; do porta_em_uso 5174 || break; sleep 0.5; done   # a porta demora a soltar
if porta_em_uso 5174 && [ "$(sistema)" = windows ]; then   # Git Bash: o node é um processo do Windows
  winpid=$(netstat -ano | tr -d '\r' | awk '$2 ~ /:5174$/ && $4 == "LISTENING" {print $5; exit}')
  [ -n "$winpid" ] && taskkill //F //T //PID "$winpid" >/dev/null 2>&1
  sleep 1
fi
porta_em_uso 5174 && echo "    AVISO: algo ainda escuta na porta 5174 (iniciado fora do rodar.sh?). Feche o terminal do npm run dev."

echo "==> Backend + PostgreSQL (Docker)"
docker compose stop backend postgres
echo
echo "Kaup desligado. Os dados continuam salvos (para ligar de novo: scripts/rodar.sh)."
