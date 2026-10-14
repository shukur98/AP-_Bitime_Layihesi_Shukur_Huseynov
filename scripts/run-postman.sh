#!/usr/bin/env bash
# Use the original sequential CallMan/Postman collection; AUTH-01/02 obtain tokens.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
if ! command -v newman >/dev/null 2>&1; then
  echo '[ERROR] Newman CLI is not on PATH. Install separately (for example: npm install -g newman), or import postman/ files into the Postman or CallMan app.' >&2
  exit 2
fi
exec newman run postman/Final_Project_CallMan_FLAT_Run_All_117.postman_collection.json \
  -e postman/Final_Project_CallMan_Environment.postman_environment.json "$@"
