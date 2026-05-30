#!/usr/bin/env bash
# Convert Let's Encrypt (or other) PEM files to PKCS12 for Ktor.
# Usage: SSL_KEYSTORE_PASSWORD=secret ./scripts/pem-to-pkcs12.sh [certs-dir]
# Expects: fullchain.pem (or cert.crt) and privkey.pem in certs-dir.
set -euo pipefail

CERT_DIR="${1:-./certs}"
FULLCHAIN="${CERT_DIR}/fullchain.pem"
PRIVKEY="${CERT_DIR}/privkey.pem"
OUT="${CERT_DIR}/keystore.p12"
ALIAS="${SSL_KEY_ALIAS:-codenames}"

if [[ -f "${CERT_DIR}/cert.crt" && ! -f "$FULLCHAIN" ]]; then
  FULLCHAIN="${CERT_DIR}/cert.crt"
fi

if [[ ! -f "$FULLCHAIN" || ! -f "$PRIVKEY" ]]; then
  echo "Missing PEM files in ${CERT_DIR}." >&2
  echo "Need fullchain.pem (or cert.crt) and privkey.pem." >&2
  exit 1
fi

if [[ -z "${SSL_KEYSTORE_PASSWORD:-}" ]]; then
  echo "Set SSL_KEYSTORE_PASSWORD before running." >&2
  exit 1
fi

openssl pkcs12 -export \
  -in "$FULLCHAIN" \
  -inkey "$PRIVKEY" \
  -out "$OUT" \
  -name "$ALIAS" \
  -passout "pass:${SSL_KEYSTORE_PASSWORD}"

chmod 600 "$OUT"
echo "Wrote ${OUT} (alias: ${ALIAS})"
