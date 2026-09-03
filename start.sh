#!/bin/bash
# Belgian Slot Club — démarrage simple
# Usage:
#   ./start.sh              → démarre (arrière-plan) + ouvre le navigateur
#   ./start.sh --fg         → premier plan (logs dans le terminal)
#   ./start.sh --restart    → redémarre si déjà lancé
#   ./start.sh --tunnel     → + tunnel public (Cloudflare)
#   ./start.sh status       → état
#   ./start.sh stop         → arrêt (équivalent à ./stop.sh)

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT_DIR"

PORT="${PORT:-8080}"
URL="http://localhost:${PORT}"
PROFILE="dev"
BACKGROUND=true
TUNNEL=false
RESTART=false
MODE="start"

for arg in "$@"; do
    case "$arg" in
        status) MODE="status" ;;
        stop) MODE="stop" ;;
        --fg|--foreground|-f) BACKGROUND=false ;;
        --background|-b) BACKGROUND=true ;;
        --tunnel|-t) TUNNEL=true ;;
        --restart|-r) RESTART=true ;;
        --help|-h)
            cat <<'EOF'
Belgian Slot Club — démarrage simple
  ./start.sh              démarre (arrière-plan) + ouvre le navigateur
  ./start.sh --fg         premier plan (logs dans le terminal)
  ./start.sh --restart    redémarre si déjà lancé
  ./start.sh --tunnel     + tunnel public (Cloudflare)
  ./start.sh status       état
  ./start.sh stop         arrêt
EOF
            exit 0
            ;;
        dev|prod) PROFILE="$arg" ;;
        *)
            echo "Argument ignoré: $arg (voir ./start.sh --help)"
            ;;
    esac
done

# --- Java 25 (requis par le projet) ---
setup_java() {
    if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
        export PATH="$JAVA_HOME/bin:$PATH"
        return 0
    fi

    local brew_home="/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home"
    if [ ! -x "$brew_home/bin/java" ]; then
        brew_home="/usr/local/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home"
    fi
    if [ -x "$brew_home/bin/java" ]; then
        export JAVA_HOME="$brew_home"
        export PATH="$JAVA_HOME/bin:$PATH"
        return 0
    fi

    if [ -x /usr/libexec/java_home ]; then
        if JAVA_HOME_CANDIDATE="$(/usr/libexec/java_home -v 25 2>/dev/null)"; then
            export JAVA_HOME="$JAVA_HOME_CANDIDATE"
            export PATH="$JAVA_HOME/bin:$PATH"
            return 0
        fi
    fi

    echo "Java 25 introuvable. Installe-le puis réessaie :"
    echo "  brew install openjdk@25"
    exit 1
}

maven_cmd() {
    if [ -x "./mvnw" ]; then
        echo "./mvnw"
    elif command -v mvn >/dev/null 2>&1; then
        echo "mvn"
    else
        echo "Maven introuvable (./mvnw manquant)." >&2
        exit 1
    fi
}

port_pids() {
    lsof -tiTCP:"$PORT" -sTCP:LISTEN 2>/dev/null || true
}

is_up() {
    [ -n "$(port_pids)" ]
}

wait_ready() {
    local tries="${1:-60}"
    local i=0
    while [ "$i" -lt "$tries" ]; do
        if curl -sf -o /dev/null --max-time 1 "$URL/" 2>/dev/null \
            || curl -sf -o /dev/null --max-time 1 "$URL" 2>/dev/null; then
            return 0
        fi
        # Au moins le port écoute ?
        if is_up && [ "$i" -gt 15 ]; then
            # Spring peut encore booter; on considère prêt si le port répond en TCP
            if nc -z localhost "$PORT" 2>/dev/null; then
                return 0
            fi
        fi
        sleep 1
        i=$((i + 1))
        printf "."
    done
    echo
    return 1
}

open_browser() {
    if command -v open >/dev/null 2>&1; then
        open "$URL" >/dev/null 2>&1 || true
    elif command -v xdg-open >/dev/null 2>&1; then
        xdg-open "$URL" >/dev/null 2>&1 || true
    fi
}

do_stop() {
    if [ -x "./stop.sh" ]; then
        ./stop.sh
        return $?
    fi
    local pids
    pids="$(port_pids)"
    if [ -z "$pids" ]; then
        echo "Rien n'écoute sur le port $PORT."
        return 0
    fi
    echo "$pids" | xargs kill 2>/dev/null || true
    sleep 2
    pids="$(port_pids)"
    if [ -n "$pids" ]; then
        echo "$pids" | xargs kill -9 2>/dev/null || true
    fi
    echo "Arrêté."
}

do_status() {
    if is_up; then
        echo "En cours — $URL (PID: $(port_pids | tr '\n' ' '))"
        if [ -f app.log ]; then
            echo "Logs: tail -f app.log"
        fi
        exit 0
    fi
    echo "Arrêtée (port $PORT libre)."
    exit 1
}

echo "Belgian Slot Club"
echo "================="

if [ "$MODE" = "stop" ]; then
    do_stop
    exit $?
fi

if [ "$MODE" = "status" ]; then
    do_status
fi

setup_java
MVN="$(maven_cmd)"

echo "Java: $(java -version 2>&1 | head -1)"
echo "Profil: $PROFILE · Port: $PORT"

if is_up; then
    if [ "$RESTART" = true ]; then
        echo "Redémarrage…"
        do_stop
        sleep 1
    else
        echo "Déjà lancée → $URL"
        open_browser
        exit 0
    fi
fi

mkdir -p data uploads
touch app.log

start_tunnel() {
    if [ "$TUNNEL" != true ]; then
        return 0
    fi
    if [ ! -x "$ROOT_DIR/tunnel.sh" ]; then
        echo "tunnel.sh introuvable — tunnel ignoré"
        return 0
    fi
    echo "Tunnel public en attente du port $PORT…"
    ("$ROOT_DIR/tunnel.sh" start) &
}

if [ "$BACKGROUND" = true ]; then
    echo "Démarrage en arrière-plan…"
    nohup $MVN spring-boot:run \
        -Dspring-boot.run.profiles="$PROFILE" \
        >> app.log 2>&1 &
    APP_PID=$!
    echo "PID Maven: $APP_PID"
    echo -n "Attente du serveur"
    if wait_ready 90; then
        echo
        echo "Prêt → $URL"
        open_browser
        start_tunnel
        echo
        echo "Commandes utiles:"
        echo "  ./start.sh status"
        echo "  ./stop.sh"
        echo "  tail -f app.log"
        exit 0
    fi
    echo
    echo "Le serveur ne répond pas encore. Voir: tail -f app.log"
    exit 1
fi

echo "Démarrage au premier plan (Ctrl+C pour arrêter)"
echo "URL: $URL"
echo
start_tunnel
trap '"$ROOT_DIR/tunnel.sh" stop >/dev/null 2>&1 || true' EXIT INT TERM
$MVN spring-boot:run -Dspring-boot.run.profiles="$PROFILE"
