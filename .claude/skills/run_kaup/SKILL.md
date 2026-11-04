---
name: run_kaup
description: Liga o Kaup (PostgreSQL + backend no Docker e o frontend com npm run dev), abre o navegador e informa o link.
disable-model-invocation: true
allowed-tools: Bash(scripts/rodar.sh:*), Bash(bash scripts/rodar.sh:*), Bash(curl:*), Bash(docker ps:*)
---

Ligue o Kaup rodando, na raiz do repositório:

```bash
scripts/rodar.sh
```

O script detecta o sistema (Linux, WSL, Mac, Windows/Git Bash, Codespaces), sobe o backend e o PostgreSQL (`docker compose up -d --build`), espera o backend responder, inicia o `npm run dev` do frontend em segundo plano e abre o navegador padrão. Pode levar 1 a 2 minutos se o backend precisar ser reconstruído.

Depois:
- Se terminou bem, responda só com o link (`http://localhost:5174`) e uma linha dizendo que o navegador foi aberto.
- Se falhou, mostre a linha `ERRO:` do script e o que fazer (a mensagem já diz). Detalhes de instalação e problemas comuns estão em `RODAR.md`. Não tente consertar sozinho coisas fora do projeto (parar outros programas, instalar pacotes) sem perguntar.
- Não mostre nem copie o conteúdo do `.env` (tem a chave do Gemini).
