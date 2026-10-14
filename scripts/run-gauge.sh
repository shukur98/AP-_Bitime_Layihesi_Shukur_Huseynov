#!/usr/bin/env bash
# Gauge + Maven test-scope dependencies. Works even when macOS project folder has spaces.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
if [[ -f .env ]]; then
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
fi
if ! command -v mvn >/dev/null 2>&1; then echo '[ERROR] Maven not installed/on PATH' >&2; exit 2; fi
if ! command -v gauge >/dev/null 2>&1; then echo '[ERROR] Gauge CLI not installed/on PATH' >&2; exit 2; fi
mvn -q -DskipTests test-compile dependency:copy-dependencies \
    -DincludeScope=test -DoutputDirectory="${ROOT}/libs"
if ! compgen -G 'libs/*.jar' >/dev/null; then
  echo '[ERROR] Gauge dependencies unavailable in libs/' >&2
  exit 2
fi
export QA_PROJECT_DIR="$ROOT"
ARGS=("$@")
if [[ $# -eq 0 ]]; then ARGS=(specs/api-testing); fi
# Some Gauge Java plugin versions cannot launch from a project directory containing
# whitespace (including an accidental trailing space in Finder). Stage a minimal
# private copy at an ASCII-only path, then transfer reports back to the original.
if [[ "$ROOT" == *[[:space:]]* ]]; then
  TMP_ROOT="$(mktemp -d "/tmp/qa-gauge-XXXXXXXX")"
  WORK="$TMP_ROOT/project"
  mkdir -p "$WORK/env" "$WORK/target" "$WORK/reports"
  chmod 700 "$TMP_ROOT"
  cp "$ROOT/manifest.json" "$ROOT/pom.xml" "$WORK/"
  cp -R "$ROOT/specs" "$ROOT/src" "$ROOT/libs" "$WORK/"
  cp -R "$ROOT/env/default" "$WORK/env/"
  cp -R "$ROOT/target/test-classes" "$WORK/target/"
  copy_back() {
    rc=$?
    if [[ -d "$WORK/reports" ]]; then
      mkdir -p "$ROOT/reports"
      cp -R "$WORK/reports/." "$ROOT/reports/" || true
    fi
    rm -rf "$TMP_ROOT"
    exit "$rc"
  }
  trap copy_back EXIT
  echo '[INFO] Gauge path workaround: starting private staged project without spaces.'
  cd "$WORK"
  gauge run "${ARGS[@]}"
else
  exec gauge run "${ARGS[@]}"
fi
