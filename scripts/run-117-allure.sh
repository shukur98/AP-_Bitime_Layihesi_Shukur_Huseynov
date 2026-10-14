#!/usr/bin/env bash
# Replays 117 HTTP requests in a new disposable sandbox and generates Allure evidence.
# Syntax: bash scripts/run-117-allure.sh --new-sandbox
set -uo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
if [[ "${1:-}" != '--new-sandbox' ]] || [[ $# -ne 1 ]]; then
  echo 'USAGE: bash scripts/run-117-allure.sh --new-sandbox' >&2
  exit 2
fi
export QA_ALLURE_WORKFLOW_ONLY=true
if bash scripts/run-117-restassured.sh --new-sandbox; then test_rc=0; else test_rc=$?; fi
if bash scripts/allure-generate.sh; then report_rc=0; else report_rc=$?; fi
if [[ $test_rc -ne 0 ]]; then
  echo "[FAILED] Maven workflow exit=$test_rc. Allure results retained for diagnosis." >&2
  exit "$test_rc"
fi
if [[ $report_rc -ne 0 ]]; then
  echo "[PASS] Workflow passed, but HTML report needs Allure CLI (exit=$report_rc)." >&2
  exit "$report_rc"
fi
