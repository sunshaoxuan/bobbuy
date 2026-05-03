# PLAN-58: P0 服务器输入接入与放行窗口执行提示词

## Summary

目标是把 PLAN-57 从“等待服务器输入”推进到“服务器窗口可执行”。本计划不新增业务研发任务，只接入服务器连接输入、执行非敏感预检查，并在输入有效时执行 PLAN-57 全部门禁。

当前状态：本计划已完成。本机 WSL 放行窗口已通过，`scripts/run-server-release-window.ps1` 输出 `release_window=pass`；Compose health、真实 AI sample gate、真实 `e2e:ai`、真实栈双角色移动端黑盒、PostgreSQL / MinIO / Nacos 恢复演练全部闭环。PLAN-00 已无执行中任务，最终证据见 `docs/reports/REPORT-13-服务器试运行证据与PLAN00关闭报告.md`。

## Required Inputs

本机优先执行方式：

```powershell
pwsh scripts/run-server-release-window.ps1
```

本机会自动选择 `local-wsl`，并把当前 Windows 仓库路径转换为 WSL 路径作为 `APP_DIR`。仅预检查：

```powershell
pwsh scripts/run-server-release-window.ps1 -PrecheckOnly
```

外部 SSH 服务器执行时，才需要在本地 shell 临时提供：

```powershell
$env:RELEASE_WINDOW_TARGET = "ssh"
$env:SSH_TARGET = "user@server"
$env:APP_DIR = "/opt/bobbuy"
$env:BRANCH = "main"
```

`BRANCH` 未设置时固定按 `main` 执行。可选提供 `BOBBUY_AGENT_AUTH_TOKEN`；如果不提供，脚本会使用 `BOBBUY_E2E_AGENT_USERNAME` / `BOBBUY_E2E_AGENT_PASSWORD`，或默认 `agent` / `agent-pass`，在服务器健康检查通过后调用 `/api/auth/login` 生成临时 access token。不得把服务器 `.env`、JWT secret、service token、Codex Bridge key、数据库或中间件密码写入仓库。

本机 WSL 测试可临时生成 JWT secret 与 service token。Codex Bridge key 不得明文写入仓库；当前仓库 `.env` 使用密文字段，解密主密码来自外部用户环境变量：

```powershell
$env:BOBBUY_AI_SECRET_MASTER_PASSWORD = "<local-only master password>"
```

## Execution Wrapper

推荐入口：

```powershell
pwsh scripts/run-server-release-window.ps1
```

仅执行服务器输入与 SSH 预检查：

```powershell
pwsh scripts/run-server-release-window.ps1 -PrecheckOnly
```

脚本行为：

- `auto` 模式下优先选择 `local-wsl`；只有显式 `-Target ssh` 或设置 `SSH_TARGET` 时才要求 SSH。
- SSH 模式缺少 `SSH_TARGET` 或任一模式缺少 `APP_DIR` 时退出码为 `2`，只输出 `present/missing` 摘要。
- 缺少 `BOBBUY_AGENT_AUTH_TOKEN` 时不退出；脚本会尝试通过 `/api/auth/login` 临时生成 agent access token。
- 本机 WSL 模式下缺少 JWT secret 或 service token 时不写文件，脚本只在当前执行进程中临时生成。
- 缺少 AI Bridge URL/API key 时退出码为 `30`，因为真实 AI sample gate 不能用伪 key 闭环。
- 预检查通过后按本计划的服务器窗口顺序执行，不输出 `.env` 内容或任何 secret 值。
- 执行结果仍需由执行者整理到 REPORT-13；敏感原始日志不得入库。

## Server Precheck

只允许输出非敏感摘要：

```bash
ssh "$SSH_TARGET" "hostname; date; test -d '$APP_DIR'; test -f '$APP_DIR/.env'"
```

预检查记录到 REPORT-13：

- `SSH_TARGET`: present / missing
- `APP_DIR`: present / missing
- `BRANCH`: value, default `main`
- SSH connectivity: not_run / pass / fail
- repo directory: not_run / pass / fail
- `.env`: not_run / pass / fail

## Server Execution

预检查通过后，按 PLAN-57 固定顺序执行：

```bash
cd "$APP_DIR"
git fetch origin
git checkout "${BRANCH:-main}"
git pull --ff-only
test -f .env
```

只校验必需变量是否存在，不输出值：

```bash
for key in \
  BOBBUY_SECURITY_JWT_SECRET \
  BOBBUY_SECURITY_SERVICE_TOKEN \
  POSTGRES_PASSWORD \
  MINIO_ROOT_PASSWORD \
  RABBITMQ_DEFAULT_PASS \
  BOBBUY_AI_LLM_CODEX_BRIDGE_URL \
  BOBBUY_AI_LLM_CODEX_BRIDGE_API_KEY
do
  grep -q "^${key}=" .env && echo "${key}=present" || echo "${key}=missing"
done
```

执行完整放行窗口：

```bash
docker compose config --quiet
bash scripts/build-service-images.sh
docker compose up -d --build postgres minio redis rabbitmq nacos nacos-init core-service ai-service im-service auth-service gateway-service frontend gateway ocr-service
docker compose ps
curl -fsS http://127.0.0.1/api/health
curl -fsS http://127.0.0.1/api/actuator/health
curl -fsS http://127.0.0.1/api/actuator/health/readiness
curl -fsS http://127.0.0.1:8000/health
pwsh scripts/verify-ai-onboarding-samples.ps1 -IncludeNeedsHumanGolden -AuthToken "<agent-token>"
cd frontend && RUN_AI_VISION_E2E=1 npm run e2e:ai
cd frontend && RUN_REAL_MOBILE_BLACKBOX=1 npm run e2e -- e2e/mobile_customer_blackbox.spec.ts e2e/mobile_agent_blackbox.spec.ts
```

备份恢复演练：

- PostgreSQL: `pg_dump -> restore` 到隔离库，并校验核心表与 `flyway_schema_history`。
- MinIO: 恢复到独立 bucket，并校验探针对象。
- Nacos: 归档配置并校验归档文件存在。

## Documentation Close Rules

全部 P0 门禁通过后：

- REPORT-13 记录服务器执行时间、commit、服务状态、健康检查、AI sample、AI E2E、真实栈双角色黑盒、备份恢复摘要与 artifact 路径。
- REPORT-07 改为 `GO_INTERNAL_TRIAL`。
- PLAN-00 中 PLAN-24 / PLAN-54 / PLAN-55 / PLAN-56 / PLAN-57 / PLAN-58 / CURRENT 全部标记为 `✅ 已完成`。
- TEST-MATRIX、CURRENT-STATE、REPORT-12 同步服务器放行事实。

若输入缺失或失败：

- REPORT-13 写明第一个阻塞点、失败命令、非敏感摘要与下一步修复动作。
- REPORT-07 保持 `GO_INTERNAL_TRIAL_PENDING_SERVER_WINDOW`。
- PLAN-58 保持执行中；PLAN-24 / PLAN-54 / PLAN-55 / PLAN-56 / PLAN-57 / CURRENT 不关闭。

## Local Test Plan

提交前本地验证：

- `docker compose config --quiet`
- `.\mvnw.cmd -f backend\pom.xml test`
- `npm test --prefix frontend`
- `npm run build --prefix frontend`
- `npm run e2e --prefix frontend`

## Assumptions

- `BRANCH` 默认 `main`。
- 服务器 secret 已存在于服务器 `.env` 或 secret manager。
- 当前无历史业务数据仍成立，旧库 adoption 继续为 N/A。
- 没有 `SSH_TARGET` / `APP_DIR` 时，不得把服务器窗口标为通过。
