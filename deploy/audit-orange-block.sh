#!/usr/bin/env bash
# Audit DNS / TLS / redirects / IP reputation for Orange Serenity-style blocks.
# Usage: ./deploy/audit-orange-block.sh [domain]
# Example: ./deploy/audit-orange-block.sh belgianslotclub.com

set -euo pipefail

DOMAIN="${1:-belgianslotclub.com}"
WWW="www.${DOMAIN}"
RESOLVER="${DNS_RESOLVER:-1.1.1.1}"
OUT_DIR="${AUDIT_OUT_DIR:-./audit-out}"
mkdir -p "$OUT_DIR"

need() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Missing dependency: $1" >&2
    exit 1
  }
}

need dig
need curl
need openssl
need awk

section() { printf '\n======== %s ========\n' "$1"; }

lookup() {
  local type="$1" name="$2"
  dig +time=3 +tries=2 @"$RESOLVER" +short "$type" "$name" 2>/dev/null || true
}

section "1) DNS — A / AAAA / NS / MX / TXT / CNAME / CAA / SOA"
echo "Resolver: $RESOLVER"
for name in "$DOMAIN" "$WWW"; do
  echo "--- $name ---"
  echo "A:     $(lookup A "$name" | tr '\n' ' ')"
  echo "AAAA:  $(lookup AAAA "$name" | tr '\n' ' ')"
  echo "CNAME: $(lookup CNAME "$name" | tr '\n' ' ')"
done
echo "NS:    $(lookup NS "$DOMAIN" | tr '\n' ' ')"
echo "MX:    $(lookup MX "$DOMAIN" | tr '\n' ' ')"
echo "TXT:   $(lookup TXT "$DOMAIN" | tr '\n' ' ')"
echo "CAA:   $(lookup CAA "$DOMAIN" | tr '\n' ' ')"
echo "SOA:   $(lookup SOA "$DOMAIN" | tr '\n' ' ')"

APEX_IPS="$(lookup A "$DOMAIN")"
echo "$APEX_IPS" >"$OUT_DIR/apex-ips.txt"
if echo "$APEX_IPS" | grep -Eq '^(104\.|172\.6[4-9]\.|172\.7[0-1]\.|173\.245\.|188\.114\.|190\.93\.|197\.234\.|198\.41\.)'; then
  echo "NOTE: Public A records look like Cloudflare anycast (origin IP is likely masked)."
fi

section "2) Redirects — HTTP→HTTPS and WWW↔apex"
http_headers() {
  local url="$1"
  curl -sI --max-time 15 -A "Mozilla/5.0 (audit)" "$url" || true
}

echo "--- http://$DOMAIN/ ---"
HTTP_H="$(http_headers "http://${DOMAIN}/")"
echo "$HTTP_H" | head -20
HTTP_CODE="$(printf '%s\n' "$HTTP_H" | awk 'NR==1{print $2}')"
HTTP_LOC="$(printf '%s\n' "$HTTP_H" | awk 'tolower($1)=="location:"{print $2; exit}')"
if [[ "$HTTP_CODE" == "301" || "$HTTP_CODE" == "302" || "$HTTP_CODE" == "308" ]]; then
  if [[ "${HTTP_LOC:-}" == https://* ]]; then
    echo "OK: HTTP redirects to HTTPS ($HTTP_CODE → $HTTP_LOC)"
  else
    echo "WARN: HTTP redirects but not clearly to HTTPS ($HTTP_CODE → ${HTTP_LOC:-none})"
  fi
else
  echo "FAIL: HTTP does NOT redirect to HTTPS (status=${HTTP_CODE:-unknown}). Enable Cloudflare Always Use HTTPS."
fi

echo "--- https://$WWW/ ---"
WWW_H="$(http_headers "https://${WWW}/")"
echo "$WWW_H" | head -15
WWW_CODE="$(printf '%s\n' "$WWW_H" | awk 'NR==1{print $2}')"
if [[ -z "$WWW_CODE" ]]; then
  echo "WARN: www does not respond — create a Cloudflare DNS record for www (CNAME → $DOMAIN, proxied)."
elif [[ "$WWW_CODE" == "301" || "$WWW_CODE" == "302" || "$WWW_CODE" == "308" ]]; then
  echo "INFO: www redirects ($(printf '%s\n' "$WWW_H" | awk 'tolower($1)=="location:"{print $2; exit}'))"
else
  echo "INFO: www responds with $WWW_CODE (decide apex vs www canonical)."
fi

section "3) TLS certificate"
TMP_PEM="$OUT_DIR/cert.pem"
if echo | openssl s_client -servername "$DOMAIN" -connect "${DOMAIN}:443" -showcerts 2>/dev/null \
  | openssl x509 -outform PEM >"$TMP_PEM" 2>/dev/null; then
  openssl x509 -in "$TMP_PEM" -noout -subject -issuer -dates -ext subjectAltName 2>/dev/null || \
    openssl x509 -in "$TMP_PEM" -noout -subject -issuer -dates
  END="$(openssl x509 -in "$TMP_PEM" -noout -enddate | cut -d= -f2)"
  echo "Expires: $END"
else
  echo "FAIL: cannot fetch TLS certificate on :443"
fi

section "4) Security response headers (HTTPS)"
HTTPS_H="$(http_headers "https://${DOMAIN}/")"
echo "$HTTPS_H" | head -30
for h in strict-transport-security content-security-policy x-content-type-options x-frame-options referrer-policy permissions-policy; do
  if printf '%s\n' "$HTTPS_H" | awk -v k="$h:" 'tolower($1)==k{found=1} END{exit !found}'; then
    echo "OK: $h present"
  else
    echo "WARN: missing $h"
  fi
done
if printf '%s\n' "$HTTPS_H" | grep -qi 'server: cloudflare'; then
  echo "OK: Server header indicates Cloudflare"
fi

section "5) Mixed content scan (HTML homepage)"
HTML="$OUT_DIR/home.html"
curl -sL --max-time 20 -A "Mozilla/5.0 (audit)" "https://${DOMAIN}/" -o "$HTML" || true
if [[ -s "$HTML" ]]; then
  echo "External URLs found:"
  grep -Eo 'https?://[^"'\''[:space:]<>]+' "$HTML" | sort -u | tee "$OUT_DIR/urls.txt" | head -80
  if grep -Eoq 'http://[^"'\''[:space:]<>]+' "$HTML"; then
    echo "FAIL: plaintext http:// URLs found in HTML (possible mixed content):"
    grep -Eo 'http://[^"'\''[:space:]<>]+' "$HTML" | sort -u
  else
    echo "OK: no http:// resource URLs in homepage HTML"
  fi
else
  echo "WARN: empty homepage HTML"
fi

section "6) Origin IP / hosting clues"
while read -r ip; do
  [[ -z "$ip" ]] && continue
  echo "Public A: $ip"
  curl -s --max-time 10 "https://ipinfo.io/${ip}/json" | tee "$OUT_DIR/ipinfo-${ip}.json" || true
  echo
done <<<"$APEX_IPS"

section "7) Blacklist / reputation quick links"
IP_FIRST="$(echo "$APEX_IPS" | head -1)"
cat <<EOF
Open these in a browser (and keep screenshots for Orange / vendors):

Domain:
  https://check.spamhaus.org/results/?query=${DOMAIN}
  https://www.virustotal.com/gui/domain/${DOMAIN}
  https://transparencyreport.google.com/safe-browsing/search?url=https://${DOMAIN}
  https://www.fortiguard.com/webfilter?q=${DOMAIN}
  https://sitereview.brightcloud.com/#/lookup-result/${DOMAIN}
  https://urlhaus.abuse.ch/browse.php?search=${DOMAIN}
  https://mxtoolbox.com/SuperTool.aspx?action=blacklist%3a${DOMAIN}&run=toolpage
  https://www.urlvoid.com/scan/${DOMAIN}/
  https://www.sucuri.net/domain-scanner/?domain=${DOMAIN}

Cloudflare edge IP (often not useful for origin reputation):
  https://check.spamhaus.org/results/?query=${IP_FIRST}
  https://mxtoolbox.com/SuperTool.aspx?action=blacklist%3a${IP_FIRST}&run=toolpage

If you know your ORIGIN (Pi / VPS) public IP, check THAT IP too — do not only check Cloudflare anycast.
EOF

section "8) Summary checklist"
cat <<EOF
[ ] HTTP→HTTPS redirect enforced
[ ] Single canonical host (apex OR www), the other redirects
[ ] Valid TLS cert, not expired
[ ] No mixed content (http:// assets)
[ ] Cloudflare proxy orange-cloud ON (origin IP hidden)
[ ] SSL/TLS mode Full (strict) once origin has a valid cert
[ ] Domain/IP clean on Spamhaus, VirusTotal, Fortinet, Google Safe Browsing
[ ] Orange false-positive ticket opened (phishing@orange.be / 1200)
EOF

echo
echo "Artifacts written under: $OUT_DIR"
