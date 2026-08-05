#!/bin/bash
# Double-clic pour démarrer Belgian Slot Club (macOS).
# Ouvre le navigateur quand l'app est prête.

cd "$(dirname "$0")" || exit 1

echo
echo "  Belgian Slot Club — démarrage…"
echo

chmod +x ./start.sh ./stop.sh ./mvnw 2>/dev/null || true
./start.sh

echo
echo "Fenêtre fermable. Pour arrêter: double-clic sur Arrêter.command"
echo "ou dans un terminal: ./stop.sh"
echo
read -r -p "Entrée pour fermer…" _
