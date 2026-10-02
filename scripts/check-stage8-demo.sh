#!/usr/bin/env bash
set -euo pipefail

NACOS_ADDR="${NACOS_ADDR:-http://127.0.0.1:8848}"
GATEWAY_ADDR="${GATEWAY_ADDR:-http://127.0.0.1:8080}"

redis-cli ping | grep -q PONG
echo "Redis: PASS"

if nc -z 127.0.0.1 15672 || nc -z 127.0.0.1 5672; then
  echo "RabbitMQ: PASS"
else
  echo "RabbitMQ: FAIL"
  exit 1
fi

curl -fsS "${NACOS_ADDR}/nacos/v1/ns/operator/metrics" >/dev/null
echo "Nacos health: PASS"

nacos_status="$(scripts/nacos-config.sh status)"
grep -q "gateway.yaml" <<< "${nacos_status}"
scripts/check-nacos-config.sh >/dev/null
echo "Nacos managed configs: PASS"

curl -fsS "${GATEWAY_ADDR}/actuator/health" >/dev/null
echo "Gateway health: PASS"

echo "阶段8演示环境检查: PASS"
