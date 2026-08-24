#!/usr/bin/env bash
# Copie quotidienne de la base H2 (tourne pendant que Java écrit : mieux que rien).
set -euo pipefail
DIR=/home/raspserver/belgianslotclub/data
mkdir -p "$DIR/backups"
if [ -f "$DIR/belgianslotclub.mv.db" ]; then
  STAMP=$(date +%Y%m%d-%H%M%S)
  cp -a "$DIR/belgianslotclub.mv.db" "$DIR/backups/belgianslotclub-${STAMP}.mv.db"
  ls -1t "$DIR/backups"/belgianslotclub-*.mv.db 2>/dev/null | tail -n +11 | xargs -r rm -f
  echo "Backup OK: belgianslotclub-${STAMP}.mv.db"
else
  echo "Pas de base H2 à sauvegarder."
fi
