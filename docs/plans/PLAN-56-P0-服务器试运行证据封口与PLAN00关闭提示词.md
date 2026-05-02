# PLAN-56: P0 服务器试运行证据封口与 PLAN-00 关闭提示词

## Summary

目标是关闭 `PLAN-00` 中仍处于执行中的历史收口任务。PLAN-40/41/42/44/47/48/49 已被 REPORT-07、PLAN-50~55、REPORT-10/11/12 覆盖，按历史覆盖项归档为完成。PLAN-24、PLAN-54、PLAN-55、CURRENT 只在服务器部署窗口证据通过后关闭。

当前执行前置缺口：尚未提供 `SSH_TARGET` 与 `APP_DIR`，因此本轮不能真实执行服务器窗口，只能完成文档封口准备与历史计划归档。

## Key Changes

- 使用 Linux SSH 作为服务器执行入口，secret 默认从服务器 `.env` 读取。
- 不读取、不回传、不提交任何明文 secret。
- 服务器证据只归档摘要、状态、命令结果和 artifact 路径；敏感原始日志不入库。
- 如果服务器窗口全部通过，则将 REPORT-07 复判为 `GO_INTERNAL_TRIAL`，并把 PLAN-24/54/55/CURRENT 关闭。
- 如果服务器窗口未执行或失败，则保持 `GO_INTERNAL_TRIAL_PENDING_SERVER_WINDOW`，只关闭历史覆盖项。

## Required Inputs

执行者在本地 shell 临时提供：

```powershell
$env:SSH_TARGET = "user@server"
$env:APP_DIR = "/opt/bobbuy"
$env:BRANCH = "main"
```

## Server Execution

在服务器上执行：

```bash
cd "$APP_DIR"
git fetch origin
git checkout "$BRANCH"
git pull --ff-only
test -f .env
```

只校验变量存在，不输出值：

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

启动与健康检查：

```bash
docker compose config --quiet
bash scripts/build-service-images.sh
docker compose up -d --build postgres minio redis rabbitmq nacos nacos-init core-service ai-service im-service auth-service gateway-service frontend gateway ocr-service
docker compose ps
curl -fsS http://127.0.0.1/api/health
curl -fsS http://127.0.0.1/api/actuator/health
curl -fsS http://127.0.0.1/api/actuator/health/readiness
curl -fsS http://127.0.0.1:8000/health
```

真实功能门禁：

```bash
pwsh scripts/verify-ai-onboarding-samples.ps1 -IncludeNeedsHumanGolden -AuthToken "<agent-token>"
cd frontend && RUN_AI_VISION_E2E=1 npm run e2e:ai
cd frontend && RUN_REAL_MOBILE_BLACKBOX=1 npm run e2e -- e2e/mobile_customer_blackbox.spec.ts e2e/mobile_agent_blackbox.spec.ts
```

备份恢复演练：

```bash
docker compose exec -T postgres pg_dump -U "$POSTGRES_USER" "$POSTGRES_DB" > /tmp/bobbuy-trial-backup.sql
docker compose exec -T postgres createdb -U "$POSTGRES_USER" bobbuy_restore_verify
docker compose exec -T postgres psql -U "$POSTGRES_USER" bobbuy_restore_verify < /tmp/bobbuy-trial-backup.sql
docker compose exec -T postgres psql -U "$POSTGRES_USER" bobbuy_restore_verify -c "select count(*) from flyway_schema_history;"
```

MinIO 与 Nacos 恢复按 `docs/runbooks/RUNBOOK-备份恢复演练.md` 执行，并把恢复 bucket、归档文件路径与校验结果写入 REPORT-13。

## Test Plan

本地提交前：

- `docker compose config --quiet`
- `.\mvnw.cmd -f backend\pom.xml test`
- `npm test --prefix frontend`
- `npm run build --prefix frontend`
- `npm run e2e --prefix frontend`

服务器通过标准：

- Compose 服务健康或 running 状态符合 REPORT-10/12。
- AI sample gate `gatePassed=true`。
- `RUN_AI_VISION_E2E=1 npm run e2e:ai` 通过。
- 真实栈双角色黑盒 `4 passed`。
- PostgreSQL、MinIO、Nacos 恢复演练有可审计摘要。

## Assumptions

- 无历史业务数据仍为事实，旧库 adoption 保持 N/A。
- Codex Bridge key、JWT secret、service token、数据库与中间件密码只存在于服务器 `.env` 或 secret manager。
- 如果缺少 SSH 输入，本计划不得把服务器窗口标为通过。
