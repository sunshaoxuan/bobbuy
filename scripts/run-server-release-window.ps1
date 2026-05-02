param(
  [string]$SshTarget = $env:SSH_TARGET,
  [string]$AppDir = $env:APP_DIR,
  [string]$Branch = $(if ($env:BRANCH) { $env:BRANCH } else { "main" }),
  [string]$AgentAuthToken = $env:BOBBUY_AGENT_AUTH_TOKEN,
  [switch]$PrecheckOnly
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Write-Section {
  param([string]$Title)
  Write-Host ""
  Write-Host "== $Title =="
}

function Invoke-RemoteScript {
  param(
    [Parameter(Mandatory = $true)][string]$Script,
    [Parameter(Mandatory = $true)][string[]]$Arguments
  )

  $Script | & ssh $SshTarget "bash" "-s" "--" @Arguments
  if ($LASTEXITCODE -ne 0) {
    throw "Remote step failed with exit code $LASTEXITCODE"
  }
}

Write-Section "Server release window inputs"
Write-Host ("SSH_TARGET: " + $(if ($SshTarget) { "present" } else { "missing" }))
Write-Host ("APP_DIR: " + $(if ($AppDir) { "present" } else { "missing" }))
Write-Host ("BRANCH: " + $Branch)
Write-Host ("BOBBUY_AGENT_AUTH_TOKEN: " + $(if ($AgentAuthToken) { "present" } else { "missing" }))

if (-not $SshTarget -or -not $AppDir) {
  Write-Host "ERROR: Missing required SSH_TARGET or APP_DIR. Set them in the current shell and rerun."
  exit 2
}

$precheck = @'
set -euo pipefail
APP_DIR="$1"
BRANCH="$2"
echo "hostname=$(hostname)"
echo "date=$(date -Is)"
if test -d "$APP_DIR"; then echo "repo_dir=pass"; else echo "repo_dir=fail"; exit 20; fi
if test -f "$APP_DIR/.env"; then echo "env_file=pass"; else echo "env_file=fail"; exit 21; fi
echo "branch=${BRANCH:-main}"
'@

Write-Section "Precheck"
Invoke-RemoteScript -Script $precheck -Arguments @($AppDir, $Branch)

if ($PrecheckOnly) {
  Write-Section "Precheck-only mode"
  Write-Host "Precheck completed. Full release window was not executed."
  exit 0
}

if (-not $AgentAuthToken) {
  Write-Host "ERROR: Missing BOBBUY_AGENT_AUTH_TOKEN. It is required for the authenticated AI sample gate."
  exit 3
}

$releaseWindow = @'
set -euo pipefail
APP_DIR="$1"
BRANCH="$2"
AGENT_AUTH_TOKEN="$3"

cd "$APP_DIR"
echo "step=git_update"
git fetch origin
git checkout "${BRANCH:-main}"
git pull --ff-only
echo "commit=$(git rev-parse HEAD)"
echo "server_time=$(date -Is)"

echo "step=env_required_keys"
required_keys="
BOBBUY_SECURITY_JWT_SECRET
BOBBUY_SECURITY_SERVICE_TOKEN
POSTGRES_PASSWORD
MINIO_ROOT_PASSWORD
RABBITMQ_DEFAULT_PASS
BOBBUY_AI_LLM_CODEX_BRIDGE_URL
BOBBUY_AI_LLM_CODEX_BRIDGE_API_KEY
"
missing=0
for key in $required_keys; do
  if grep -q "^${key}=" .env; then
    echo "${key}=present"
  else
    echo "${key}=missing"
    missing=1
  fi
done
if [ "$missing" -ne 0 ]; then
  echo "required_env=fail"
  exit 30
fi
echo "required_env=pass"

echo "step=compose_config"
docker compose config --quiet

echo "step=build_service_images"
bash scripts/build-service-images.sh

echo "step=compose_up"
docker compose up -d --build postgres minio redis rabbitmq nacos nacos-init core-service ai-service im-service auth-service gateway-service frontend gateway ocr-service
docker compose ps

echo "step=health_checks"
curl -fsS http://127.0.0.1/api/health
curl -fsS http://127.0.0.1/api/actuator/health
curl -fsS http://127.0.0.1/api/actuator/health/readiness
curl -fsS http://127.0.0.1:8000/health

echo "step=ai_sample_gate"
if command -v pwsh >/dev/null 2>&1; then
  pwsh scripts/verify-ai-onboarding-samples.ps1 -IncludeNeedsHumanGolden -AuthToken "$AGENT_AUTH_TOKEN"
else
  powershell scripts/verify-ai-onboarding-samples.ps1 -IncludeNeedsHumanGolden -AuthToken "$AGENT_AUTH_TOKEN"
fi

echo "step=ai_e2e"
(cd frontend && RUN_AI_VISION_E2E=1 npm run e2e:ai)

echo "step=mobile_blackbox"
(cd frontend && RUN_REAL_MOBILE_BLACKBOX=1 npm run e2e -- e2e/mobile_customer_blackbox.spec.ts e2e/mobile_agent_blackbox.spec.ts)

echo "step=postgres_restore_drill"
backup_dir="${APP_DIR}/scratch/server-release-window"
mkdir -p "$backup_dir"
docker compose exec -T postgres pg_dump -U bobbuy bobbuy > "$backup_dir/postgres-release-window.sql"
docker compose exec -T postgres psql -U bobbuy -d postgres -c "DROP DATABASE IF EXISTS bobbuy_restore_verify;"
docker compose exec -T postgres psql -U bobbuy -d postgres -c "CREATE DATABASE bobbuy_restore_verify;"
docker compose exec -T postgres psql -U bobbuy -d bobbuy_restore_verify < "$backup_dir/postgres-release-window.sql"
docker compose exec -T postgres psql -U bobbuy -d bobbuy_restore_verify -c "SELECT COUNT(*) AS flyway_rows FROM flyway_schema_history;"
docker compose exec -T postgres psql -U bobbuy -d bobbuy_restore_verify -c "SELECT COUNT(*) AS products FROM products;"

echo "step=minio_restore_probe"
probe_file="$backup_dir/minio-probe.txt"
echo "server-release-window $(date -Is)" > "$probe_file"
docker compose cp "$probe_file" minio:/tmp/minio-probe.txt
docker compose exec -T minio sh -lc 'mc alias set local http://127.0.0.1:9000 "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD" >/dev/null && mc mb --ignore-existing local/bobbuy-restore-verify >/dev/null && mc cp /tmp/minio-probe.txt local/bobbuy-restore-verify/minio-probe.txt >/dev/null && mc stat local/bobbuy-restore-verify/minio-probe.txt >/dev/null'
echo "minio_restore_probe=pass"

echo "step=nacos_archive_probe"
nacos_archive="$backup_dir/nacos-config-archive.txt"
docker compose logs --no-color nacos-init > "$nacos_archive"
test -s "$nacos_archive"
echo "nacos_archive=$nacos_archive"

echo "release_window=pass"
'@

Write-Section "Full server release window"
Invoke-RemoteScript -Script $releaseWindow -Arguments @($AppDir, $Branch, $AgentAuthToken)
