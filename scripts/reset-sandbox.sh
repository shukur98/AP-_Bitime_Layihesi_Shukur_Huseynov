#!/usr/bin/env bash
# Intentionally discard locally cached token(s). NEXT test run creates a new, EMPTY sandbox.
# Does not revoke prior backend tokens or erase server data immediately.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
if [[ -d .qa-sandbox ]]; then
  rm -f .qa-sandbox/user1.properties .qa-sandbox/user2.properties .qa-sandbox/fixture-card-user1.properties
fi
echo "Local sandbox tokens cleared. Next authenticated run will open a fresh EMPTY sandbox."
