# 本地 / CI 测试执行矩阵

> 2026-04-29 更新：默认上线门禁以 `.github/workflows/ci.yml` 为准。后端 `mvn test`、前端 `npm ci && npm test`、前端 `npm run build` 已恢复为默认门禁；部署前还需额外执行 `docker compose config` 做配置渲染校验；Flyway PostgreSQL migration 验证、Playwright、AI 真实视觉链路与安全扫描按分层策略执行。`core-service` / `ai-service` / `im-service` / `auth-service` 当前仍复用 `backend` 共享代码，因此默认门禁不把它们当作已经拥有独立测试边界的微服务。

## 1. 默认门禁（每个 PR / `main` push 必跑）

| 验证项 | 本地命令 | CI job / step | 默认门禁 | 外部依赖 |
| :-- | :-- | :-- | :-- | :-- |
| 后端测试 | `cd /home/runner/work/bobbuy/bobbuy/backend && mvn test` | `backend-test` / `Run backend tests` | 是 | 否；默认使用测试资源与 fake/mock 配置，不依赖真实 Ollama、Codex CLI、MinIO、外网 AI 服务 |
| 前端单测 | `cd /home/runner/work/bobbuy/bobbuy/frontend && npm ci && npm test` | `frontend-quality` / `Install dependencies` + `Run frontend tests` | 是 | 否 |
| 前端构建 | `cd /home/runner/work/bobbuy/bobbuy/frontend && npm run build` | `frontend-quality` / `Build frontend` | 是 | 否 |
| 后端 Docker 构建 | `cd /home/runner/work/bobbuy/bobbuy/backend && mvn -DskipTests package && cd /home/runner/work/bobbuy/bobbuy && docker build backend -t bobbuy-backend-test` | `docker-build` / `Build backend package for Docker image` + `Build backend image` | 是 | 否 |
| 前端 Docker 构建 | `cd /home/runner/work/bobbuy/bobbuy && docker build frontend -t bobbuy-frontend-test` | `docker-build` / `Build frontend image` | 是 | 否 |

## 2. 专用环境门禁（手动触发 / 条件执行）

| 验证项 | 本地命令 | CI 触发方式 | 默认门禁 | 环境要求 |
| :-- | :-- | :-- | :-- | :-- |
| PostgreSQL 空库 migration | `cd /home/runner/work/bobbuy/bobbuy/backend && mvn -Dflyway.url=jdbc:postgresql://localhost:5432/bobbuy -Dflyway.user=bobbuy -Dflyway.password=bobbuypassword -Dflyway.cleanDisabled=false flyway:clean flyway:migrate flyway:validate` | `workflow_dispatch` + `postgres-migration-verify` job（输入 `run_postgres_migration_verify=true`） | 否 | 需要可写 PostgreSQL 空库；默认由 Flyway 插件验证 `backend/src/main/resources/db/migration` |
| Playwright 页面回归 | `cd /home/runner/work/bobbuy/bobbuy/frontend && npm run e2e` | `workflow_dispatch` + `playwright-e2e` job（输入 `run_playwright_e2e=true`） | 否 | GitHub Hosted Runner 可执行；常规用例走前端共享 mock，不依赖真实 AI / MinIO |
| AI 真实视觉链路 | `cd /home/runner/work/bobbuy/bobbuy/frontend && npm run e2e:ai` | 不纳入默认 Hosted CI；仅在专用环境手动执行并单独记录结果 | 否 | 必须提供 `RUN_AI_VISION_E2E=1`、`SPRING_PROFILES_ACTIVE=dev,ai-hermes`、可访问的 AI 模型、MinIO、seed 数据与样本图片 |
| Compose 配置渲染 | `cd /home/runner/work/bobbuy/bobbuy && docker compose config` | 当前未纳入默认 CI；作为试运行部署前置校验执行 | 否 | 要求 `.env` / 默认变量可成功渲染 Compose，且不得依赖未声明变量 |
| 备份恢复演练 | 见 `docs/runbooks/RUNBOOK-备份恢复演练.md` | 不纳入默认 CI；按试运行变更窗口手工执行并记录结果 | 否 | 需要 Docker / PostgreSQL / MinIO / Nacos 可访问，且恢复验证必须在新库 / 独立 bucket / 独立目录进行 |

## 3. 风险登记 / 独立安全门禁

| 验证项 | 默认状态 | 执行要求 | 备注 |
| :-- | :-- | :-- | :-- |
| CodeQL / 安全扫描 | 不纳入默认 `ci.yml` | 独立执行；若本次 PR / Release 未执行，必须登记为风险项，不得写成“已通过” | 如出现超时或平台限制，需记录原因与缓解动作 |
| 依赖审计 | 不纳入默认 `ci.yml` | 按需独立执行；未执行时同样登记风险 | 重点关注 npm / Maven 高危依赖 |

## 4. 执行约束与已知噪声

- `frontend-quality` job 通过一次 `npm ci --prefix frontend` 复用同一份依赖，避免测试与构建重复安装。
- 新 clone 或本地清空 `node_modules` 后，必须先执行 `npm ci --prefix frontend`，再运行前端测试 / 构建。
- PostgreSQL migration 验证默认通过 Flyway Maven Plugin 执行，不要求后端在 Hosted CI 中额外拉起 Redis/RabbitMQ/MinIO。
- 试运行 Compose 当前固定 PostgreSQL 15，并默认把宿主机端口绑定到 `127.0.0.1`，避免内部依赖无意暴露到公网。
- `playwright-e2e` 继续复用 `npm run e2e:prepare` 的浏览器探测逻辑，仅在缺失 Chromium 时安装。
- 后端默认测试必须继续使用 H2 / fake/mock 资源，禁止默认门禁外连真实 Ollama、Codex CLI、MinIO 或公网服务。
- AI/OCR 可靠性用例（provider unconfigured、OCR/LLM 失败、fallback、人工复核、重试）必须继续保留在默认 mock 测试中，禁止切换到真实外部服务。
- 后端 `mvn test` 现同时覆盖 JWT 登录、`/api/auth/me`、401/403、customer 本人数据隔离、WebSocket STOMP `CONNECT` 鉴权与聊天上下文授权，以及 `bobbuy.security.header-auth.enabled=false` 时伪造 header 不得提权。
- 前端单测已覆盖 WebSocket STOMP 连接携带 Bearer token、token 缺失时静默降级为 REST 路径、鉴权失败时停止重连。
- `core-service` / `ai-service` / `im-service` / `auth-service` / `gateway-service` 当前无独立模块级测试；若后续要把服务外壳降级为 optional/profile 或继续拆分，必须先补模块启动 smoke test、服务间鉴权与拆分后 CI/CD。
- 运维基线当前按“默认门禁 + 手工 Runbook 校验”执行：`docker compose config`、健康检查、日志巡检、备份恢复演练不进入默认 Hosted CI。
- 当前已知前端测试噪声：
  - Ant Design `useForm` 未连接 warning。
  - 预期失败路径中的 `Delete failed: Error: Server error` console 输出。
  以上噪声目前不阻断默认门禁，但应继续在后续清理项中跟踪。

## 5. 2026-04-29 本地验证结果

- [x] `cd /home/runner/work/bobbuy/bobbuy && docker compose config`
- [x] `cd /home/runner/work/bobbuy/bobbuy/backend && mvn test`
- [x] `cd /home/runner/work/bobbuy/bobbuy/frontend && npm ci && npm test`
- [x] `cd /home/runner/work/bobbuy/bobbuy/frontend && npm run build`
- [x] `cd /home/runner/work/bobbuy/bobbuy/backend && ./mvnw test -Dtest=ChatControllerSecurityIntegrationTest,WebSocketAuthenticationChannelInterceptorTest`
- [x] `cd /home/runner/work/bobbuy/bobbuy/frontend && npm test -- --run src/hooks/useChatWebSocket.test.tsx src/components/ChatWidget.test.tsx`
- [x] AI/OCR 默认单测继续使用 fake/mock，不连接真实 Ollama、Codex CLI、OCR service、MinIO
- [x] `cd /home/runner/work/bobbuy/bobbuy/backend && mvn -DskipTests package`
- [x] `cd /home/runner/work/bobbuy/bobbuy && docker build backend -t bobbuy-backend-test`
- [x] `cd /home/runner/work/bobbuy/bobbuy && docker build frontend -t bobbuy-frontend-test`
- [x] `cd /home/runner/work/bobbuy/bobbuy/backend && mvn -Dflyway.url=jdbc:postgresql://localhost:5432/bobbuy -Dflyway.user=bobbuy -Dflyway.password=bobbuypassword -Dflyway.cleanDisabled=false flyway:clean flyway:migrate flyway:validate`
- [x] 按 `docs/runbooks/RUNBOOK-备份恢复演练.md` 执行一次本地基础设施级恢复演练
  - PostgreSQL：`pg_dump` -> 新库 `bobbuy_restore_verify` 恢复校验通过
  - MinIO：恢复验证 bucket `bobbuy-media-restore-verify` 中存在 `probe.txt`
  - Nacos：`infra/nacos/config` 已归档为 `/tmp/plan33/backup/nacos-config-restore-drill.tgz`
  - 未执行完整应用栈登录 / 页面预览验收，后续需在专门试运行窗口继续补全
- [ ] `cd /home/runner/work/bobbuy/bobbuy/frontend && npm run e2e`
  - 本次实际执行：`44 passed / 2 failed / 2 skipped`
  - 失败用例：`chat_publish_flow.spec.ts`、`client_role_gate.spec.ts`
- [ ] `cd /home/runner/work/bobbuy/bobbuy/frontend && npm run e2e:ai`
- [ ] CodeQL / 依赖审计（未纳入默认门禁，需单独执行或在 PR / Release 中登记）
- [ ] 独立服务间 service token / mTLS 验证（本阶段未实现，仅登记风险）
