#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
exec "${SECURITY_PYTHON:-python3}" "$ROOT/scripts/security_check.py" "$@"
