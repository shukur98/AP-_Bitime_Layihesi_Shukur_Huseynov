#!/usr/bin/env bash
# Offline project verification: request mapping, assertion inventory and AZ collation.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
exec bash "$ROOT/scripts/verify-offline.sh" "$@"
