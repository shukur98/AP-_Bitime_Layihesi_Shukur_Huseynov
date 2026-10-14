#!/usr/bin/env bash
# Standard JUnit suite with Allure. Intended to run independently of 117-step workflow.
set -uo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
rm -rf "${ROOT}/target/allure-results"
if bash scripts/run-local.sh "$@"; then test_rc=0; else test_rc=$?; fi
if bash scripts/allure-generate.sh; then report_rc=0; else report_rc=$?; fi
if [[ $test_rc -ne 0 ]]; then exit "$test_rc"; fi
if [[ $report_rc -ne 0 ]]; then exit "$report_rc"; fi
