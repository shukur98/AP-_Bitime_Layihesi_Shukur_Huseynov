#!/usr/bin/env bash
# Safe presence check; no tokens or passwords echoed or saved.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
if [[ -f .env ]]; then
  set -a
  source .env
  set +a
  echo "[INFO] .env loaded for this check (will not print secrets)"
else
  echo "[INFO] No .env: Java uses teacher's documented sandbox defaults on the exact teaching host"
fi
url="${QA_API_BASE_URL:-https://api.anarabbas.com}"
url="${url%/}"
printf 'API host: %s\n' "$url"
for user in 1 2; do
  t="QA_TOKEN_USER${user}"; e="QA_USER${user}_EMAIL"; p="QA_USER${user}_PASSWORD"
  token="${!t:-}"; email="${!e:-}"; password="${!p:-}"
  if [[ -n "$token" ]]; then
    echo "user${user}: using existing token (no login needed)"
  elif [[ -n "$email" && -n "$password" ]]; then
    echo "user${user}: login from explicit credentials; POST /auth/login"
  elif [[ -z "$email" && -z "$password" && "$url" == 'https://api.anarabbas.com' ]]; then
    echo "user${user}: auto-login with built-in documented teacher sandbox account"
  else
    echo "[ERROR] user${user}: missing/incomplete credentials for selected host" >&2
    exit 2
  fi
  if [[ -f ".qa-sandbox/user${user}.properties" ]]; then
    echo "user${user}: local sandbox cache file exists (will be validated on use; never print token)"
  fi
done
printf 'Ready to attempt/reuse sandbox authentication; this does NOT prove the remote server accepts credentials.\n'
