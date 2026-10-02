#!/usr/bin/env bash
set -euo pipefail

NACOS_ADDR="${NACOS_ADDR:-http://127.0.0.1:8848}"
GROUP="${NACOS_GROUP:-DEFAULT_GROUP}"
MARKER="# share-rental-demo-managed: true"
ACTION="${1:-status}"
FORCE=0

if [[ "${2:-}" == "--force" ]]; then
  FORCE=1
fi

usage() {
  cat <<'EOF'
Usage:
  scripts/nacos-config.sh apply [--force]
  scripts/nacos-config.sh status
  scripts/nacos-config.sh delete [--force]

Environment:
  NACOS_ADDR=http://127.0.0.1:8848
  NACOS_GROUP=DEFAULT_GROUP
EOF
}

config_content() {
  case "$1" in
    gateway.yaml)
      cat <<'EOF'
# share-rental-demo-managed: true
sentinel:
  item-list:
    qps: 20
rental:
  application:
    qps: 5
EOF
      ;;
    item-service.yaml)
      cat <<'EOF'
# share-rental-demo-managed: true
item:
  list:
    max-page-size: 100
    default-sort: createTime
sentinel:
  item-list:
    qps: 20
resilience:
  demo:
    item-delay-ms: 0
EOF
      ;;
    rental-service.yaml)
      cat <<'EOF'
# share-rental-demo-managed: true
rental:
  application:
    qps: 5
  payment:
    timeout-minutes: 30
    demo-timeout-seconds:
    timeout-scan-ms: 60000
resilience:
  item:
    slow-call-rt-ms: 1000
    slow-ratio-threshold: 0.5
    min-request-amount: 5
    stat-interval-ms: 60000
    time-window-seconds: 10
  wallet:
    slow-call-rt-ms: 1000
    slow-ratio-threshold: 0.5
    min-request-amount: 5
    stat-interval-ms: 60000
    time-window-seconds: 10
EOF
      ;;
    wallet-service.yaml)
      cat <<'EOF'
# share-rental-demo-managed: true
wallet:
  recharge:
    max-amount: 99999
resilience:
  demo:
    wallet-delay-ms: 0
EOF
      ;;
    message-service.yaml)
      cat <<'EOF'
# share-rental-demo-managed: true
message:
  websocket:
    online-ttl-seconds: 300
EOF
      ;;
    *)
      return 1
      ;;
  esac
}

data_ids=(
  gateway.yaml
  item-service.yaml
  rental-service.yaml
  wallet-service.yaml
  message-service.yaml
)

fetch_config() {
  local data_id="$1"
  curl -fsS -G "$NACOS_ADDR/nacos/v1/cs/configs" \
    --data-urlencode "dataId=$data_id" \
    --data-urlencode "group=$GROUP" 2>/dev/null || true
}

apply_config() {
  local data_id="$1"
  local existing
  existing="$(fetch_config "$data_id")"
  if [[ -n "$existing" && "$existing" != *"$MARKER"* && "$FORCE" -ne 1 ]]; then
    echo "$data_id SKIP unmanaged existing config; use --force to overwrite"
    return 1
  fi

  local content
  content="$(config_content "$data_id")"
  curl -fsS -X POST "$NACOS_ADDR/nacos/v1/cs/configs" \
    --data-urlencode "dataId=$data_id" \
    --data-urlencode "group=$GROUP" \
    --data-urlencode "type=yaml" \
    --data-urlencode "content=$content" >/dev/null
  echo "$data_id APPLIED managed=true"
}

status_config() {
  local data_id="$1"
  local existing
  existing="$(fetch_config "$data_id")"
  if [[ -z "$existing" ]]; then
    echo "$data_id MISSING managed=false"
  elif [[ "$existing" == *"$MARKER"* ]]; then
    echo "$data_id FOUND managed=true"
  else
    echo "$data_id FOUND managed=false"
  fi
}

delete_config() {
  local data_id="$1"
  local existing
  existing="$(fetch_config "$data_id")"
  if [[ -z "$existing" ]]; then
    echo "$data_id MISSING"
    return 0
  fi
  if [[ "$existing" != *"$MARKER"* && "$FORCE" -ne 1 ]]; then
    echo "$data_id SKIP unmanaged existing config; use --force to delete"
    return 1
  fi

  curl -fsS -X DELETE "$NACOS_ADDR/nacos/v1/cs/configs" \
    --data-urlencode "dataId=$data_id" \
    --data-urlencode "group=$GROUP" >/dev/null
  echo "$data_id DELETED"
}

case "$ACTION" in
  apply)
    for data_id in "${data_ids[@]}"; do
      apply_config "$data_id"
    done
    ;;
  status)
    for data_id in "${data_ids[@]}"; do
      status_config "$data_id"
    done
    ;;
  delete)
    for data_id in "${data_ids[@]}"; do
      delete_config "$data_id"
    done
    ;;
  -h|--help|help)
    usage
    ;;
  *)
    usage
    exit 2
    ;;
esac
