#!/bin/bash

# Script de lancement pour Belgian Slot Club Spring Boot Application
# Usage: ./start.sh [dev|prod] [--background|-b] [--tunnel|-t]

echo "🏁 Belgian Slot Club - Spring Boot Application"
echo "=============================================="

# Vérifier si Java est installé
if ! command -v java &> /dev/null; then
    echo "❌ Java n'est pas installé. Veuillez installer Java d'abord."
    exit 1
fi

# Utiliser Maven wrapper si disponible, sinon Maven
if [ -f "./mvnw" ]; then
    MVN_CMD="./mvnw"
    echo "✅ Utilisation du Maven wrapper"
elif command -v mvn &> /dev/null; then
    MVN_CMD="mvn"
    echo "✅ Utilisation de Maven"
else
    echo "❌ Maven n'est pas installé et le wrapper n'est pas disponible."
    exit 1
fi

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT_DIR"

# Parser les arguments
PROFILE="dev"
BACKGROUND=false
TUNNEL=false

for arg in "$@"; do
    case "$arg" in
        --background|-b)
            BACKGROUND=true
            ;;
        --tunnel|-t)
            TUNNEL=true
            ;;
        dev|prod)
            PROFILE="$arg"
            ;;
        *)
            echo "⚠️  Argument ignoré: $arg"
            ;;
    esac
done

# Vérifier si le port 8080 est déjà utilisé
if lsof -Pi :8080 -sTCP:LISTEN -t >/dev/null 2>&1 ; then
    echo "⚠️  Le port 8080 est déjà utilisé."
    read -p "Voulez-vous arrêter le processus existant? (o/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Oo]$ ]]; then
        echo "🛑 Arrêt du processus sur le port 8080..."
        lsof -ti:8080 | xargs kill -9 2>/dev/null
        sleep 2
    else
        echo "❌ Annulation du lancement"
        exit 1
    fi
fi

echo "📦 Compilation du projet..."

# Nettoyer et compiler le projet
$MVN_CMD clean compile -q

if [ $? -eq 0 ]; then
    echo "✅ Compilation réussie"
else
    echo "❌ Erreur lors de la compilation"
    exit 1
fi

echo "🚀 Lancement en mode: $PROFILE"

# Lancer l'application Spring Boot
echo "🌐 Démarrage de l'application..."
echo "📱 L'application sera accessible sur: http://localhost:8080"

start_tunnel_when_ready() {
    if [ "$TUNNEL" != true ]; then
        return 0
    fi
    if [ ! -x "$ROOT_DIR/tunnel.sh" ]; then
        echo "⚠️  tunnel.sh introuvable ou non exécutable — tunnel ignoré"
        return 0
    fi
    echo "🌍 Tunnel 4G demandé — démarrage dès que le port 8080 est prêt..."
    (
        "$ROOT_DIR/tunnel.sh" start
    ) &
}

if [ "$BACKGROUND" = true ]; then
    echo "🔄 Lancement en arrière-plan..."
    echo "💡 Pour arrêter l'application, utilisez: ./stop.sh ou kill \$(lsof -ti:8080)"
    echo ""
    nohup $MVN_CMD spring-boot:run -Dspring-boot.run.profiles=$PROFILE > app.log 2>&1 &
    echo "✅ Application lancée en arrière-plan (PID: $!)"
    echo "📄 Logs disponibles dans: app.log"
    echo "🔍 Suivre les logs: tail -f app.log"
    start_tunnel_when_ready
    if [ "$TUNNEL" = true ]; then
        wait
    fi
else
    echo "⏹️  Pour arrêter l'application, appuyez sur Ctrl+C"
    if [ "$TUNNEL" = true ]; then
        echo "🌍 Un tunnel public sera créé dès que le serveur est prêt"
    fi
    echo ""
    start_tunnel_when_ready
    # Arrêter le tunnel si Spring s'arrête (Ctrl+C)
    trap '"$ROOT_DIR/tunnel.sh" stop >/dev/null 2>&1 || true' EXIT INT TERM
    $MVN_CMD spring-boot:run -Dspring-boot.run.profiles=$PROFILE
fi
