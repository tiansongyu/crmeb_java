#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

ADMIN_WEB="${ADMIN_WEB:-http://127.0.0.1:8080}"
ADMIN_API="${ADMIN_API:-http://127.0.0.1:20400}"
FRONT_API="${FRONT_API:-http://127.0.0.1:20410}"

python3 tests/api_smoke.py \
  --admin-web "$ADMIN_WEB" \
  --admin-api "$ADMIN_API" \
  --front-api "$FRONT_API"
