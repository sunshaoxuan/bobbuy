param(
    [ValidateSet("ai-e2e", "mobile-blackbox")]
    [string]$Mode,
    [string]$BaseUrl = "http://127.0.0.1"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$frontendDir = Join-Path $repoRoot "frontend"

Push-Location $frontendDir
try {
    $env:PLAYWRIGHT_BASE_URL = $BaseUrl
    $env:PLAYWRIGHT_SKIP_WEB_SERVER = "1"

    if ($Mode -eq "ai-e2e") {
        $env:RUN_AI_VISION_E2E = "1"
        npm run e2e:ai
    } else {
        $env:RUN_REAL_MOBILE_BLACKBOX = "1"
        npm run e2e -- e2e/mobile_customer_blackbox.spec.ts e2e/mobile_agent_blackbox.spec.ts
    }

    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
} finally {
    Remove-Item Env:PLAYWRIGHT_BASE_URL,Env:PLAYWRIGHT_SKIP_WEB_SERVER,Env:RUN_AI_VISION_E2E,Env:RUN_REAL_MOBILE_BLACKBOX -ErrorAction SilentlyContinue
    Pop-Location
}
