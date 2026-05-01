#!/bin/sh
set -eu

NACOS_SERVER_ADDR="${NACOS_SERVER_ADDR:-nacos:8848}"
NACOS_GROUP="${NACOS_GROUP:-DEFAULT_GROUP}"
CONFIG_DIR="${CONFIG_DIR:-/config}"
NACOS_INIT_RETRIES="${NACOS_INIT_RETRIES:-30}"
NACOS_INIT_DELAY_SECONDS="${NACOS_INIT_DELAY_SECONDS:-2}"

attempt=1
while [ "$attempt" -le "$NACOS_INIT_RETRIES" ]; do
  if curl -fsS "http://${NACOS_SERVER_ADDR}/nacos/v1/console/health/readiness" >/dev/null; then
    break
  fi
  echo "Waiting for Nacos ${NACOS_SERVER_ADDR} (${attempt}/${NACOS_INIT_RETRIES})"
  sleep "$NACOS_INIT_DELAY_SECONDS"
  attempt=$((attempt + 1))
done

if [ "$attempt" -gt "$NACOS_INIT_RETRIES" ]; then
  echo "Nacos ${NACOS_SERVER_ADDR} never became reachable" >&2
  exit 1
fi

for file in "${CONFIG_DIR}"/*.yaml; do
  [ -f "$file" ] || continue
  data_id="$(basename "$file")"
  echo "Publishing ${data_id} to Nacos ${NACOS_SERVER_ADDR}"
  curl -fsS -X POST "http://${NACOS_SERVER_ADDR}/nacos/v1/cs/configs" \
    --data-urlencode "dataId=${data_id}" \
    --data-urlencode "group=${NACOS_GROUP}" \
    --data-urlencode "type=yaml" \
    --data-urlencode "content@${file}"
done
