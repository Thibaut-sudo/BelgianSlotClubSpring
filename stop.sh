#!/bin/bash

# Script pour arrêter l'application Belgian Slot Club Spring Boot
# Usage: ./stop.sh

echo "🛑 Arrêt de l'application Belgian Slot Club..."

# Trouver le processus sur le port 8080
PID=$(lsof -ti:8080)

if [ -z "$PID" ]; then
    echo "ℹ️  Aucune application n'est en cours d'exécution sur le port 8080"
    exit 0
fi

echo "📋 Processus trouvé (PID: $PID)"
echo "🔄 Arrêt en cours..."

# Arrêter le processus
kill $PID 2>/dev/null

# Attendre un peu
sleep 2

# Vérifier si le processus est toujours actif
if lsof -ti:8080 >/dev/null 2>&1; then
    echo "⚠️  Le processus n'a pas répondu, arrêt forcé..."
    kill -9 $PID 2>/dev/null
    sleep 1
fi

# Vérification finale
if lsof -ti:8080 >/dev/null 2>&1; then
    echo "❌ Impossible d'arrêter l'application"
    exit 1
else
    echo "✅ Application arrêtée avec succès"
    exit 0
fi


