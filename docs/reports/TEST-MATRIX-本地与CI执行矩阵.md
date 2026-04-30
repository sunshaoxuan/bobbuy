# 本地 / CI 测试执行矩阵

> 2026-04-30 更新：默认上线门禁以 `.github/workflows/ci.yml` 为准。后端 `mvn test`、前端 `npm ci && npm test`、前端 `npm run build` 已恢复为默认门禁；部署前还需额外执行 `docker compose config` 做配置渲染校验；Flyway PostgreSQL migration 验证、服务壳 smoke test、Playwright、AI 真实视觉链路与安全扫描按分层策略执行。`REPORT-06` 已记录最新放行结论为 `NO_GO`：默认 CI 与本地 smoke 已通过，但 CodeQL 实跑、Maven 可信依赖审计、真实 AI/OCR 与真实旧库 adoption 证据仍未完成。

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
| 服务壳 smoke test | `cd /home/runner/work/bobbuy/bobbuy && mvn -pl bobbuy-core,bobbuy-ai,bobbuy-im,bobbuy-auth,bobbuy-gateway -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest='*SmokeTest,*InternalServiceHeaderFilterTest' test` | 当前仅本地 / 手动执行 | 否 | 使用 H2 与关闭 Nacos 的测试配置，验证 `core-service`、`ai-service`、`im-service`、`auth-service`、`gateway-service` 最小启动以及 gateway 内部 header 清理 |
| Playwright 页面回归 | `cd /home/runner/work/bobbuy/bobbuy/frontend && npm run e2e` | `workflow_dispatch` + `playwright-e2e` job（输入 `run_playwright_e2e=true`） | 否 | GitHub Hosted Runner 可执行；使用 Vite dev server + 前端共享 mock 浏览器 smoke，覆盖 agent/customer 登录态、角色门禁、订单/账单/聊天、采购/拣货/库存人工接管，不依赖真实 AI / MinIO |
| AI 真实视觉链路 | `cd /home/runner/work/bobbuy/bobbuy/frontend && npm run e2e:ai` | 不纳入默认 Hosted CI；仅在专用环境手动执行并单独记录结果 | 否 | 必须提供 `RUN_AI_VISION_E2E=1`、`SPRING_PROFILES_ACTIVE=dev,ai-hermes`、可访问的 AI 模型、MinIO、seed 数据与样本图片 |
| AI sample 字段级对比（gate 模式） | `pwsh /home/runner/work/bobbuy/bobbuy/scripts/verify-ai-onboarding-samples.ps1 -IncludeNeedsHumanGolden` | 当前仅本地 / 专用环境执行 | 否 | 需要真实 `/api/ai/onboard/scan`、样本图片目录与 `docs/fixtures/ai-onboarding-sample-golden.json`；输出 `/tmp/ai-onboarding-sample-report.json` + `/tmp/ai-onboarding-sample-report.md`；遇到 `FAIL` / `SCAN_FAIL` / `MISSING_FILE` 返回非零 |
| AI sample 脚本 dry-run 自检（gate 模式） | `pwsh -NoProfile -Command "& '/home/runner/work/bobbuy/bobbuy/scripts/verify-ai-onboarding-samples.ps1' -MockScanResponsePath '/home/runner/work/bobbuy/bobbuy/docs/fixtures/ai-onboarding-sample-scan-mock.json' -SampleIds @('IMG_1484.jpg','IMG_1638.jpg') -IncludeNeedsHumanGolden"` | 当前仅本地执行 | 否 | 不依赖真实 `/api/ai/onboard/scan`；用于验证字段别名映射、optional path 规范化、报告格式与 gate 退出码 |
| AI sample 报告生成（report-only） | `pwsh -NoProfile -Command "& '/home/runner/work/bobbuy/bobbuy/scripts/verify-ai-onboarding-samples.ps1' -MockScanResponsePath '/home/runner/work/bobbuy/bobbuy/docs/fixtures/ai-onboarding-sample-scan-mock-fail.json' -SampleIds @('IMG_1484.jpg') -ReportOnly"` | 当前仅本地执行 | 否 | 仅用于人工生成 JSON/Markdown 报告；即使 `gatePassed=false` 也返回 `0`，不得作为 release gate |
| Compose 配置渲染 | `cd /home/runner/work/bobbuy/bobbuy && docker compose config` | 当前未纳入默认 CI；作为试运行部署前置校验执行 | 否 | 要求 `.env` / 默认变量可成功渲染 Compose，且不得依赖未声明变量 |
| 备份恢复演练 | 见 `docs/runbooks/RUNBOOK-备份恢复演练.md` | 不纳入默认 CI；按试运行变更窗口手工执行并记录结果 | 否 | 需要 Docker / PostgreSQL / MinIO / Nacos 可访问，且恢复验证必须在新库 / 独立 bucket / 独立目录进行 |

## 3. 风险登记 / 独立安全门禁

| 验证项 | 默认状态 | 执行要求 | 备注 |
| :-- | :-- | :-- | :-- |
| CodeQL / 安全扫描 | 不纳入默认 `ci.yml` | 手动触发 `.github/workflows/codeql.yml`；若本次 PR / Release 未执行，必须登记为风险项，不得写成“已通过” | 覆盖 Java/Kotlin、JavaScript/TypeScript、Actions |
| 依赖审计 | 不纳入默认 `ci.yml` | 按需独立执行；前端使用 `npm audit --json`，后端优先执行 OWASP Dependency-Check Maven | 若因网络/沙箱限制失败，必须记录具体错误与发版前责任边界 |

## 4. 执行约束与已知噪声

- `frontend-quality` job 通过一次 `npm ci --prefix frontend` 复用同一份依赖，避免测试与构建重复安装。
- 新 clone 或本地清空 `node_modules` 后，必须先执行 `npm ci --prefix frontend`，再运行前端测试 / 构建。
- PostgreSQL migration 验证默认通过 Flyway Maven Plugin 执行，不要求后端在 Hosted CI 中额外拉起 Redis/RabbitMQ/MinIO。
- 试运行 Compose 当前固定 PostgreSQL 15，并默认把宿主机端口绑定到 `127.0.0.1`，避免内部依赖无意暴露到公网。
- `playwright-e2e` 继续复用 `npm run e2e:prepare` 的浏览器探测逻辑，仅在缺失 Chromium 时安装；CI 额外执行 `npx playwright install --with-deps chromium` 并上传 `frontend/playwright-report`、`frontend/test-results` artifact。
- Playwright smoke 现在使用带 access token + HttpOnly refresh cookie + CSRF cookie 的 mock 浏览器会话，不再依赖 `bobbuy_test_role` / `bobbuy_test_user` 伪造角色头。
- 后端默认测试必须继续使用 H2 / fake/mock 资源，禁止默认门禁外连真实 Ollama、Codex CLI、MinIO 或公网服务。
- AI/OCR 可靠性用例（provider unconfigured、OCR/LLM 失败、fallback、人工复核、重试）必须继续保留在默认 mock 测试中，禁止切换到真实外部服务。
- `docs/fixtures/ai-onboarding-sample-golden.json` 中标记 `needsHumanGolden=true` 的样例不会阻断默认脚本，需要人工补齐黄金值后再提升为强制门禁。
- `scripts/verify-ai-onboarding-samples.ps1` 当前已内建 `basePrice -> price` 实际字段别名、`expected.` optional path 规范化、`gatePassed` 汇总，以及 gate/report-only 分流；真实门禁仍必须保留 golden 字段名，不得把脚本修复等同于放宽黄金值判定。
- 后端 `mvn test` 现同时覆盖 JWT 登录、HttpOnly refresh cookie 下发、refresh/logout 的 CSRF 拒绝、refresh token 单次轮换/过期/撤销、并发 refresh 只成功一次、`/api/auth/me`、401/403、customer 本人数据隔离、WebSocket STOMP `CONNECT` 鉴权与聊天上下文授权，以及 `bobbuy.security.header-auth.enabled=false` 时伪造 header 不得提权。
- 前端单测已覆盖 access token 持久化、refresh token 不再写入 localStorage、HTTP 401 单轮 refresh+retry、并发 401 合并为单轮 refresh、refresh/logout 携带 cookie + `X-BOBBUY-CSRF-TOKEN`、refresh 失败清理登录态，以及 WebSocket STOMP 使用 Bearer token、鉴权失败后 refresh 一次并在失败时停止重连。
- `core-service` / `ai-service` / `im-service` / `auth-service` / `gateway-service` 当前已补最小模块启动 smoke test，但尚无契约测试、独立 schema 验证与拆分后 CI/CD；若后续要继续拆分，仍需补齐这些门禁。
- 运维基线当前按“默认门禁 + 手工 Runbook 校验”执行：`docker compose config`、健康检查、日志巡检、备份恢复演练不进入默认 Hosted CI。
- 当前已知前端测试噪声：
  - Ant Design `useForm` 未连接 warning。
  - 预期失败路径中的 `Delete failed: Error: Server error` console 输出。
  以上噪声目前不阻断默认门禁，但应继续在后续清理项中跟踪。

## 5. 2026-04-30 最新验证结果

- [x] `cd /home/runner/work/bobbuy/bobbuy && docker compose config`
- [x] GitHub Actions `BOBBuy CI` run <https://github.com/sunshaoxuan/bobbuy/actions/runs/25141502571>
  - `backend-test` / `frontend-quality` / `docker-build` 成功
  - `playwright-e2e` / `postgres-migration-verify` 因手动输入未开启而 skipped
- [x] `cd /home/runner/work/bobbuy/bobbuy/backend && mvn test`
- [x] `cd /home/runner/work/bobbuy/bobbuy/frontend && npm ci && npm test`
- [x] `cd /home/runner/work/bobbuy/bobbuy/frontend && npm run build`
- [x] `cd /home/runner/work/bobbuy/bobbuy/backend && ./mvnw test -Dtest=ChatControllerSecurityIntegrationTest,WebSocketAuthenticationChannelInterceptorTest`
- [x] `cd /home/runner/work/bobbuy/bobbuy/backend && mvn test -Dtest=AuthControllerIntegrationTest,AuthRefreshTokenExpiryIntegrationTest`
- [x] `cd /home/runner/work/bobbuy/bobbuy/frontend && npm test -- --run src/hooks/useChatWebSocket.test.tsx src/components/ChatWidget.test.tsx`
- [x] `cd /home/runner/work/bobbuy/bobbuy/frontend && npm test -- --run src/api.test.ts src/context/UserRoleContext.test.tsx src/pages/LoginPage.test.tsx src/hooks/useChatWebSocket.test.tsx`
- [x] AI/OCR 默认单测继续使用 fake/mock，不连接真实 Ollama、Codex CLI、OCR service、MinIO
- [x] `cd /home/runner/work/bobbuy/bobbuy/backend && mvn -DskipTests package`
- [x] `cd /home/runner/work/bobbuy/bobbuy && docker build backend -t bobbuy-backend-test`
- [x] `cd /home/runner/work/bobbuy/bobbuy && docker build frontend -t bobbuy-frontend-test`
- [x] `cd /home/runner/work/bobbuy/bobbuy/backend && mvn -Dflyway.url=jdbc:postgresql://localhost:5432/bobbuy -Dflyway.user=bobbuy -Dflyway.password=bobbuypassword -Dflyway.cleanDisabled=false flyway:clean flyway:migrate flyway:validate`
- [x] `cd /home/runner/work/bobbuy/bobbuy && mvn -pl bobbuy-core,bobbuy-ai,bobbuy-im,bobbuy-auth,bobbuy-gateway -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest='*SmokeTest,*InternalServiceHeaderFilterTest' test`
- [x] 按 `docs/runbooks/RUNBOOK-备份恢复演练.md` 执行一次本地基础设施级恢复演练
  - PostgreSQL：`pg_dump` -> 新库 `bobbuy_restore_verify` 恢复校验通过
  - MinIO：恢复验证 bucket `bobbuy-media-restore-verify` 中存在 `probe.txt`
  - Nacos：`infra/nacos/config` 已归档为 `/tmp/plan33/backup/nacos-config-restore-drill.tgz`
  - 未执行完整应用栈登录 / 页面预览验收，后续需在专门试运行窗口继续补全
- [x] `cd /home/runner/work/bobbuy/bobbuy/frontend && npm run e2e`
  - 本次实际执行：`46 passed / 2 skipped`
  - `2 skipped` 为 `npm run e2e:ai` 专用的 `RUN_AI_VISION_E2E` 门控用例
- [x] `pwsh -NoProfile -Command "& '/home/runner/work/bobbuy/bobbuy/scripts/verify-ai-onboarding-samples.ps1' -MockScanResponsePath '/home/runner/work/bobbuy/bobbuy/docs/fixtures/ai-onboarding-sample-scan-mock.json' -SampleIds @('IMG_1484.jpg','IMG_1638.jpg') -IncludeNeedsHumanGolden"`
  - gate 模式返回 `0`
  - `IMG_1484.jpg`：验证 `expected.basePrice` 可命中实际 `price`
  - `IMG_1638.jpg`：验证 `expected.existingProductId` optional path 规范化为 `optional-missing`
- [x] `pwsh -NoProfile -Command "& '/home/runner/work/bobbuy/bobbuy/scripts/verify-ai-onboarding-samples.ps1' -MockScanResponsePath '/home/runner/work/bobbuy/bobbuy/docs/fixtures/ai-onboarding-sample-scan-mock-fail.json' -SampleIds @('IMG_1484.jpg')"`
  - gate 模式返回非零
  - 报告仍输出，`summary.gatePassed=false`
- [x] `pwsh -NoProfile -Command "& '/home/runner/work/bobbuy/bobbuy/scripts/verify-ai-onboarding-samples.ps1' -MockScanResponsePath '/home/runner/work/bobbuy/bobbuy/docs/fixtures/ai-onboarding-sample-scan-mock-fail.json' -SampleIds @('IMG_1484.jpg') -ReportOnly"`
  - report-only 返回 `0`
  - 仅用于人工报告，不得代替门禁
- [x] `cd /home/runner/work/bobbuy/bobbuy/frontend && npm audit --json`
  - 结果：`0 critical / 0 high / 6 moderate`
- [x] `.github/workflows/codeql.yml`
  - 已补手动 CodeQL workflow
  - 截至 `2026-04-30` workflow run 数量仍为 `0`
- [x] 本地 PostgreSQL 15 Flyway 与恢复演练
  - `docker compose up -d postgres`
  - `cd /home/runner/work/bobbuy/bobbuy/backend && mvn -Dflyway.url=jdbc:postgresql://localhost:5432/bobbuy -Dflyway.user=bobbuy -Dflyway.password=bobbuypassword -Dflyway.cleanDisabled=false flyway:clean flyway:migrate flyway:validate`
  - `pg_dump -> bobbuy_restore_verify_plan40` 恢复校验通过
- [ ] `cd /home/runner/work/bobbuy/bobbuy/backend && mvn -B org.owasp:dependency-check-maven:12.1.8:check -Dformat=HTML,JSON -DoutputDirectory=/tmp/plan42-dependency-check -DskipProvidedScope=true -DskipTestScope=true`
  - 当前受 `UnknownHostException: www.cisa.gov` 阻塞，未生成可信 HTML/JSON 报告
- [ ] `cd /home/runner/work/bobbuy/bobbuy/frontend && npm run e2e:ai`
- [ ] `pwsh /home/runner/work/bobbuy/bobbuy/scripts/verify-ai-onboarding-samples.ps1 -IncludeNeedsHumanGolden`
- [ ] CodeQL 实跑与 code scanning 结果归档（workflow 已补，但 run 数量仍为 `0`）
- [ ] mTLS / service mesh / 契约测试（本阶段未实现，需继续登记风险）
