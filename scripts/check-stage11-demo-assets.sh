#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

required_files=(
  "docs/tests/Share Rental API - 双用户完整流程.postman_collection.json"
  "docs/tests/Share Rental API - environment.local.postman_environment.json"
  "docs/tests/README-postman.md"
  "docs/tests/README-jmeter.md"
  "docs/tests/README-stage11-demo.md"
  "docs/tests/item-list-stress.jmx"
  "docs/tests/application-submit-limit.jmx"
  "docs/tests/rental-time-lock-consistency.jmx"
  "docs/tests/wallet-slow-call-circuit-breaker.jmx"
  "docs/tests/item-service-down-degrade.jmx"
)

for file in "${required_files[@]}"; do
  if [[ ! -f "$file" ]]; then
    echo "MISSING: $file"
    exit 1
  fi
done

node -e "JSON.parse(require('fs').readFileSync('docs/tests/Share Rental API - 双用户完整流程.postman_collection.json','utf8'))"
node -e "JSON.parse(require('fs').readFileSync('docs/tests/Share Rental API - environment.local.postman_environment.json','utf8'))"

python3 - <<'PY'
from pathlib import Path
import xml.etree.ElementTree as ET

for path in sorted(Path("docs/tests").glob("*.jmx")):
    ET.parse(path)
print("JMX XML: PASS")
PY

grep -q "Postman" docs/tests/README-postman.md
grep -q "JMeter" docs/tests/README-jmeter.md
grep -q "Nacos" docs/tests/README-stage11-demo.md
grep -q "Sentinel" docs/tests/README-stage11-demo.md
grep -q "RabbitMQ" docs/tests/README-stage11-demo.md
grep -q "Redis" docs/tests/README-stage11-demo.md

echo "阶段11测试演示资产检查: PASS"
