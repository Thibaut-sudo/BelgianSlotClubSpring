#!/usr/bin/env bash
# Durcit Cloudflare pour belgianslotclub.com (HTTPS, HSTS, www, SSL).
#
# Prérequis :
#   export CF_API_TOKEN='...'   # Token avec Zone:Edit + DNS:Edit
#   # optionnel : export CF_ZONE_ID='...'  (sinon résolu via le domaine)
#
# Usage :
#   ./deploy/harden-cloudflare.sh
#   DOMAIN=belgianslotclub.com ./deploy/harden-cloudflare.sh

set -euo pipefail

DOMAIN="${DOMAIN:-belgianslotclub.com}"
API="https://api.cloudflare.com/client/v4"

if [[ -z "${CF_API_TOKEN:-}" ]]; then
  cat >&2 <<'EOF'
CF_API_TOKEN manquant.

1. Cloudflare → My Profile → API Tokens → Create Token
2. Template "Edit zone DNS" OU permissions :
   - Zone Settings:Edit
   - Zone:Read
   - DNS:Edit
   - Zone → include → belgianslotclub.com
3. Puis :
   export CF_API_TOKEN='ton_token'
   ./deploy/harden-cloudflare.sh
EOF
  exit 1
fi

need() { command -v "$1" >/dev/null || { echo "Manque: $1" >&2; exit 1; }; }
need curl
need python3

auth_hdr=(-H "Authorization: Bearer ${CF_API_TOKEN}" -H "Content-Type: application/json")

cf_get() {
  curl -sS "${auth_hdr[@]}" "$1"
}

cf_patch() {
  local url="$1" body="$2"
  curl -sS -X PATCH "${auth_hdr[@]}" --data "$body" "$url"
}

cf_post() {
  local url="$1" body="$2"
  curl -sS -X POST "${auth_hdr[@]}" --data "$body" "$url"
}

cf_put() {
  local url="$1" body="$2"
  curl -sS -X PUT "${auth_hdr[@]}" --data "$body" "$url"
}

json_ok() {
  python3 -c 'import json,sys; d=json.load(sys.stdin); sys.exit(0 if d.get("success") else 1)'
}

echo "==> Zone ID pour ${DOMAIN}"
if [[ -z "${CF_ZONE_ID:-}" ]]; then
  ZONES="$(cf_get "${API}/zones?name=${DOMAIN}")"
  CF_ZONE_ID="$(printf '%s' "$ZONES" | python3 -c 'import json,sys; r=json.load(sys.stdin); print(r["result"][0]["id"] if r.get("success") and r.get("result") else "")')"
fi
if [[ -z "${CF_ZONE_ID}" ]]; then
  echo "Zone introuvable pour ${DOMAIN}. Vérifie le token / le domaine." >&2
  exit 1
fi
echo "Zone: ${CF_ZONE_ID}"

patch_setting() {
  local id="$1" body="$2"
  echo "— setting ${id}"
  resp="$(cf_patch "${API}/zones/${CF_ZONE_ID}/settings/${id}" "$body")"
  if printf '%s' "$resp" | json_ok; then
    echo "  OK"
  else
    echo "  WARN: $(printf '%s' "$resp" | python3 -c 'import json,sys; d=json.load(sys.stdin); print(d.get("errors") or d.get("messages") or d)' 2>/dev/null || echo "$resp")"
  fi
}

echo "==> SSL / HTTPS / HSTS"
# flexible tant que l'origine n'a pas de cert ; passer à full/strict après Origin cert
SSL_MODE="${CF_SSL_MODE:-flexible}"
patch_setting ssl "{\"value\":\"${SSL_MODE}\"}"
patch_setting always_use_https '{"value":"on"}'
patch_setting automatic_https_rewrites '{"value":"on"}'
patch_setting opportunistic_encryption '{"value":"on"}'
patch_setting tls_1_3 '{"value":"on"}'
patch_setting min_tls_version '{"value":"1.2"}'
# HSTS OFF volontairement : sur 4G Orange Serenity, l'interception TLS + HSTS
# bloque Chrome (ERR_CERT_COMMON_NAME_INVALID) sans "continuer".
# Réactiver seulement après déblocage officiel Orange.
patch_setting security_header '{"value":{"strict_transport_security":{"enabled":false,"max_age":0,"include_subdomains":false,"nosniff":true,"preload":false}}}'
patch_setting browser_check '{"value":"on"}'
patch_setting email_obfuscation '{"value":"on"}'
patch_setting server_side_exclude '{"value":"on"}'
patch_setting security_level '{"value":"medium"}'

echo "==> DNS www (CNAME proxied → apex)"
EXISTING="$(cf_get "${API}/zones/${CF_ZONE_ID}/dns_records?type=CNAME&name=www.${DOMAIN}")"
WWW_ID="$(printf '%s' "$EXISTING" | python3 -c 'import json,sys; r=json.load(sys.stdin); print(r["result"][0]["id"] if r.get("result") else "")')"
WWW_BODY="$(python3 - <<PY
import json
print(json.dumps({
  "type": "CNAME",
  "name": "www",
  "content": "${DOMAIN}",
  "ttl": 1,
  "proxied": True,
  "comment": "www → apex (Orange/HTTPS harden)"
}))
PY
)"
if [[ -n "$WWW_ID" ]]; then
  resp="$(cf_put "${API}/zones/${CF_ZONE_ID}/dns_records/${WWW_ID}" "$WWW_BODY")"
else
  resp="$(cf_post "${API}/zones/${CF_ZONE_ID}/dns_records" "$WWW_BODY")"
fi
if printf '%s' "$resp" | json_ok; then
  echo "  OK www.${DOMAIN} → ${DOMAIN} (proxied)"
else
  echo "  WARN www: $(printf '%s' "$resp" | python3 -c 'import json,sys; print(json.load(sys.stdin).get("errors"))')"
fi

echo "==> Page Rule www → apex (301) si dispo sur le plan"
# Page Rules free = 3 ; ignore si quota
PR_BODY="$(python3 - <<PY
import json
print(json.dumps({
  "targets": [{"target": "url", "constraint": {"operator": "matches", "value": "www.${DOMAIN}/*"}}],
  "actions": [{"id": "forwarding_url", "value": {"url": "https://${DOMAIN}/\$1", "status_code": 301}}],
  "priority": 1,
  "status": "active"
}))
PY
)"
# Évite les doublons
PRS="$(cf_get "${API}/zones/${CF_ZONE_ID}/pagerules")"
HAS_WWW="$(printf '%s' "$PRS" | python3 -c 'import json,sys; r=json.load(sys.stdin); print("1" if any("www.'"${DOMAIN}"'" in str(p) for p in r.get("result",[])) else "")')"
if [[ -z "$HAS_WWW" ]]; then
  resp="$(cf_post "${API}/zones/${CF_ZONE_ID}/pagerules" "$PR_BODY")"
  if printf '%s' "$resp" | json_ok; then
    echo "  OK page rule www→apex"
  else
    echo "  INFO page rule non créée (plan/quota) — le CNAME www suffit avec Always Use HTTPS"
  fi
else
  echo "  OK page rule www déjà présente"
fi

echo
echo "==> Vérification locale"
sleep 2
curl -sI --max-time 15 "http://${DOMAIN}/" | head -8 || true
echo "---"
curl -sI --max-time 15 "https://${DOMAIN}/" | head -12 || true

cat <<EOF

Terminé côté Cloudflare.

Suite manuelle recommandée (Full strict) :
1. Cloudflare → SSL/TLS → Origin Server → Create Certificate (15 ans)
2. Sur le Pi :
   sudo mkdir -p /etc/ssl/cloudflare
   # colle le cert dans belgianslotclub.pem et la clé dans belgianslotclub.key
   sudo chmod 600 /etc/ssl/cloudflare/belgianslotclub.key
3. Relance le deploy, puis :
   export CF_SSL_MODE=strict
   ./deploy/harden-cloudflare.sh
4. Firewall Pi : n'autorise 80/443 que depuis les IP Cloudflare
   https://www.cloudflare.com/ips/

Orange : le ticket faux positif reste nécessaire (phishing@orange.be).
EOF
