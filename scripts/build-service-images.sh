#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MODULES=(bobbuy-core bobbuy-ai bobbuy-im bobbuy-auth bobbuy-gateway)
SERVICES=(core-service ai-service im-service auth-service gateway-service)
MODULE_LIST="$(IFS=,; echo "${MODULES[*]}")"

run_package=true
if [[ "${1:-}" == "--skip-package" ]]; then
  run_package=false
elif [[ $# -gt 0 ]]; then
  echo "Usage: $0 [--skip-package]" >&2
  exit 64
fi

if [[ "$run_package" == true ]]; then
  mvn -f "${ROOT_DIR}/pom.xml" -DskipTests package -pl "${MODULE_LIST}" -am
fi

missing_modules=()
for module in "${MODULES[@]}"; do
  if ! compgen -G "${ROOT_DIR}/${module}/target/${module}-*.jar" > /dev/null; then
    missing_modules+=("${module}")
  fi
done

if (( ${#missing_modules[@]} > 0 )); then
  echo "Missing prebuilt service jar(s): ${missing_modules[*]}" >&2
  echo "Build them first with:" >&2
  echo "  mvn -f ${ROOT_DIR}/pom.xml -DskipTests package -pl ${MODULE_LIST} -am" >&2
  echo "Then rerun this script or call it without --skip-package." >&2
  exit 1
fi

cd "${ROOT_DIR}"
docker compose build "${SERVICES[@]}"
