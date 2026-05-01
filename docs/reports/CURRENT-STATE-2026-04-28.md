# CURRENT STATE: BOBBuy 当前实现基线

**日期**: 2026-04-28（2026-04-29 部署、配置、微服务边界、WebSocket 鉴权、最小 service token 与服务壳 smoke 增量更新；2026-04-30 补充专用环境发版证据执行与放行判定、Codex Bridge provider；2026-05-01 补充依赖高危版本覆盖与 Compose 宿主机 jar 打包路径）
**状态**: 当前事实基线，用于替代过期 Release/Plan 口径
**依据**: `main` 分支当前代码、README、Release V14-V16、PLAN-20 至 PLAN-23、容器与测试配置

---

## 1. 系统目标

BOBBuy 的目标是服务真实线下代购业务：把客户咨询、商家采购、AI 识别、订单确认、线下结算、配送准备与财务审计串成一个可追溯闭环。

当前阶段目标不是“完整 SaaS 平台”，而是：

1. 支撑内部/小范围真实试运行。
2. 保证订单、采购、账单、拣货、商品上架、AI 辅助识别可用。
3. 保留人工确认与审计链，避免无人值守 AI 直接改账。
4. 为后续生产化认证、支付、地图、异步任务与服务拆分奠定边界。

---

## 2. 当前已实现能力

### 2.1 角色与入口

- `CUSTOMER`：发现商品、快捷下单、查看订单、查看账单、确认收货/账单、聊天。
- `AGENT`：仪表盘、行程、订单、采购 HUD、拣货、集中上架、供应商规则、用户档案、审计视图。
- 前端通过 `ProtectedRoute` 做页面级角色隔离；后端通过 Spring Security + `X-BOBBUY-ROLE` / `X-BOBBUY-USER` 做试运行级角色注入。

### 2.2 订单、行程、采购

- 行程与订单 CRUD、状态流转、容量校验、订单头行模型。
- Procurement HUD 覆盖利润看板、额外支出、物流跟踪、客户账单、线下收款、财务审计、钱包摘要。
- `COMPLETED` / `SETTLED` 进入结算冻结语义，后端拒绝会改变财务、履约或确认状态的动作。

### 2.3 客户账单与线下结算

- 客户可按行程查看 `businessId` 级账单。
- 账单包含订单行、实采数量、差额说明、本次应收、已收、待收、余额结转。
- 支持客户确认收货与确认账单。
- 线下收款方式限制为 `CASH`、`BANK_TRANSFER`、`OTHER`。
- 财务审计链记录关键确认、对账与复核动作。

### 2.4 小票核销与拣货

- 采购小票支持多图上传、原图/缩略图存储、AI 或规则回退识别。
- 复核结果保留 `rawRecognitionResult` 与 `manualReconciliationResult`。
- 小票识别结果现保留 `recognitionStatus`、`trace`、`traceHistory`，用于记录 provider / activeProvider / model / stage / latency / error / fallback / attempt。
- `/procurement` 与 `/picking` 共用 reviewed receipt + picking checklist 单一事实来源。
- 支持待配送客户列表与地址/经纬度 CSV 导出。

### 2.5 AI 商品上架

- 支持 OCR-first 商品识别、LLM 结构化、Web research、来源治理、既有商品匹配、人工确认。
- 支持供应商档案中的 `onboardingRules`，用于不同商家商品编号/品番规则提示。
- 集中上架页面支持 AI 识别结果人工修正后确认；若 AI/OCR 失败，则进入人工补录/重试路径并保留 trace。
- 商品主数据支持多语言名称/描述、品牌、价格、媒体、品番、价格层级，以及 `Product.attributes` 中的结构化字段（净含量、单位价格、包装规格、储存提示等）。

### 2.6 LLM 与 OCR

- 主文本 LLM provider 支持 `auto`：启动时探测 Ollama `/api/tags`，可用则走 Ollama，不可用则优先走 OpenAI-compatible `codex-bridge`，最后才走 Codex CLI 兜底。
- 运行中 Ollama 主节点失败时会优先切换到 `codex-bridge`；若未配置 bridge 但配置了 CLI，则切换到 Codex CLI。
- 视觉/图片任务仍走 edge 模型路径。
- `codex-bridge` 支持明文 key 环境注入或 AES-GCM 密文字段配置，解密主密码必须留在 git 外部；Codex CLI 兜底只适合本地场景，Linux 服务器容器内默认不可假定可用。
- 当前 AI/OCR 可观测性以响应 trace + 结构化日志为主；已补最小运维 Runbook、日志检索口径与轻量 `/api/metrics`，真实告警平台与长期成功率自动统计仍待后续阶段补齐。

### 2.7 聊天实时化

- 聊天已具备 REST 持久化 + WebSocket/STOMP 推送能力。
- 前端 `useChatWebSocket` 通过 STOMP `CONNECT` header 携带 `Authorization: Bearer <access-token>` 订阅 `/ws`；token 缺失时不建连，鉴权失败时停止重连并退回页面刷新/轮询补偿。
- Docker 形态下 `im-service` 作为聊天 / WebSocket 服务外壳承载入口，RabbitMQ STOMP 可作为 broker relay；其业务实现仍来自共享 `backend` 代码。
- 后端在 STOMP `CONNECT` / `SUBSCRIBE` 阶段校验 JWT，并限制 `CUSTOMER` 只能访问本人订单/行程聊天上下文；query token 方案未采用。

---

## 3. 当前架构事实

### 3.1 决策摘要

- 已通过 [ADR-01-试运行阶段服务边界决策](../architecture/ADR-01-试运行阶段服务边界决策.md) 固定试运行路线：**主业务单体优先稳定**。
- **主业务入口**：`backend`
- **事实源**：`core-service`
- **服务外壳**：`ai-service`、`im-service`、`auth-service`
- **可选服务**：`ocr-service` 属于 AI/OCR 增强能力依赖，不是核心业务事实源。
- **后续拆分候选**：`ai-service`、`im-service`、`auth-service`

### 3.2 模块 / 服务职责矩阵

| 模块 / 服务 | 是否有真实业务实现 | 是否被 Compose 启动 | 是否访问数据库 | 是否执行 Flyway | 是否需要 JWT 校验 | 是否有独立测试 | 试运行职责 |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| `backend` | 是 | 否 | 是 | 是 | 是 | 是 | 开发/测试主入口，包含主要 Controller、Service、Repository、Flyway 与默认自动化测试 |
| `bobbuy-common` | 否（共享依赖层） | 否 | 否 | 否 | 否 | 否 | 共享依赖、配置与 DTO/响应模型 |
| `core-service` | 部分（复用共享 `backend` 代码） | 是 | 是 | 是 | 是 | 最小 smoke | 订单、行程、采购、账单、钱包、审计等核心业务事实源 |
| `ai-service` | 部分（复用共享 `backend` 代码） | 是 | 是 | 否 | 是 | 最小 smoke | AI/OCR 服务外壳，不独立持有核心业务事实 |
| `im-service` | 部分（复用共享 `backend` 代码） | 是 | 是 | 否 | 是 | 最小 smoke | 聊天 / WebSocket 服务外壳，共享数据库与 RabbitMQ |
| `auth-service` | 部分（复用共享 `backend` 代码） | 是 | 是 | 否 | 是 | 最小 smoke | 登录 / JWT 服务外壳，不独立持有身份事实 |
| `gateway-service` | 否（路由层） | 是 | 否 | 否 | 否 | 最小 smoke | Spring Cloud Gateway 路由与健康检查 |

> 注：服务壳当前只有最小启动 smoke test，没有独立契约/业务测试；默认测试仍以 `backend` 与前端门禁为主。

### 3.3 部署形态

- Docker Compose 当前包含 PostgreSQL 15、MinIO、Redis、RabbitMQ、Nacos、core-service、ai-service、im-service、auth-service、gateway-service、frontend、nginx gateway、ocr-service。
- `Dockerfile.service` 当前直接复制宿主机预先构建的服务 jar；Compose 服务镜像不再在容器内执行 Maven，从而规避 `repo.maven.apache.org` 证书链 / PKIX 阻塞。
- Compose 服务固定 `SPRING_PROFILES_ACTIVE=prod`；配置优先级收敛为：环境变量 > Nacos > `application-{profile}.properties` > `application.properties`。
- 共享 PostgreSQL schema 的 migration 由 `core-service` 独占执行；其余服务通过 Compose + Nacos 保持 `spring.flyway.enabled=false`，避免并发迁移。
- 对宿主机开放的默认端口改为仅绑定 `127.0.0.1`，用于网关、PostgreSQL、MinIO Console/API、Nacos Console、OCR 调试入口。
- Gateway 路由目标：
  - `/api/auth/**` -> `auth-service`
  - `/api/chat/**`、`/ws/**` -> `im-service`
  - `/api/ai/**` -> `ai-service`
  - 其余 `/api/**` -> `core-service`
- 当前 Compose 更接近集成/试运行部署，不等同于生产高可用方案；虽然默认会启动多个服务外壳，但这不等于完成独立微服务事实源拆分。
- 已补服务壳启动 smoke test，但在独立 schema、契约测试与拆分后 CI/CD 准备完成前，当前仍不把 `ai-service` / `im-service` / `auth-service` 安全降级为 optional/profile。

### 3.4 最小运维基础

- 已新增 [`RUNBOOK-监控告警与故障处置`](../runbooks/RUNBOOK-监控告警与故障处置.md)，统一日志入口、健康检查、关键指标、告警阈值与故障处置清单。
- 已新增 [`RUNBOOK-备份恢复演练`](../runbooks/RUNBOOK-备份恢复演练.md)，覆盖 PostgreSQL、MinIO、Nacos 配置与 `.env` 边界。
- `RequestLoggingInterceptor` 当前会记录 `method/path/status/cost/trace_id/user/role/internal_service`，并回写 `X-Trace-Id` 响应头。
- `/api/metrics` 当前提供轻量 endpoint 请求次数、`p95/p99`、`4xx/5xx`、登录失败次数、全局 `5xx` 比率；AI/OCR、RabbitMQ、Redis、Nacos 仍以日志 + 人工巡检为主。
- 当前仍未接入 Prometheus/Grafana、集中日志平台、自动化备份任务与服务级 SLO。

---

## 4. 当前明确边界

1. 不提供真实第三方支付网关。
2. 不提供社交 OAuth / SSO。
3. 不提供真实地图路径规划或实时配送追踪。
4. 不承诺无人值守 AI 自动改账或自动发布商品。
5. 不承诺 Codex CLI 可在 Linux 服务器容器内直接使用；若使用个人 Codex 订阅桥接到云端服务器，必须通过受控 OpenAI-compatible bridge 与外部 secret 注入完成，不得把可直接恢复的明文或主密码提交到仓库。
6. 当前单体后端已切换为用户名/密码登录 + HMAC JWT access token；header auth 仅在 dev/test 显式开关开启时保留兼容，Compose / 试运行默认固定为 `false`。
7. WebSocket 握手仍允许匿名进入 `/ws`，但 STOMP `CONNECT`/`SUBSCRIBE` 已要求 Bearer access token；未登录、过期或伪造 token 会在消息通道阶段被拒绝。
8. 已补齐 refresh token 会话生命周期治理与浏览器端硬化：登录/刷新通过 `Set-Cookie` 下发 `bobbuy_refresh_token` HttpOnly SameSite cookie（默认 `Path=/api/auth`，生产建议 `Secure=true`），服务端只保存 refresh token hash；默认 access token TTL 为 `BOBBUY_SECURITY_JWT_TTL_SECONDS`（默认 3600 秒），refresh token TTL 为 `BOBBUY_SECURITY_REFRESH_TOKEN_TTL_SECONDS`（默认 604800 秒 / 7 天）。前端只在 localStorage 保存 access token、用户信息与过期时间，不再保存 refresh token 明文；`/api/auth/refresh`、`/api/auth/logout` 采用 `bobbuy_csrf_token` + `X-BOBBUY-CSRF-TOKEN` 的 double-submit CSRF 防护，且当前已由 Spring Security 对 cookie-backed refresh/logout 请求强制校验并返回 JSON 403；同一旧 refresh token 并发刷新只允许一次成功，其他请求返回 401。
9. 已落地最小 service token：gateway-service 会清理外部伪造的内部 header，并在配置 `BOBBUY_SECURITY_SERVICE_TOKEN` 后向下游附带可信 `X-BOBBUY-SERVICE-TOKEN` / `X-BOBBUY-INTERNAL-SERVICE`；后端仅对 `/internal/**` 信任该 token，普通业务接口仍以用户 JWT 为准。mTLS / service mesh 仍未完成，不得据此继续真实微服务深拆。
10. 已引入 Flyway 基线 migration；`backend` 提供 migration 源码，`core-service` 负责试运行部署中的 PostgreSQL schema 初始化，旧库 adoption 仍需备份与一次性 baseline 评估。

---

## 5. 质量与测试状态

### 5.1 最近验证

- `cd /home/runner/work/bobbuy/bobbuy && docker compose config`：通过。
- `cd backend && mvn test`：通过。
- `cd frontend && npm ci && npm test`：通过。
- `cd frontend && npm run build`：通过。
- `cd /home/runner/work/bobbuy/bobbuy && mvn -f pom.xml -DskipTests package -pl bobbuy-core,bobbuy-ai,bobbuy-im,bobbuy-auth,bobbuy-gateway -am`：通过。
- `cd /home/runner/work/bobbuy/bobbuy && docker compose build core-service ai-service im-service auth-service gateway-service`：通过。
- `cd frontend && npm run e2e`：通过（`46 passed / 2 skipped`；`2 skipped` 为 `RUN_AI_VISION_E2E` 门控用例）。
- `pwsh -NoProfile -Command "& '/home/runner/work/bobbuy/bobbuy/scripts/verify-ai-onboarding-samples.ps1' -MockScanResponsePath '/home/runner/work/bobbuy/bobbuy/docs/fixtures/ai-onboarding-sample-scan-mock.json' -SampleIds @('IMG_1484.jpg','IMG_1638.jpg') -IncludeNeedsHumanGolden"`：通过（gate 模式返回 `0`，已验证 `basePrice -> price` 别名与 optional path 规范化）
- `pwsh -NoProfile -Command "& '/home/runner/work/bobbuy/bobbuy/scripts/verify-ai-onboarding-samples.ps1' -MockScanResponsePath '/home/runner/work/bobbuy/bobbuy/docs/fixtures/ai-onboarding-sample-scan-mock-fail.json' -SampleIds @('IMG_1484.jpg')"`：按预期返回非零，且继续输出 JSON/Markdown 报告
- `pwsh -NoProfile -Command "& '/home/runner/work/bobbuy/bobbuy/scripts/verify-ai-onboarding-samples.ps1' -MockScanResponsePath '/home/runner/work/bobbuy/bobbuy/docs/fixtures/ai-onboarding-sample-scan-mock-fail.json' -SampleIds @('IMG_1484.jpg') -ReportOnly"`：通过（仅人工报告，`gatePassed=false`）
- `cd frontend && npm audit --json`：已降至 `0 critical / 0 high / 6 moderate`
- `cd backend && mvn -DskipTests package`：通过。
- `cd /home/runner/work/bobbuy/bobbuy && docker build backend -t bobbuy-backend-test`：通过。
- `cd /home/runner/work/bobbuy/bobbuy && docker build frontend -t bobbuy-frontend-test`：通过。
- `cd backend && mvn -Dflyway.url=jdbc:postgresql://localhost:5432/bobbuy -Dflyway.user=bobbuy -Dflyway.password=bobbuypassword -Dflyway.cleanDisabled=false flyway:clean flyway:migrate flyway:validate`：通过。
- PostgreSQL 备份恢复演练：`pg_dump -> bobbuy_restore_verify_plan40` 恢复校验通过。
- `cd /home/runner/work/bobbuy/bobbuy && mvn -pl bobbuy-core,bobbuy-ai,bobbuy-im,bobbuy-auth,bobbuy-gateway -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest='*SmokeTest,*InternalServiceHeaderFilterTest' test`：通过。

### 5.2 当前门禁分层

1. **默认 CI 门禁（每个 PR / `main` push 必跑）**
   - `backend-test`：`cd backend && mvn test`
   - `frontend-quality`：`cd frontend && npm test`、`cd frontend && npm run build`
   - `docker-build`：后端 / 前端镜像构建验证
2. **专用环境门禁（手动 / 条件执行）**
    - `cd backend && mvn -Dflyway.url=jdbc:postgresql://localhost:5432/bobbuy -Dflyway.user=bobbuy -Dflyway.password=bobbuypassword -Dflyway.cleanDisabled=false flyway:clean flyway:migrate flyway:validate`
    - `cd /home/runner/work/bobbuy/bobbuy && mvn -pl bobbuy-core,bobbuy-ai,bobbuy-im,bobbuy-auth,bobbuy-gateway -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest='*SmokeTest,*InternalServiceHeaderFilterTest' test`
    - `cd frontend && npm run e2e`
   - `cd frontend && npm run e2e:ai`
   - `workflow_dispatch`: `.github/workflows/ai-release-evidence.yml`
    - Playwright smoke 当前走 Vite dev server + 浏览器 mock 会话，已覆盖 agent/customer 角色门禁、客户订单/账单/聊天、采购/拣货/库存人工接管；失败 artifact 统一看 `playwright-report/` 与 `test-results/`
3. **风险登记 / 独立安全门禁**
   - CodeQL / 安全扫描
   - 依赖审计
   - 若未执行，必须在 PR / Release 中登记，不得写成已通过
4. **手工运维校验（试运行部署前）**
   - `cd /home/runner/work/bobbuy/bobbuy && docker compose config`
   - 按 `RUNBOOK-监控告警与故障处置` 执行健康检查与日志巡检
   - 按 `RUNBOOK-备份恢复演练` 执行或登记恢复演练结果

### 5.3 当前已知事项

- 前端单测仍存在非阻塞噪声：Ant Design `useForm` warning、预期失败路径的 console error。
- AI 真实视觉链路仍需专用环境，不进入默认 GitHub Hosted CI。
- `frontend npm run e2e` 已恢复为稳定手动门禁，当前结果为 `46 passed / 2 skipped`；`2 skipped` 保留给 `RUN_AI_VISION_E2E` 专用链路。
- 已新增 AI sample golden 基线 `docs/fixtures/ai-onboarding-sample-golden.json`、对比脚本 `scripts/verify-ai-onboarding-samples.ps1`、mock dry-run fixture `docs/fixtures/ai-onboarding-sample-scan-mock.json` / `docs/fixtures/ai-onboarding-sample-scan-mock-fail.json` 与专项报告 `docs/reports/REPORT-03-AI商品字段识别样例验证报告.md`；其中 `basePrice -> price` 字段别名、optional path 漂移、gate/report-only 语义与 `gatePassed` 汇总已修复。
- 发版候选证据包与阻断项处置见 `docs/reports/REPORT-04-发版候选门禁验收报告.md`、`docs/reports/REPORT-05-发版阻断项处置报告.md`。
- 已新增 `docs/reports/REPORT-06-专用环境发版证据与放行判定.md`；当前可对照 `main` 分支默认 CI run `25178072203` 成功，但最终放行结论仍为 `NO_GO`。
- 已更新 `docs/reports/REPORT-07-NO-GO阻断项解阻与放行复判.md`；当前已确认默认 CI 恢复、CodeQL 3 个 high 在 main 上均为 `fixed`、dependency-check artifact 可下载，但最终结论仍为 `NO_GO`。
- `.github/workflows/codeql.yml` 当前已恢复 `push` / `pull_request` / `workflow_dispatch`；最新 main success run 为 <https://github.com/sunshaoxuan/bobbuy/actions/runs/25198280107>，code scanning alerts API 显示 3 个 high 均为 `fixed`。
- `.github/workflows/dependency-check.yml` 已新增；最新可信 `main` artifact 来自 <https://github.com/sunshaoxuan/bobbuy/actions/runs/25198280108>，artifact `dependency-check-report`（id `6744112430`）已验证同时包含 HTML/JSON，摘要为 `8 critical / 21 high / 19 moderate`（unique CVE）。
- 当前源码已把高危依赖解析版本提升到 `tomcat-embed-core 10.1.54`、`netty-transport 4.1.132.Final`、`commons-fileupload 1.6.0`，并已将 pgjdbc 升级到 `42.7.11`；上一轮 GitHub-hosted dependency-check 复扫 run `25215061203` 的 artifact `6749713633` 摘要为 `0 critical / 1 high / 10 medium`，唯一 high 为升级前 `postgresql-42.6.2.jar` / `CVE-2026-42198`；下一轮需归档 pgjdbc 升级后的新复扫结果。
- 已尝试在本沙箱拉起真实 compose 栈执行 AI sample / `e2e:ai` / Flyway 证据；`Dockerfile.service` 的 Maven PKIX 阻塞已解除，但当前剩余阻塞变为 `nacos/nacos-server:v2.3.2-slim` 在本沙箱 cgroup v2 环境下启动时触发 `ProcessorMetrics` 空指针，且仓库内未提供真实旧库 dump。
- 前端依赖审计已清零 `critical/high`，当前剩余 `6 moderate` 均为 Vite/Vitest dev-only 风险，需按 `REPORT-05` / `REPORT-06` 的豁免与升级计划继续跟踪。
- Maven OWASP Dependency-Check 本地复扫仍受 `www.cisa.gov` DNS 解析失败阻塞；仓库内 GitHub-hosted workflow 已能生成可信 HTML/JSON artifact，下一步需要等待新 run 完成并处置余项。
- OAuth/SSO、跨站前后端分离下的更通用浏览器认证方案、mTLS / service mesh 仍未纳入当前交付范围，需在后续安全任务中继续跟进。
- `ai-service`、`im-service`、`auth-service` 已补最小启动 smoke test 与最小 service token 边界，但仍缺少独立 schema、Tracing、SLO、契约测试与拆分后 CI/CD。
- 当前运维能力仍停留在最小基础：日志 / 指标 / 告警 / 备份恢复以 Runbook + 轻量接口 + 手工巡检为主，尚未接入真实监控与自动备份平台。

### 5.4 测试结论

当前代码的后端测试、前端单测、前端构建与 Docker build 默认门禁已恢复为稳定基线；sample 脚本别名漂移已修复并有本地 dry-run 证明；数据库空库初始化与 PostgreSQL 恢复演练已验证；CodeQL 3 个 high 告警已在 main 上标记 fixed，Maven dependency-check artifact 可下载且已记录严重级别摘要；当前分支已完成高危依赖版本覆盖、pgjdbc 升级、Compose 服务镜像 Maven PKIX 解阻、Nacos cgroup v2 启动修复与 OCR 延迟初始化；但文档尚未完全拉平，pgjdbc 升级后的 dependency-check 复扫、真实 AI/OCR 专用环境与真实旧库 adoption 证据仍未补齐，当前结论仍为 `NO_GO`。

---

## 6. 下一阶段优先级

### P0: 文档与测试基线

1. 持续保持默认 CI 门禁（后端测试、前端测试、前端构建、Docker 构建）稳定。
2. 在专用环境执行 Playwright / AI 真实视觉链路，并把结果与 `playwright-report` / `test-results` artifact 链接写入 PR / Release 验证记录。
3. 把 CodeQL / 安全扫描 / 依赖审计继续收敛为可追踪的独立门禁或明确风险登记流程。
4. 后续文档更新必须引用本文件或更新本文件，避免 Release/Plan 漂移。

稳定上线差距的排序与任务拆解见 [PLAN-24: 稳定上线差距收口优先级](../plans/PLAN-24-稳定上线差距收口优先级.md)。

### P1: 试运行收口

1. 给供应商规则、商品删除、AI 上架人工修正确认补测试。
2. 继续补私有 AI gateway / Ollama 的专用部署与观测策略，避免服务器依赖个人 Codex CLI。
3. 持续执行 `.env`、Nacos、backend profile、Docker Compose 的命名一致性审查。
4. 为旧库 adoption、回滚评估与数据修复脚本补专项方案，并补备份恢复演练记录。
5. 在尝试把服务外壳降级为 optional/profile 前，继续补网关路由回归、契约测试与拆分后 CI/CD。

### P2: 生产化

1. 为 OAuth/SSO、跨站前后端分离部署下的更通用浏览器认证方案、mTLS / service mesh、服务间契约测试与更细粒度聊天授权补齐后续方案。
2. 固化旧库 adoption、回滚评估与数据修复脚本。
3. 在当前最小运维基础上继续完善 secret 管理、日志、指标、告警、备份恢复自动化。
4. 接入支付、OAuth、地图路径规划等后续能力。
5. 仅在独立 schema / 数据所有权、服务间鉴权、Tracing、SLO、拆分后 CI/CD 准备完成后，再推进真实微服务拆分。
