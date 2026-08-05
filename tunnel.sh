#!/bin/bash

# Tunnel Cloudflare pour accès 4G / hors Wi-Fi
# Usage: ./tunnel.sh [start|stop|status|url]
# Prérequis: serveur local sur le port 8080 (./start.sh)

set -euo pipefail

PORT=8080
ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
PID_FILE="$ROOT_DIR/.cloudflared.pid"
LOG_FILE="$ROOT_DIR/.cloudflared.log"
URL_FILE="$ROOT_DIR/.cloudflared.url"
WAIT_SECONDS=90

CLOUDFLARED_BIN="$(command -v cloudflared || true)"
if [ -z "$CLOUDFLARED_BIN" ]; then
    echo "❌ cloudflared n'est pas installé."
    echo "   Installez-le avec: brew install cloudflared"
    exit 1
fi

is_running() {
    if [ -f "$PID_FILE" ]; then
        local pid
        pid="$(cat "$PID_FILE" 2>/dev/null || true)"
        if [ -n "${pid:-}" ] && kill -0 "$pid" 2>/dev/null; then
            return 0
        fi
    fi
    # Fallback: process cloudflared encore actif
    if pgrep -f "cloudflared tunnel --url http://localhost:$PORT" >/dev/null 2>&1; then
        return 0
    fi
    return 1
}

wait_for_server() {
    local elapsed=0
    while ! lsof -Pi :"$PORT" -sTCP:LISTEN -t >/dev/null 2>&1; do
        if [ "$elapsed" -ge "$WAIT_SECONDS" ]; then
            echo "❌ Aucun serveur sur le port $PORT après ${WAIT_SECONDS}s."
            echo "   Lance d'abord: ./start.sh"
            return 1
        fi
        sleep 1
        elapsed=$((elapsed + 1))
    done
    return 0
}

extract_url_from_log() {
    if [ -f "$LOG_FILE" ]; then
        grep -Eo 'https://[a-zA-Z0-9.-]+\.trycloudflare\.com' "$LOG_FILE" | tail -n 1 || true
    fi
}

extract_url() {
    if [ -f "$URL_FILE" ]; then
        local saved
        saved="$(tr -d '[:space:]' < "$URL_FILE" 2>/dev/null || true)"
        if [ -n "${saved:-}" ]; then
            echo "$saved"
            return 0
        fi
    fi
    extract_url_from_log
}

copy_url() {
    local url="$1"
    [ -n "$url" ] || return 1
    echo "$url" > "$URL_FILE"
    if command -v pbcopy >/dev/null 2>&1; then
        printf '%s' "$url" | pbcopy
        echo "📋 URL copiée dans le presse-papiers"
    fi
}

wait_for_url() {
    local elapsed=0
    local url=""
    while [ "$elapsed" -lt 30 ]; do
        url="$(extract_url_from_log)"
        if [ -n "$url" ]; then
            echo "$url" > "$URL_FILE"
            echo "$url"
            return 0
        fi
        sleep 1
        elapsed=$((elapsed + 1))
    done
    return 1
}

start_tunnel() {
    if is_running; then
        local url
        url="$(extract_url || true)"
        echo "ℹ️  Tunnel déjà actif"
        if [ -n "${url:-}" ]; then
            echo "🌐 URL publique: $url"
            copy_url "$url" >/dev/null || true
            echo "📋 URL copiée dans le presse-papiers"
        fi
        return 0
    fi

    echo "⏳ Vérification du serveur local (port $PORT)..."
    wait_for_server || exit 1

    echo "🚀 Démarrage du tunnel Cloudflare..."
    : > "$LOG_FILE"
    rm -f "$URL_FILE"

    # Détache proprement le process pour qu'il survive à la fermeture du shell
    (
        exec "$CLOUDFLARED_BIN" tunnel --url "http://localhost:$PORT" \
            </dev/null >"$LOG_FILE" 2>&1
    ) &
    echo $! > "$PID_FILE"
    disown "$!" 2>/dev/null || true

    echo "⏳ Récupération de l'URL publique..."
    local url
    if url="$(wait_for_url)"; then
        echo ""
        echo "✅ Tunnel actif"
        echo "📱 Ouvre cette URL sur ton téléphone (4G):"
        echo "   $url"
        copy_url "$url" || true
        echo ""
        echo "💡 Afficher / recopier l'URL: ./tunnel.sh url"
        echo "🛑 Arrêter le tunnel: ./tunnel.sh stop (ou ./stop.sh)"
    else
        echo "⚠️  Tunnel lancé (PID: $(cat "$PID_FILE")) mais URL non détectée."
        echo "   Consulte les logs: tail -f $LOG_FILE"
        exit 1
    fi
}

stop_tunnel() {
    local stopped=false

    if [ -f "$PID_FILE" ]; then
        local pid
        pid="$(cat "$PID_FILE" 2>/dev/null || true)"
        if [ -n "${pid:-}" ] && kill -0 "$pid" 2>/dev/null; then
            kill "$pid" 2>/dev/null || true
            sleep 1
            if kill -0 "$pid" 2>/dev/null; then
                kill -9 "$pid" 2>/dev/null || true
            fi
            stopped=true
        fi
        rm -f "$PID_FILE"
    fi

    # Nettoyage éventuel d'anciens process cloudflared quick tunnel
    pkill -f "cloudflared tunnel --url http://localhost:$PORT" 2>/dev/null || true

    rm -f "$URL_FILE"
    if [ "$stopped" = true ]; then
        echo "✅ Tunnel arrêté"
    else
        echo "ℹ️  Aucun tunnel actif"
    fi
}

show_status() {
    if is_running; then
        local url
        url="$(extract_url || true)"
        echo "✅ Tunnel actif"
        if [ -n "${url:-}" ]; then
            echo "🌐 URL: $url"
            copy_url "$url" >/dev/null || true
            echo "📋 URL copiée dans le presse-papiers"
        else
            echo "⚠️  URL pas encore disponible — voir $LOG_FILE"
        fi
    else
        echo "ℹ️  Tunnel inactif"
        exit 1
    fi
}

show_url() {
    local url
    url="$(extract_url || true)"
    if [ -n "${url:-}" ]; then
        echo "$url"
        copy_url "$url" >/dev/null || true
        echo "📋 Copiée dans le presse-papiers" >&2
    else
        echo "❌ Pas d'URL disponible. Lance: ./tunnel.sh start" >&2
        exit 1
    fi
}

ACTION="${1:-start}"
case "$ACTION" in
    start) start_tunnel ;;
    stop) stop_tunnel ;;
    status) show_status ;;
    url) show_url ;;
    *)
        echo "Usage: ./tunnel.sh [start|stop|status|url]"
        exit 1
        ;;
esac
