#!/usr/bin/env bash
# Non-destructive preflight — no login, HTTP request or sandbox mutation.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
python3 scripts/verify-callman-mapping.py
python3 scripts/audit-callman-assertions.py
python3 -m py_compile scripts/audit-callman-assertions.py scripts/verify-callman-mapping.py scripts/export-surefire-results.py

# Fail before HTTP if the Java alphabet comparator is incompatible with AZ primary ordering.
bash scripts/check-az-collation.sh
printf '%s\n' '[OFFLINE PASS] Mapping, audit, Python, Azerbaijani collation checked. No API calls were made.'
