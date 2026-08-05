#!/bin/bash

# Arrêt Belgian Slot Club (+ tunnel éventuel)
# Usage: ./stop.sh

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT_DIR"

PORT="${PORT:-8080}"

echo "Arrêt Belgian Slot Club…"

if [ -x "./tunnel.sh" ]; then
    ./tunnel.sh stop >/dev/null 2>&1 || true
fi

PIDS="$(lsof -tiTCP:"$PORT" -sTCP:LISTEN 2>/dev/null || true)"

if [ -z "$PIDS" ]; then
    # Tuer aussi d'éventuels mvn spring-boot:run orphelins
    pkill -f "spring-boot:run" 2>/dev/null || true
    echo "Rien n'écoute sur le port $PORT."
    exit 0
fi

echo "PID: $PIDS"
kill $PIDS 2>/dev/null || true
sleep 2

if lsof -tiTCP:"$PORT" -sTCP:LISTEN >/dev/null 2>&1; then
    echo "Arrêt forcé…"
    kill -9 $PIDS 2>/dev/null || true
    sleep 1
fi

pkill -f "spring-boot:run" 2>/dev/null || true

if lsof -tiTCP:"$PORT" -sTCP:LISTEN >/dev/null 2>&1; then
    echo "Impossible d'arrêter l'application."
    exit 1
fi

echo "Application arrêtée."
exit 0
