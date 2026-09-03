#!/usr/bin/env bash
# Déploie Belgian Slot Club sur raspserver.local
# Usage (depuis la racine du projet) :
#   SSHPASS='motdepasse' ./deploy/deploy-to-pi.sh

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
HOST="${DEPLOY_HOST:-raspserver@raspserver.local}"
REMOTE_DIR="${DEPLOY_DIR:-/home/raspserver/belgianslotclub}"
JAR_NAME="BelgianSlotClubSpring-0.0.1-SNAPSHOT.jar"

# ssh : user@fe80::…%en0  (sans crochets)
# scp/rsync : user@[fe80::…%en0]:path
if [[ "$HOST" == *@\[*\] ]]; then
  _user="${HOST%%@*}"
  _addr="${HOST#*@}"
  _addr="${_addr#\[}"
  _addr="${_addr%\]}"
  SSH_HOST="${_user}@${_addr}"
else
  SSH_HOST="$HOST"
fi
_user="${SSH_HOST%%@*}"
_addr="${SSH_HOST#*@}"
if [[ "$_addr" == *:* ]]; then
  SCP_HOST="${_user}@[${_addr}]"
else
  SCP_HOST="$SSH_HOST"
fi
HOST="$SSH_HOST"

cd "$ROOT_DIR"

if ! command -v sshpass >/dev/null; then
  echo "sshpass requis (brew install sshpass)" >&2
  exit 1
fi

if [ -z "${SSHPASS:-}" ]; then
  echo "Définis SSHPASS avant de lancer le script." >&2
  exit 1
fi

SSH=(sshpass -e ssh -o PreferredAuthentications=password -o PubkeyAuthentication=no -o StrictHostKeyChecking=accept-new)
SCP=(sshpass -e scp -o PreferredAuthentications=password -o PubkeyAuthentication=no -o StrictHostKeyChecking=accept-new)
RSYNC=(sshpass -e rsync -e "ssh -o PreferredAuthentications=password -o PubkeyAuthentication=no -o StrictHostKeyChecking=accept-new")

echo "==> Build JAR (Mac)"
./mvnw -DskipTests -q package

if [ ! -f "target/$JAR_NAME" ]; then
  echo "JAR introuvable: target/$JAR_NAME" >&2
  exit 1
fi

echo "==> Préparation dossier distant"
"${SSH[@]}" "$HOST" "mkdir -p '$REMOTE_DIR'/{data,uploads/marketplace,uploads/forum,logs,bin}"

echo "==> Sauvegarde base Pi (avant deploy)"
"${SSH[@]}" "$HOST" "bash -s" <<'BACKUP'
set -euo pipefail
DIR=/home/raspserver/belgianslotclub/data
mkdir -p "$DIR/backups"
if [ -f "$DIR/belgianslotclub.mv.db" ]; then
  STAMP=$(date +%Y%m%d-%H%M%S)
  cp -a "$DIR/belgianslotclub.mv.db" "$DIR/backups/belgianslotclub-${STAMP}.mv.db"
  # garde les 10 plus récentes
  ls -1t "$DIR/backups"/belgianslotclub-*.mv.db 2>/dev/null | tail -n +11 | xargs -r rm -f
  echo "Backup OK: belgianslotclub-${STAMP}.mv.db"
else
  echo "Pas encore de base distante."
fi
BACKUP

echo "==> Copie JAR (+ uploads, sans écraser la base distante)"
"${SCP[@]}" "target/$JAR_NAME" "$SCP_HOST:$REMOTE_DIR/app.jar"
# Ne jamais rsync la DB H2 locale vers le Pi par défaut (écrase les rallyes en prod).
# Pour forcer : DEPLOY_SYNC_DATA=1 SSHPASS=… ./deploy/deploy-to-pi.sh
if [ "${DEPLOY_SYNC_DATA:-}" = "1" ] && [ -d data ]; then
  echo "==> ATTENTION : synchronisation data/ locale → Pi (DEPLOY_SYNC_DATA=1)"
  "${RSYNC[@]}" -az --exclude '*.lock.db' --exclude 'backups/' data/ "$SCP_HOST:$REMOTE_DIR/data/" || true
fi
if [ -d uploads ]; then
  "${RSYNC[@]}" -az uploads/ "$SCP_HOST:$REMOTE_DIR/uploads/" || true
fi
"${SCP[@]}" deploy/belgianslotclub.service "$SCP_HOST:/tmp/belgianslotclub.service"
"${SCP[@]}" deploy/nginx-belgianslotclub.conf "$SCP_HOST:/tmp/nginx-belgianslotclub.conf"
"${SCP[@]}" deploy/nginx-belgianslotclub-http-redirect.conf "$SCP_HOST:/tmp/nginx-belgianslotclub-http-redirect.conf"
"${SCP[@]}" deploy/nginx-belgianslotclub-proxy.conf "$SCP_HOST:/tmp/nginx-belgianslotclub-proxy.conf"
"${SCP[@]}" deploy/nginx-belgianslotclub-proxy-params.conf "$SCP_HOST:/tmp/nginx-belgianslotclub-proxy-params.conf"
"${SCP[@]}" deploy/nginx-belgianslotclub-limits.conf "$SCP_HOST:/tmp/nginx-belgianslotclub-limits.conf"
"${SCP[@]}" deploy/nginx-belgianslotclub-ssl.conf "$SCP_HOST:/tmp/nginx-belgianslotclub-ssl.conf"
"${SCP[@]}" deploy/h2-backup.sh "$SCP_HOST:/tmp/h2-backup.sh"
"${SCP[@]}" deploy/h2-backup.service "$SCP_HOST:/tmp/h2-backup.service"
"${SCP[@]}" deploy/h2-backup.timer "$SCP_HOST:/tmp/h2-backup.timer"

echo "==> Installation Java / Nginx + service (sudo)"
"${SSH[@]}" "$HOST" "SSHPASS_REMOTE='$SSHPASS' bash -s" <<'REMOTE'
set -euo pipefail
REMOTE_DIR=/home/raspserver/belgianslotclub
sudo_cmd() {
  if sudo -n true 2>/dev/null; then
    sudo "$@"
  else
    echo "$SSHPASS_REMOTE" | sudo -S "$@"
  fi
}

if ! command -v java >/dev/null 2>&1; then
  echo "Installation OpenJDK 25…"
  sudo_cmd apt-get update -y
  sudo_cmd apt-get install -y openjdk-25-jre-headless nginx
else
  echo "Installation / mise à jour OpenJDK 25…"
  sudo_cmd apt-get install -y openjdk-25-jre-headless
fi

if ! command -v nginx >/dev/null 2>&1; then
  sudo_cmd apt-get update -y
  sudo_cmd apt-get install -y nginx
fi

JAVA25_HOME="$(ls -d /usr/lib/jvm/java-25-openjdk-* 2>/dev/null | head -1 || true)"
if [ -z "$JAVA25_HOME" ] || [ ! -x "$JAVA25_HOME/bin/java" ]; then
  echo "OpenJDK 25 introuvable après install" >&2
  exit 1
fi
sudo_cmd update-alternatives --install /usr/bin/java java "$JAVA25_HOME/bin/java" 2500
sudo_cmd update-alternatives --set java "$JAVA25_HOME/bin/java" || true
sed -i "s|^Environment=JAVA_HOME=.*|Environment=JAVA_HOME=${JAVA25_HOME}|" /tmp/belgianslotclub.service

sudo_cmd mkdir -p /etc/nginx/snippets /etc/nginx/conf.d /etc/ssl/cloudflare
sudo_cmd cp /tmp/belgianslotclub.service /etc/systemd/system/belgianslotclub.service
sudo_cmd cp /tmp/nginx-belgianslotclub-proxy.conf /etc/nginx/snippets/belgianslotclub-proxy.conf
sudo_cmd cp /tmp/nginx-belgianslotclub-proxy-params.conf /etc/nginx/snippets/belgianslotclub-proxy-params.conf
sudo_cmd cp /tmp/nginx-belgianslotclub-limits.conf /etc/nginx/conf.d/belgianslotclub-limits.conf
sudo_cmd cp /tmp/nginx-belgianslotclub.conf /etc/nginx/sites-available/belgianslotclub
sudo_cmd ln -sfn /etc/nginx/sites-available/belgianslotclub /etc/nginx/sites-enabled/belgianslotclub
sudo_cmd rm -f /etc/nginx/sites-enabled/default

install -d -m 0755 "$REMOTE_DIR/bin" "$REMOTE_DIR/uploads/marketplace" "$REMOTE_DIR/uploads/forum"
install -m 0755 /tmp/h2-backup.sh "$REMOTE_DIR/bin/h2-backup.sh"
sudo_cmd cp /tmp/h2-backup.service /etc/systemd/system/h2-backup.service
sudo_cmd cp /tmp/h2-backup.timer /etc/systemd/system/h2-backup.timer

# Active HTTPS Origin seulement si le certificat Cloudflare est présent
if [ -f /etc/ssl/cloudflare/belgianslotclub.pem ] && [ -f /etc/ssl/cloudflare/belgianslotclub.key ]; then
  echo "Cert Origin Cloudflare détecté → HTTP redirect + SSL Full/Strict"
  sudo_cmd cp /tmp/nginx-belgianslotclub-http-redirect.conf /etc/nginx/sites-available/belgianslotclub
  sudo_cmd cp /tmp/nginx-belgianslotclub-ssl.conf /etc/nginx/sites-available/belgianslotclub-ssl
  sudo_cmd ln -sfn /etc/nginx/sites-available/belgianslotclub-ssl /etc/nginx/sites-enabled/belgianslotclub-ssl
else
  echo "Pas de cert Origin → Flexible OK (Always Use HTTPS côté Cloudflare)"
  sudo_cmd rm -f /etc/nginx/sites-enabled/belgianslotclub-ssl
fi

sudo_cmd nginx -t
sudo_cmd systemctl daemon-reload
sudo_cmd systemctl enable belgianslotclub
sudo_cmd systemctl enable --now h2-backup.timer
sudo_cmd systemctl restart belgianslotclub
sudo_cmd systemctl reload nginx

sleep 4
systemctl is-active belgianslotclub || true
curl -sf -o /dev/null -w "app:%{http_code}\n" http://127.0.0.1:8080/ || echo "app:down"
curl -sf -o /dev/null -w "nginx-lan:%{http_code}\n" -H 'Host: raspserver.local' http://127.0.0.1/ || echo "nginx-lan:down"
REMOTE

echo
echo "Déployé."
echo "  Local : http://raspserver.local / http://192.168.0.104"
echo "  Public : https://belgianslotclub.com"
echo "  Cloudflare : export CF_API_TOKEN=… && ./deploy/harden-cloudflare.sh"
echo "  Audit : ./deploy/audit-orange-block.sh belgianslotclub.com"
