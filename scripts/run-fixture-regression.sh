#!/usr/bin/env bash
# Explicit, state-mutating regression using a NEW disposable teacher sandbox.
# Do not run in a valuable manual testing session: creates card(s) and beneficiaries.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
if [[ "${1:-}" != "--new-sandbox" ]]; then
  echo "USAGE: bash scripts/run-fixture-regression.sh --new-sandbox" >&2
  echo "This removes local session tokens, starts a NEW empty sandbox, and creates test data." >&2
  exit 2
fi
if [[ -f .env ]]; then
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
fi
if [[ "${QA_API_BASE_URL:-https://api.anarabbas.com}" != "https://api.anarabbas.com" ]]; then
  echo "Refusing to auto-seed outside the documented teacher API." >&2
  exit 2
fi
if [[ -n "${QA_TOKEN_USER1:-}" || -n "${QA_TOKEN_USER2:-}" ]]; then
  echo "Explicit tokens bypass session reset; unset QA_TOKEN_USER1 and QA_TOKEN_USER2 first." >&2
  exit 2
fi
# Do not let stale ids from a manual sandbox override V8's own fresh fixtures.
unset QA_TEST_IBAN QA_ACTIVE_CARD_ID
bash scripts/reset-sandbox.sh
export QA_AUTO_FIXTURES=true
mvn test -Dqa.mutationTests=true
