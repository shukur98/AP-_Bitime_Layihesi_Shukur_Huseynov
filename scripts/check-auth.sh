#!/usr/bin/env bash
# One-off authenticated smoke test: token is never printed or persisted.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
exec bash "$ROOT/scripts/run-local.sh" -Dtest=AuthBootstrapTest
