# PLAN-57: P0 关闭剩余执行中任务与服务器放行复判提示词

## Summary

目标是关闭 PLAN-00 中剩余的执行中任务：PLAN-24、PLAN-56、PLAN-54、PLAN-55、CURRENT。关闭条件只有一个：服务器部署窗口真实执行并通过完整 P0 门禁。若缺少服务器输入或任一 P0 门禁失败，只更新阻塞证据，不把任务标为完成。

当前状态：本地执行环境未提供 `SSH_TARGET` 与 `APP_DIR`，因此本轮只能完成执行提示词与文档口径收口，服务器窗口仍未执行。

## Required Inputs

执行前在本地 shell 临时提供：

```powershell
$env:SSH_TARGET = "user@server"
$env:APP_DIR = "/opt/bobbuy"
$env:BRANCH = "main"
```

不得把服务器 `.env`、JWT secret、service token、Codex Bridge key、数据库或中间件密码写入仓库。

## Server Execution

通过 SSH 在服务器执行：

```bash
cd "$APP_DIR"
git fetch origin
git checkout "${BRANCH:-main}"
git pull --ff-only
test -f .env
```

只校验 secret 变量是否存在，不输出值：

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

真实放行门禁：

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

MinIO 恢复到独立 bucket 并校验探针对象；Nacos 配置归档并校验归档文件存在。两项按 `docs/runbooks/RUNBOOK-备份恢复演练.md` 执行，摘要写入 REPORT-13。

## Documentation Close Rules

全部 P0 门禁通过后：

- REPORT-13 写入服务器执行时间、commit、服务状态、健康检查、AI sample、AI E2E、真实栈双角色黑盒、备份恢复摘要与 artifact 路径。
- REPORT-07 改为 `GO_INTERNAL_TRIAL`。
- PLAN-00 中 PLAN-24、PLAN-54、PLAN-55、PLAN-56、CURRENT 全部标记为 `✅ 已完成`。
- TEST-MATRIX、CURRENT-STATE、REPORT-12 同步服务器放行事实。

若输入缺失或失败：

- REPORT-13 写明唯一阻塞项与下一步输入。
- REPORT-07 保持 `GO_INTERNAL_TRIAL_PENDING_SERVER_WINDOW`。
- PLAN-56 保持执行中；PLAN-24、PLAN-54、PLAN-55、CURRENT 不关闭。

## Local Test Plan

提交前本地验证：

- `docker compose config --quiet`
- `.\mvnw.cmd -f backend\pom.xml test`
- `npm test --prefix frontend`
- `npm run build --prefix frontend`
- `npm run e2e --prefix frontend`

## Assumptions

- 当前无历史业务数据仍成立，旧库 adoption 继续为 N/A。
- 服务器 secret 由 `.env` 或 secret manager 提供，不进入 git。
- 没有 `SSH_TARGET` / `APP_DIR` 时，不得把服务器窗口标为通过。
