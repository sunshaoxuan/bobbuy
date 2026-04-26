#!/bin/sh
NACOS_SERVER_ADDR="${NACOS_SERVER_ADDR:-nacos:8848}"
NACOS_GROUP="${NACOS_GROUP:-DEFAULT_GROUP}"
CONFIG_DIR="${CONFIG_DIR:-/config}"

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
