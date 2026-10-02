#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

fail() {
  echo "JMeter demo context asset check: FAIL"
  echo "$1"
  exit 1
}

require_file() {
  local path="$1"
  [[ -f "$path" ]] || fail "missing file: $path"
}

require_executable() {
  local path="$1"
  [[ -x "$path" ]] || fail "file is not executable: $path"
}

require_grep() {
  local pattern="$1"
  local path="$2"
  grep -qE "$pattern" "$path" || fail "missing pattern '$pattern' in $path"
}

require_file scripts/prepare-jmeter-demo-context.sh
require_executable scripts/prepare-jmeter-demo-context.sh
if grep -q "mapfile" scripts/prepare-jmeter-demo-context.sh; then
  fail "scripts/prepare-jmeter-demo-context.sh must be compatible with macOS Bash 3; do not use mapfile"
fi
require_grep 'LOCK_APPLICATION_COUNT="\$\{SR_JMETER_LOCK_APPLICATION_COUNT:-10\}"' scripts/prepare-jmeter-demo-context.sh

require_file docs/tests/rental-time-lock-consistency.jmx
require_grep "CSVDataSet" docs/tests/rental-time-lock-consistency.jmx
require_grep "docs/tests/output/jmeter-application-ids.csv" docs/tests/rental-time-lock-consistency.jmx

require_file docs/tests/README-jmeter.md
require_grep "scripts/prepare-jmeter-demo-context.sh" docs/tests/README-jmeter.md
require_grep "docs/tests/output/jmeter-demo.properties" docs/tests/README-jmeter.md
require_grep "docs/tests/output/jmeter-application-ids.csv" docs/tests/README-jmeter.md
require_grep "配置管理[[:space:]]*->[[:space:]]*配置列表" docs/tests/README-jmeter.md

require_file docs/tests/README-stage11-demo.md
require_grep "scripts/prepare-jmeter-demo-context.sh" docs/tests/README-stage11-demo.md
require_grep "绿色启动" docs/tests/README-stage11-demo.md

python3 - <<'PY' || fail "item-service-down-degrade.jmx default requests must not exceed rental.application.qps=5"
import xml.etree.ElementTree as ET

root = ET.parse("docs/tests/item-service-down-degrade.jmx").getroot()
thread_group = root.find(".//ThreadGroup")
threads = int(thread_group.find("./intProp[@name='ThreadGroup.num_threads']").text)
ramp_time = int(thread_group.find("./intProp[@name='ThreadGroup.ramp_time']").text)
loops = int(thread_group.find(".//stringProp[@name='LoopController.loops']").text)
if threads * loops > 5:
    raise SystemExit(1)
if ramp_time < 3:
    raise SystemExit(1)
PY

echo "JMeter demo context asset check: PASS"
