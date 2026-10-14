#!/usr/bin/env bash
# Backward compatibility. Reuse the Java codepath instead of a separate Python login.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
exec bash "$ROOT/scripts/check-auth.sh"
