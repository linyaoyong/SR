#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

BASE_URL="${SR_JMETER_BASE_URL:-http://127.0.0.1:8080}"
HOST="${SR_JMETER_HOST:-127.0.0.1}"
PORT="${SR_JMETER_PORT:-8080}"
ADMIN_USERNAME="${SR_JMETER_ADMIN_USERNAME:-admin}"
ADMIN_PASSWORD="${SR_JMETER_ADMIN_PASSWORD:-123456}"
OUTPUT_DIR="docs/tests/output"
PROPERTIES_FILE="$OUTPUT_DIR/jmeter-demo.properties"
APPLICATION_CSV="$OUTPUT_DIR/jmeter-application-ids.csv"
LOCK_APPLICATION_COUNT="${SR_JMETER_LOCK_APPLICATION_COUNT:-10}"
RUN_ID="${SR_JMETER_RUN_ID:-$(date +%m%d%H%M%S)}"
OWNER_USERNAME="${SR_JMETER_OWNER_USERNAME:-jmo${RUN_ID}}"
USER_B_USERNAME="${SR_JMETER_USER_B_USERNAME:-jmb${RUN_ID}}"
OWNER_PASSWORD="${SR_JMETER_OWNER_PASSWORD:-Passw0rd!A}"
USER_B_PASSWORD="${SR_JMETER_USER_B_PASSWORD:-Passw0rd!B}"
PAY_AMOUNT="${SR_JMETER_PAY_AMOUNT:-60.00}"

TMP_DIR="$(mktemp -d "${TMPDIR:-/tmp}/sr-jmeter-demo.XXXXXX")"
trap 'rm -rf "$TMP_DIR"' EXIT

usage() {
  cat <<EOF
Usage:
  scripts/prepare-jmeter-demo-context.sh

Environment:
  SR_JMETER_BASE_URL=$BASE_URL
  SR_JMETER_ADMIN_USERNAME=$ADMIN_USERNAME
  SR_JMETER_ADMIN_PASSWORD=<admin password>
  SR_JMETER_LOCK_APPLICATION_COUNT=$LOCK_APPLICATION_COUNT

Outputs:
  $PROPERTIES_FILE
  $APPLICATION_CSV
EOF
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

require_command() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "missing required command: $1" >&2
    exit 1
  }
}

require_command curl
require_command python3

json_body() {
  python3 - "$@" <<'PY'
import json
import sys

if len(sys.argv) % 2 == 0:
    raise SystemExit("json_body expects key/value pairs")

out = {}
for index in range(1, len(sys.argv), 2):
    key = sys.argv[index]
    raw = sys.argv[index + 1]
    if raw == "null":
        value = None
    elif raw in ("true", "false"):
        value = raw == "true"
    else:
        try:
            if "." in raw:
                value = float(raw)
            else:
                value = int(raw)
        except ValueError:
            value = raw
    out[key] = value

print(json.dumps(out, ensure_ascii=False))
PY
}

json_get() {
  local file="$1"
  local path="$2"
  python3 - "$file" "$path" <<'PY'
import json
import sys

with open(sys.argv[1], "r", encoding="utf-8") as fh:
    value = json.load(fh)

for part in sys.argv[2].split("."):
    if isinstance(value, list):
        value = value[int(part)]
    else:
        value = value.get(part)
    if value is None:
        print("")
        raise SystemExit(0)

print(value)
PY
}

json_first_order_id() {
  local file="$1"
  local application_id="$2"
  python3 - "$file" "$application_id" <<'PY'
import json
import sys

with open(sys.argv[1], "r", encoding="utf-8") as fh:
    body = json.load(fh)

orders = body.get("data") or []
target = str(sys.argv[2])
for order in orders:
    if str(order.get("applicationId")) == target:
        print(order.get("id") or "")
        raise SystemExit(0)

print("")
PY
}

check_api_success() {
  local file="$1"
  local label="$2"
  python3 - "$file" "$label" <<'PY'
import json
import sys

with open(sys.argv[1], "r", encoding="utf-8") as fh:
    body = json.load(fh)

code = body.get("code")
if code in (0, "0", None):
    raise SystemExit(0)

print(f"{sys.argv[2]} failed: code={code} message={body.get('message')}", file=sys.stderr)
raise SystemExit(1)
PY
}

api() {
  local method="$1"
  local path="$2"
  local token="$3"
  local body="${4:-}"
  local output="$5"
  local status
  local args=(-sS -o "$output" -w "%{http_code}" -X "$method" "$BASE_URL$path" -H "Content-Type: application/json")

  if [[ -n "$token" ]]; then
    args+=(-H "Authorization: Bearer $token")
  fi
  if [[ -n "$body" ]]; then
    args+=(-d "$body")
  fi

  status="$(curl "${args[@]}")"
  if [[ "$status" -lt 200 || "$status" -ge 300 ]]; then
    echo "$method $path failed with HTTP $status" >&2
    cat "$output" >&2 || true
    exit 1
  fi
}

step() {
  printf '\n==> %s\n' "$1"
}

register_user() {
  local username="$1"
  local password="$2"
  local response="$TMP_DIR/register-${username}.json"
  local body
  body="$(json_body username "$username" password "$password")"
  api POST /api/auth/register "" "$body" "$response"
  check_api_success "$response" "register $username"
}

login_user() {
  local username="$1"
  local password="$2"
  local response="$TMP_DIR/login-${username}.json"
  local body
  body="$(json_body username "$username" password "$password")"
  api POST /api/auth/login "" "$body" "$response"
  check_api_success "$response" "login $username"
  json_get "$response" data.accessToken
}

recharge_user() {
  local token="$1"
  local amount="$2"
  local remark="$3"
  local response="$TMP_DIR/recharge-$(date +%s%N).json"
  local body
  body="$(json_body amount "$amount" remark "$remark")"
  api POST /api/wallet/recharge "$token" "$body" "$response"
  check_api_success "$response" "wallet recharge"
}

create_item() {
  local token="$1"
  local title="$2"
  local quantity="$3"
  local daily_price="$4"
  local deposit_amount="$5"
  local response="$TMP_DIR/item-$(date +%s%N).json"
  local body
  body="$(json_body \
    title "$title" \
    description "SR JMeter demo item $RUN_ID" \
    categoryId 1 \
    tags "jmeter,demo,sr" \
    dailyPrice "$daily_price" \
    depositAmount "$deposit_amount" \
    quantity "$quantity" \
    supportDelivery 1 \
    deliveryCity "Guangzhou" \
    supportMeetup 1 \
    meetupLocation "Campus gate" \
    priceType 1 \
    minRentDays 1 \
    freeRent 0 \
    depositEnabled 1 \
    creditDepositEnabled 1 \
    minCreditScore 600 \
    freeDepositScore 750 \
    reducedDepositScore 700 \
    reducedDepositAmount 100)"
  api POST /api/items "$token" "$body" "$response"
  check_api_success "$response" "create item"
  json_get "$response" data.id
}

approve_item() {
  local token="$1"
  local item_id="$2"
  local response="$TMP_DIR/approve-item-${item_id}.json"
  local body
  body="$(json_body auditStatus 1 auditReason "JMeter demo approval")"
  api POST "/api/admin/items/${item_id}/audit" "$token" "$body" "$response"
  check_api_success "$response" "approve item $item_id"
}

create_application() {
  local token="$1"
  local item_id="$2"
  local start_time="$3"
  local end_time="$4"
  local rent_amount="$5"
  local deposit_amount="$6"
  local remark="$7"
  local response="$TMP_DIR/application-$(date +%s%N).json"
  local body
  body="$(json_body \
    itemId "$item_id" \
    quantity 1 \
    rentStartTime "$start_time" \
    rentEndTime "$end_time" \
    deliveryType 1 \
    receiverName "JMeter renter" \
    receiverPhone "13800000000" \
    receiverAddress "Campus demo address" \
    rentAmount "$rent_amount" \
    depositAmount "$deposit_amount" \
    remark "$remark")"
  api POST /api/rentals/applications "$token" "$body" "$response"
  check_api_success "$response" "create rental application"
  json_get "$response" data.id
}

confirm_application() {
  local token="$1"
  local application_id="$2"
  local response="$TMP_DIR/confirm-${application_id}.json"
  api PUT "/api/rentals/applications/${application_id}/confirm" "$token" "" "$response"
  check_api_success "$response" "confirm application $application_id"
}

find_order_id() {
  local token="$1"
  local application_id="$2"
  local response="$TMP_DIR/orders-${application_id}.json"
  api GET /api/orders "$token" "" "$response"
  check_api_success "$response" "list orders"
  json_first_order_id "$response" "$application_id"
}

future_times() {
  python3 <<'PY'
from datetime import datetime, timedelta
base = datetime.now().replace(hour=10, minute=0, second=0, microsecond=0) + timedelta(days=1)
print(base.strftime("%Y-%m-%d %H:%M:%S"))
print((base + timedelta(days=2)).strftime("%Y-%m-%d %H:%M:%S"))
print((base + timedelta(days=7)).strftime("%Y-%m-%d %H:%M:%S"))
print((base + timedelta(days=9)).strftime("%Y-%m-%d %H:%M:%S"))
PY
}

mkdir -p "$OUTPUT_DIR"

step "Checking Gateway health at $BASE_URL"
curl -fsS "$BASE_URL/actuator/health" >/dev/null

TIMES_FILE="$TMP_DIR/future-times.txt"
future_times > "$TIMES_FILE"
LOCK_RENT_START_TIME="${SR_JMETER_RENT_START_TIME:-$(sed -n '1p' "$TIMES_FILE")}"
LOCK_RENT_END_TIME="${SR_JMETER_RENT_END_TIME:-$(sed -n '2p' "$TIMES_FILE")}"
ORDER_RENT_START_TIME="${SR_JMETER_ORDER_RENT_START_TIME:-$(sed -n '3p' "$TIMES_FILE")}"
ORDER_RENT_END_TIME="${SR_JMETER_ORDER_RENT_END_TIME:-$(sed -n '4p' "$TIMES_FILE")}"

step "Creating demo users"
register_user "$OWNER_USERNAME" "$OWNER_PASSWORD"
register_user "$USER_B_USERNAME" "$USER_B_PASSWORD"
USER_A_TOKEN="$(login_user "$OWNER_USERNAME" "$OWNER_PASSWORD")"
USER_B_TOKEN="$(login_user "$USER_B_USERNAME" "$USER_B_PASSWORD")"
ADMIN_TOKEN="$(login_user "$ADMIN_USERNAME" "$ADMIN_PASSWORD")"

step "Preparing wallets"
recharge_user "$USER_A_TOKEN" 1000 "JMeter owner setup recharge"
recharge_user "$USER_B_TOKEN" 20000 "JMeter renter setup recharge"

step "Creating and approving demo items"
APPLICATION_ITEM_ID="$(create_item "$USER_A_TOKEN" "SR JMeter application item $RUN_ID" 5 20 200)"
ORDER_ITEM_ID="$(create_item "$USER_A_TOKEN" "SR JMeter wallet item $RUN_ID" 1 20 0)"
approve_item "$ADMIN_TOKEN" "$APPLICATION_ITEM_ID"
approve_item "$ADMIN_TOKEN" "$ORDER_ITEM_ID"

step "Creating pending payment order for wallet slow-call JMeter script"
ORDER_APPLICATION_ID="$(create_application "$USER_B_TOKEN" "$ORDER_ITEM_ID" "$ORDER_RENT_START_TIME" "$ORDER_RENT_END_TIME" 9999 0 "JMeter wallet slow-call order")"
confirm_application "$USER_A_TOKEN" "$ORDER_APPLICATION_ID"
ORDER_ID="$(find_order_id "$USER_B_TOKEN" "$ORDER_APPLICATION_ID")"
if [[ -z "$ORDER_ID" ]]; then
  echo "could not find order for application $ORDER_APPLICATION_ID" >&2
  exit 1
fi

step "Creating $LOCK_APPLICATION_COUNT time-lock applications for CSV-driven confirmation"
printf 'applicationId\n' > "$APPLICATION_CSV"
FIRST_LOCK_APPLICATION_ID=""
for index in $(seq 1 "$LOCK_APPLICATION_COUNT"); do
  renter_username="jml${RUN_ID}${index}"
  renter_password="Passw0rd!L"
  register_user "$renter_username" "$renter_password"
  renter_token="$(login_user "$renter_username" "$renter_password")"
  recharge_user "$renter_token" 1000 "JMeter time-lock renter setup recharge"
  application_id="$(create_application "$renter_token" "$APPLICATION_ITEM_ID" "$LOCK_RENT_START_TIME" "$LOCK_RENT_END_TIME" 40 200 "JMeter time-lock application $index")"
  if [[ -z "$FIRST_LOCK_APPLICATION_ID" ]]; then
    FIRST_LOCK_APPLICATION_ID="$application_id"
  fi
  printf '%s\n' "$application_id" >> "$APPLICATION_CSV"
done

step "Writing JMeter property file"
cat > "$PROPERTIES_FILE" <<EOF
host=$HOST
port=$PORT
userAToken=$USER_A_TOKEN
userBToken=$USER_B_TOKEN
itemId=$APPLICATION_ITEM_ID
applicationId=$FIRST_LOCK_APPLICATION_ID
orderId=$ORDER_ID
rentStartTime=$LOCK_RENT_START_TIME
rentEndTime=$LOCK_RENT_END_TIME
payAmount=$PAY_AMOUNT
applicationCsv=$APPLICATION_CSV
EOF

cat <<EOF

JMeter demo context prepared.

Properties:
  $PROPERTIES_FILE

CSV:
  $APPLICATION_CSV

Open a GUI script from the SR root, then click the green Start button:
  jmeter -q $PROPERTIES_FILE -t docs/tests/item-list-stress.jmx
  jmeter -q $PROPERTIES_FILE -t docs/tests/application-submit-limit.jmx
  jmeter -q $PROPERTIES_FILE -t docs/tests/rental-time-lock-consistency.jmx
  jmeter -q $PROPERTIES_FILE -t docs/tests/wallet-slow-call-circuit-breaker.jmx
  jmeter -q $PROPERTIES_FILE -t docs/tests/item-service-down-degrade.jmx
EOF
