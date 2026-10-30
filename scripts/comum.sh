# Funções usadas por rodar.sh e parar.sh (não é para rodar sozinho).

erro() { echo "ERRO: $*" >&2; exit 1; }

# Onde estamos rodando: linux | wsl | mac | windows (Git Bash) | codespaces | desconhecido
sistema() {
  [ -n "${CODESPACES:-}" ] && { echo codespaces; return; }
  case "$(uname -s)" in
    Darwin) echo mac ;;
    MINGW* | MSYS* | CYGWIN*) echo windows ;;
    Linux) if grep -qi microsoft /proc/version 2>/dev/null; then echo wsl; else echo linux; fi ;;
    *) echo desconhecido ;;
  esac
}

# Há alguém escutando nesta porta? (só bash: funciona em Linux, Mac e Git Bash, sem ss/lsof)
porta_em_uso() { (exec 3<>"/dev/tcp/127.0.0.1/$1") 2>/dev/null; }

# Link do app para quem está de fora (no Codespaces a porta é redirecionada para uma URL pública).
link_do_app() {
  if [ "$(sistema)" = codespaces ] && [ -n "${CODESPACE_NAME:-}" ]; then
    echo "https://${CODESPACE_NAME}-$1.${GITHUB_CODESPACES_PORT_FORWARDING_DOMAIN:-app.github.dev}"
  else
    echo "http://localhost:$1"
  fi
}

# Abre o navegador padrão. Devolve erro se não souber/der (o script só avisa e segue).
abrir_navegador() {
  local url=$1
  case "$(sistema)" in
    codespaces) [ -n "${BROWSER:-}" ] && "$BROWSER" "$url" >/dev/null 2>&1 ;;  # abre a porta redirecionada
    mac) open "$url" ;;
    windows) cmd.exe //c start "" "$url" >/dev/null 2>&1 ;;
    wsl)
      if command -v wslview >/dev/null; then wslview "$url"
      else powershell.exe -NoProfile -Command "Start-Process '$url'" >/dev/null 2>&1; fi ;;
    linux)
      { [ -n "${DISPLAY:-}" ] || [ -n "${WAYLAND_DISPLAY:-}" ]; } && xdg-open "$url" >/dev/null 2>&1 ;;
    *) return 1 ;;
  esac
}

# Encerra um processo e todos os filhos (o npm cria o vite, que cria outros).
matar_arvore() {
  local pai=$1 filho
  for filho in $(pgrep -P "$pai" 2>/dev/null); do matar_arvore "$filho"; done
  kill "$pai" 2>/dev/null || true
}
