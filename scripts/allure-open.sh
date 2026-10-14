#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
if ! command -v allure >/dev/null 2>&1; then
  echo '[ERROR] Install Allure CLI (e.g. brew install allure)' >&2
  exit 3
fi
if [[ ! -d target/allure-results ]]; then echo '[ERROR] Run JUnit tests first' >&2; exit 2; fi
allure open reports/allure-report
