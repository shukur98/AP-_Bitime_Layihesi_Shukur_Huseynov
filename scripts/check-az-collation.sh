#!/usr/bin/env bash
# Offline Java 11+ check; no network, no Maven dependencies and no sandbox changes.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT
javac -encoding UTF-8 -d "$TMP_DIR" \
  "$ROOT/src/test/java/utils/AzerbaijaniCollation.java" \
  "$ROOT/scripts/java/AzerbaijaniCollationSmoke.java"
java -cp "$TMP_DIR" checks.AzerbaijaniCollationSmoke
