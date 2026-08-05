#!/bin/bash
# Double-clic pour arrêter Belgian Slot Club (macOS).

cd "$(dirname "$0")" || exit 1

echo
echo "  Belgian Slot Club — arrêt…"
echo

chmod +x ./stop.sh ./start.sh 2>/dev/null || true
./stop.sh

echo
read -r -p "Entrée pour fermer…" _
