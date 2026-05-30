#!/usr/bin/env bash
# Self-signed certificate for local HTTPS testing.
# Usage: SSL_KEYSTORE_PASSWORD=secret ./scripts/generate-dev-certs.sh
set -euo pipefail

CERT_DIR="${1:-./certs}"
mkdir -p "$CERT_DIR"

PASSWORD="${SSL_KEYSTORE_PASSWORD:-dev-secret}"
DOMAIN="${SSL_DEV_DOMAIN:-localhost}"

openssl req -x509 -newkey rsa:2048 -nodes \
  -keyout "${CERT_DIR}/privkey.pem" \
  -out "${CERT_DIR}/fullchain.pem" \
  -days 365 \
  -subj "/CN=${DOMAIN}" \
  -addext "subjectAltName=DNS:${DOMAIN},DNS:localhost,IP:127.0.0.1"

export SSL_KEYSTORE_PASSWORD="$PASSWORD"
export SSL_PRIVATE_KEY_PASSWORD="$PASSWORD"
"$(dirname "$0")/pem-to-pkcs12.sh" "$CERT_DIR"

echo "Dev keystore ready. Start with: docker compose -f docker-compose.prod.yml up"
