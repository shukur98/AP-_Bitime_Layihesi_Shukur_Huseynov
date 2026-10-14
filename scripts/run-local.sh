#!/usr/bin/env bash
# Maven/JUnit runner; securely loads local .env, does NOT echo credentials.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
if [[ -f .env ]]; then
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
fi
# Examples (do not paste inline comments after commands):
# bash scripts/run-local.sh -Dtest=BillerCatalogTest
# bash scripts/run-local.sh
exec mvn test "$@"
