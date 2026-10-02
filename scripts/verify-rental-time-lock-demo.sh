#!/usr/bin/env bash
set -euo pipefail

MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:?Set MYSQL_PASSWORD in your local environment}"
ITEM_ID="${ITEM_ID:-9}"
START_TIME="${START_TIME:-2026-06-28T10:00:00}"
END_TIME="${END_TIME:-2026-06-30T10:00:00}"
EXPECTED_QUANTITY="${EXPECTED_QUANTITY:-5}"

START_SQL="${START_TIME/T/ }"
END_SQL="${END_TIME/T/ }"

occupied="$(MYSQL_PWD="$MYSQL_PASSWORD" mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" -N -B sr_rental -e "SELECT COALESCE(SUM(quantity),0) FROM rental_time_locks WHERE item_id=${ITEM_ID} AND status=0 AND rent_start_time < '${END_SQL}' AND rent_end_time > '${START_SQL}';")"
remaining=$((EXPECTED_QUANTITY - occupied))

echo "初始数量: ${EXPECTED_QUANTITY}"
echo "成功占用数量: <= ${EXPECTED_QUANTITY}"
echo "实际占用数量: ${occupied}"
echo "剩余可租数量: >= 0"
echo "实际剩余数量: ${remaining}"

if [ "$occupied" -le "$EXPECTED_QUANTITY" ]; then
  echo "时间段库存未超租: PASS"
else
  echo "时间段库存未超租: FAIL"
  exit 1
fi

if [ "$remaining" -ge 0 ]; then
  echo "库存守恒检查: PASS"
else
  echo "库存守恒检查: FAIL"
  exit 1
fi
