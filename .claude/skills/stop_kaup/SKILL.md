---
name: stop_kaup
description: Desliga o Kaup: para o npm run dev do frontend e o backend + PostgreSQL do Docker, mantendo os dados.
disable-model-invocation: true
allowed-tools: Bash(scripts/parar.sh:*), Bash(bash scripts/parar.sh:*), Bash(ss:*)
---

Desligue o Kaup rodando, na raiz do repositório:

```bash
scripts/parar.sh
```

O script para o `npm run dev` e faz `docker compose stop backend postgres`. Os dados do banco continuam salvos, e nada é apagado (nunca use `docker compose down -v` aqui: o `-v` apaga o banco).

Depois, responda em uma linha confirmando o que foi parado. Se o script avisar que algo ainda escuta na porta 5174, repita o aviso e pergunte antes de encerrar qualquer processo por conta própria.
