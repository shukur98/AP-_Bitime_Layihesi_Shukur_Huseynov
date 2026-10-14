#!/usr/bin/env bash
# Generate the real Allure HTML report from actual JUnit adapter results.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
RESULTS="${ROOT}/target/allure-results"
OUT="${ROOT}/reports/allure-report"
if [[ ! -d "$RESULTS" ]] || ! find "$RESULTS" -name '*-result.json' -print -quit | grep -q .; then
  echo '[ERROR] No Allure test results were found. Run a JUnit suite first.' >&2
  exit 2
fi
if ! command -v allure >/dev/null 2>&1; then
  echo "[INFO] Genuine Allure results saved in ${RESULTS}"
  echo '[ACTION] Install Allure Report CLI (e.g. brew install allure), then run bash scripts/allure-generate.sh'
  exit 3
fi
cat > "${RESULTS}/environment.properties" <<'EOF'
Project=API Bitirme Layihesi
Environment=Teacher Sandbox
Framework=JUnit 5 - REST Assured
ReportSource=Actual local JUnit execution
EOF
allure generate "$RESULTS" --clean -o "$OUT"
echo "[ALLURE HTML] ${OUT}/index.html"
