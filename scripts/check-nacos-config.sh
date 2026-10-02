#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
status_output="$("$SCRIPT_DIR/nacos-config.sh" status)"
echo "$status_output"

missing=0
while IFS= read -r line; do
  if [[ "$line" != *"FOUND managed=true"* ]]; then
    missing=1
  fi
done <<< "$status_output"

if [[ "$missing" -eq 0 ]]; then
  echo "Nacos managed config check: PASS"
else
  echo "Nacos managed config check: FAIL"
  exit 1
fi
