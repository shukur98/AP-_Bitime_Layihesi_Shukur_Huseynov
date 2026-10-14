#!/usr/bin/env bash
# Full 117-step CallMan-equivalent RestAssured workflow.
# It ALWAYS obtains new isolated user1/user2 logins in-memory; it does NOT
# reset, replace, or reuse any locally saved .qa-sandbox session.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
if [[ "${1:-}" != "--new-sandbox" || $# -ne 1 ]]; then
  echo "USAGE: bash scripts/run-117-restassured.sh --new-sandbox" >&2
  echo "Creates a brand-new disposable sandbox and runs the 117 dependent steps ONCE." >&2
  exit 2
fi
if [[ -f .env ]]; then
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
fi
if [[ "${QA_API_BASE_URL:-https://api.anarabbas.com}" != 'https://api.anarabbas.com' ]]; then
  echo '[ERROR] Refusing 117 state-changing requests outside teacher training host.' >&2
  exit 2
fi
if [[ -n "${QA_TOKEN_USER1:-}" || -n "${QA_TOKEN_USER2:-}" ]]; then
  echo '[ERROR] Remove explicitly supplied QA_TOKEN_USER1/USER2 for isolated workflow.' >&2
  exit 2
fi
# Check the source collection and translated assertions BEFORE opening a new sandbox.
# The offline Java tests verify source-to-Java expression translation.
python3 scripts/verify-callman-mapping.py
python3 scripts/audit-callman-assertions.py
bash scripts/check-az-collation.sh
mvn -q test -Dtest=WorkflowManifestTest,WorkflowAssertionTranslationTest,AzerbaijaniCollationTest \
  -Djunit.jupiter.execution.parallel.enabled=false
# Opt-in: exclude offline preflight JUnit events from the report for this workflow.
if [[ "${QA_ALLURE_WORKFLOW_ONLY:-false}" == 'true' ]]; then
  rm -rf "${ROOT}/target/allure-results"
fi
export QA_SESSION_PERSIST=false
export QA_FRESH_SANDBOX=true
export QA_FULL_FRESH=true
export QA_AUTO_FIXTURES=false
printf '%s\n' '[INFO] Opening NEW disposable sandbox for both QA users; existing stored tokens unchanged.'
printf '%s\n' '[INFO] All 117 scenarios will be attempted sequentially. A failed step stops dependent steps.'
exec mvn test -Dtest=FullCallManWorkflowIT -Dqa.fullWorkflow=true \
  -Djunit.jupiter.execution.parallel.enabled=false
