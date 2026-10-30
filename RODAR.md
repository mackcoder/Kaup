# Como rodar o Kaup

Backend (Spring Boot + PostgreSQL, no Docker) e frontend (React, `npm run dev`).

| Sistema | Como rodar os scripts | Testado? |
|---|---|---|
| Linux (Ubuntu) | terminal | sim |
| WSL2 (Windows) | terminal do WSL (Ubuntu) | só a detecção |
| macOS | Terminal | só a detecção e o caminho sem `setsid` |
| Windows sem WSL | **Git Bash** (vem com o Git for Windows); o recomendado é o WSL2 | não |
| GitHub Codespaces | terminal do Codespace; o link sai como `https://<codespace>-5174.app.github.dev` | só a detecção |

Os scripts detectam o sistema sozinhos (para abrir o navegador e achar a porta em uso). Se o navegador não abrir, eles avisam e você abre o link. `KAUP_NO_BROWSER=1 scripts/rodar.sh` nunca abre o navegador.

## Rodar (já instalado)

Na raiz do repositório:

```bash
scripts/rodar.sh      # liga tudo e abre http://localhost:5174 no navegador
scripts/parar.sh      # desliga tudo (os dados do banco ficam salvos)
```

Com o Claude Code, dentro do projeto: `/run_kaup` e `/stop_kaup` fazem o mesmo.

| Serviço | Endereço |
|---|---|
| App (frontend) | http://localhost:5174 |
| API (backend) | http://localhost:8080 (teste: `curl http://localhost:8080/api/analise/health`) |

Login de desenvolvimento do administrador: `ADMIN@ADMIN` / `ADMIN` (vê todos os documentos). Qualquer pessoa pode criar a própria conta na tela "Criar conta".

## Os mesmos passos, sem os scripts

Ligar:

```bash
docker compose up -d --build postgres backend     # backend + banco (1ª vez leva alguns minutos)
curl http://localhost:8080/api/analise/health     # deve responder 200
cd frontend && npm run dev                        # deixe este terminal aberto; abra http://localhost:5174
```

Desligar:

```bash
# Ctrl+C no terminal do npm run dev, e então:
docker compose stop backend postgres
```

Nunca use `docker compose down -v`: o `-v` apaga o banco.

## Instalar do zero

Faça só os passos que ainda faltam.

### 1. Baixar o código

```bash
sudo apt update && sudo apt install -y git
git clone <URL-DO-REPOSITORIO> Kaup
cd Kaup
```

### 2. Instalar o Docker

Confira antes: `docker --version` e `docker compose version`. Se faltar:

```bash
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER      # depois saia da sessão e entre de novo (ou reinicie)
docker run --rm hello-world        # deve imprimir "Hello from Docker!"
```

- macOS: instale o Docker Desktop (https://www.docker.com/products/docker-desktop) e abra-o.
- Windows: instale o WSL2 (`wsl --install` no PowerShell como administrador, reinicie) e o Docker Desktop com a integração WSL ligada; rode tudo dentro do terminal do Ubuntu (WSL).
- Codespaces: o Docker já vem instalado.

### 3. Instalar o Node.js 22+

Confira antes: `node --version` (precisa ser v22 ou maior). Se faltar:

```bash
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.1/install.sh | bash
# feche e abra o terminal, então:
nvm install 22
node --version && npm --version
```

macOS também aceita `brew install node@22`. Codespaces já traz o Node (confira a versão).

### 4. Obter a chave do Gemini

A IA que lê os contratos e conversa é o Gemini. Crie uma chave gratuita em https://aistudio.google.com/apikey e copie. Ela é secreta: não coloque em código, mensagens ou commits.

### 5. Configurar o `.env` (backend)

Na raiz do repositório:

```bash
cp .env.example .env
```

Abra o `.env` e preencha `GEMINI_API_KEY=` com a sua chave. O arquivo é ignorado pelo git. Opcionais (já têm padrão): `GEMINI_MODEL`, `KAUP_ADMIN_*`, `KAUP_CODIGO_FAKE`, `KAUP_RETENCAO_DIAS` (comentados no próprio arquivo).

### 6. Configurar e instalar o frontend

```bash
cd frontend
cp .env.example .env
```

No `frontend/.env`, deixe:

```
VITE_DATA_SOURCE=api
VITE_API_TIMEOUT_MS=60000
```

(`VITE_DATA_SOURCE=mock` abre o app com dados fictícios, sem backend.) Depois:

```bash
npm install
cd ..
```

### 7. Primeira execução

```bash
scripts/rodar.sh
```

Na primeira vez o Docker baixa imagens e compila o backend (alguns minutos). No fim, o navegador abre em http://localhost:5174. Crie uma conta e envie um contrato (PDF ou foto).

## Problemas comuns

| Sintoma | O que fazer |
|---|---|
| `permission denied` ao usar docker | Passo 2: `usermod -aG docker` e entrar de novo na sessão. |
| `The Docker daemon isn't running` | Linux: `sudo systemctl start docker`. Mac/Windows: abra o Docker Desktop. |
| Porta 8080 em uso | Outro programa/container a usa. Veja: `docker ps` (Linux: `ss -ltnp 'sport = :8080'`; Mac: `lsof -iTCP:8080 -sTCP:LISTEN`); pare o outro. |
| Porta 5174 em uso | Outro `npm run dev` aberto. Rode `scripts/parar.sh` ou feche o outro terminal. |
| Tela diz "Não foi possível falar com o servidor" | O backend está desligado: `docker compose up -d postgres backend` e espere o `curl .../health` dar 200. |
| Análise falha com erro da IA (502) | Chave do Gemini vazia, errada ou sem cota: confira o `.env` e reinicie com `docker compose up -d backend`. |
| Mudei o `.env` e nada mudou | `docker compose up -d --force-recreate backend` (o frontend precisa de reinício do `npm run dev`). |
| Ver o que o backend está dizendo | `docker compose logs --tail 100 backend`. Log do frontend iniciado pelo script: `/tmp/kaup-vite.log`. |

## Testes (opcional)

```bash
cd frontend && npm test                                  # frontend
docker run --rm -v "$PWD/backend":/app -w /app maven:3.9-eclipse-temurin-17 mvn -B test   # backend, sem instalar Java
```
