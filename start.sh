#!/bin/bash

# Script de lancement pour Belgian Slot Club Spring Boot Application
# Usage: ./start.sh [dev|prod]

echo "🏁 Belgian Slot Club - Spring Boot Application"
echo "=============================================="

# Vérifier si Maven est installé
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven n'est pas installé. Veuillez installer Maven d'abord."
    exit 1
fi

# Vérifier si Java est installé
if ! command -v java &> /dev/null; then
    echo "❌ Java n'est pas installé. Veuillez installer Java d'abord."
    exit 1
fi

echo "✅ Maven et Java sont installés"
echo "📦 Compilation du projet..."

# Nettoyer et compiler le projet
mvn clean compile -q

if [ $? -eq 0 ]; then
    echo "✅ Compilation réussie"
else
    echo "❌ Erreur lors de la compilation"
    exit 1
fi

# Déterminer le profil à utiliser
PROFILE=${1:-dev}
echo "🚀 Lancement en mode: $PROFILE"

# Lancer l'application Spring Boot
echo "🌐 Démarrage de l'application..."
echo "📱 L'application sera accessible sur: http://localhost:8080"
echo "⏹️  Pour arrêter l'application, appuyez sur Ctrl+C"
echo ""

mvn spring-boot:run -Dspring-boot.run.profiles=$PROFILE 