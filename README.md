# BOBBuy

BOBBuy 当前是一套以 **Spring Boot + React + PostgreSQL/MinIO + WebSocket(STOMP) 实时聊天 + AI/OCR 辅助识别** 为基础的代购协作系统。

当前实现基线以 [CURRENT-STATE-2026-04-28](docs/reports/CURRENT-STATE-2026-04-28.md) 为准；旧 Release / Plan 文档若与该基线冲突，以当前基线为准。

## 当前真实能力
- **订单 / 行程管理**：行程、订单、批量状态流转、客户侧订单查询。
- **采购 HUD**：利润看板、额外支出、物流追踪、手工对账、客户账单汇总、财务审计流水、伙伴钱包。
- **参与者档案登记**：支持维护电话、邮箱、备注、默认地址与社交账号登记信息。
- **客户账单闭环**：客户可按行程查看 `businessId` 级账单、订单行详情、实采数量、差额说明，并完成“确认收货 / 确认账单”；`COMPLETED` / `SETTLED` 后确认动作自动只读。
- **线下收款与差额结转**：采购端可登记现金 / 转账 / 其他线下收款；当前余额、历史结转余额、本次应收/已收/待收统一仅统计进入结算语义的订单，排除 `NEW` / `CANCELLED` / 无效草稿单。
- **结算冻结治理**：行程进入 `COMPLETED` / `SETTLED` 后，后端与前端统一拦截客户确认、线下收款、小票重新识别/人工复核、拣货确认等会改变财务/履约/确认状态的动作，仅保留查询、导出、审计查看。
- **采购小票核销工作台 V1**：支持上传多张采购小票，保存原图/缩略图/上传时间/处理状态；优先调用真实 AI 识别小票内容，并在 AI 不可用时降级为规则回退结果；展示 AI / RULE_FALLBACK、识别状态、provider/model/stage/attempt trace、置信度、复核状态，并保留人工核销审计。
- **账本精算修正**：历史余额排除取消单、未生效单、未来无效单；线下收款方式后端强校验为 `CASH / BANK_TRANSFER / OTHER`。
- **配送准备与地址清单**：采购 HUD 与客户端账单展示默认地址摘要；支持待配送客户列表与地址 / 经纬度 CSV 导出。
- **拣货确认闭环**：`/procurement` 与 `/picking` 共用 reviewed receipt + picking checklist 单一数据源，按 `businessId` 展示 `PENDING_DELIVERY` / `READY_FOR_DELIVERY`，保留 `SHORT_SHIPPED` / `ON_SITE_REPLENISHED` / `SELF_USE` 标签，并在冻结后统一只读。
- **聊天协作**：聊天已升级为 REST 持久化 + WebSocket(STOMP) 实时推送；客户侧聊天保持“订单上下文优先，Trip 次级筛选”。
- **AI 商品上架**：支持 OCR-first 识别、LLM 结构化、供应商规则、来源治理、既有商品匹配与人工确认；返回 provider/model/stage/latency/error/fallback trace，并在失败时支持人工补录为草稿。
- **数据库迁移治理**：已引入 Flyway；`backend/src/main/resources/db/migration` 提供 PostgreSQL 基线 schema，`backend` / `core-service` 通过 migration 初始化空库，生产/试运行不再依赖 Hibernate `ddl-auto=update`。
- **LLM 兜底**：主文本 LLM 支持 `auto` 路由，优先 Ollama，不可用时可切换到 Codex CLI；服务器生产环境不假定 Codex CLI 可用。

## 当前未实现 / 不宣称
- 消息队列驱动的非聊天业务闭环
- 真实第三方支付网关
- 社交 OAuth 登录
- 真实地图路径规划 / 实时配送追踪
- 无人值守 AI 小票识别
- refresh token / OAuth

## 技术栈
- **Backend**: Spring Boot 3 / Spring Cloud / Nacos / OpenFeign / Resilience4j / Spring Security / Spring Data JPA
- **Frontend**: React 18 / Ant Design / Vite / Vitest / Playwright
- **Storage**: PostgreSQL 15 / MinIO

## 试运行阶段服务边界

- 默认路线采用 **主业务单体优先稳定**，详见 [ADR-01-试运行阶段服务边界决策](docs/architecture/ADR-01-试运行阶段服务边界决策.md)。
- **主业务入口**：`backend`（开发、测试、Flyway migration 源码入口）。
- **事实源**：试运行部署中由 `core-service` 承载核心业务事实源，并独占 Flyway migration。
- **服务外壳**：`ai-service`、`im-service`、`auth-service` 当前继续复用 `backend` 共享代码、共享 PostgreSQL schema 与共享安全配置，不是独立业务事实源。
- **可选服务**：`ocr-service` 属于 AI/OCR 增强能力依赖；不可用时系统按既有 fallback / 人工复核路径降级。
- **后续拆分候选**：`ai-service`、`im-service`、`auth-service`；在服务间鉴权、独立 schema、Tracing、SLO、独立 CI/CD 到位前，不继续深拆。
- 当前默认 Compose 仍保留 `ai-service` / `im-service` / `auth-service`；虽已补最小服务壳 smoke test，但在独立 schema、契约测试与拆分后 CI/CD 到位前，仍不安全降级为 optional/profile。

## 试运行运维入口

- 试运行部署：[`docs/runbooks/RUNBOOK-试运行部署.md`](docs/runbooks/RUNBOOK-试运行部署.md)
- 监控告警与故障处置：[`docs/runbooks/RUNBOOK-监控告警与故障处置.md`](docs/runbooks/RUNBOOK-监控告警与故障处置.md)
- 备份恢复演练：[`docs/runbooks/RUNBOOK-备份恢复演练.md`](docs/runbooks/RUNBOOK-备份恢复演练.md)
- 本次运维基线执行报告：[`docs/reports/REPORT-02-生产运维基础执行报告.md`](docs/reports/REPORT-02-生产运维基础执行报告.md)

## 模块 / 服务定位

| 模块 | 当前定位 | 试运行职责 |
| :-- | :-- | :-- |
| `backend` | 主业务入口 | 核心 Controller / Service / Repository / Flyway / 测试 |
| `bobbuy-common` | 共享依赖层 | 公共依赖、配置、DTO/响应模型 |
| `bobbuy-core` | 核心业务服务外壳 | `core-service` 部署入口，承载共享核心业务事实源 |
| `bobbuy-ai` | AI 服务外壳 | `ai-service` 部署入口，承载 AI/OCR 相关接口但不独立持有业务事实 |
| `bobbuy-im` | IM 服务外壳 | `im-service` 部署入口，承载聊天/WebSocket 相关接口但共享数据库 |
| `bobbuy-auth` | 认证服务外壳 | `auth-service` 部署入口，承载登录/JWT 相关接口但不独立持有身份事实 |
| `bobbuy-gateway` | 路由层 | `gateway-service`，只负责路由与健康检查 |

完整职责矩阵、风险与验证记录见 [REPORT-01-试运行服务边界执行报告](docs/reports/REPORT-01-试运行服务边界执行报告.md)。

## 快速开始

1. 复制部署模板并填写必改项：
   ```bash
   cd /home/runner/work/bobbuy/bobbuy
   cp .env.template .env
   ```
2. 至少修改：
    - `BOBBUY_SECURITY_JWT_SECRET`
    - `BOBBUY_SECURITY_SERVICE_TOKEN`
    - `POSTGRES_PASSWORD`
    - `MINIO_ROOT_PASSWORD`
    - `RABBITMQ_DEFAULT_PASS`
3. 校验并启动：
   ```bash
   cd /home/runner/work/bobbuy/bobbuy
   docker compose config
   docker compose up -d --build
   ```

默认仅绑定到宿主机 `127.0.0.1`：

- Gateway / Frontend: `http://127.0.0.1`
- Gateway API: `http://127.0.0.1/api`
- MinIO Console: `http://127.0.0.1:9001`
- Nacos Console: `http://127.0.0.1:8848/nacos`

详细试运行步骤见 [docs/runbooks/RUNBOOK-试运行部署.md](docs/runbooks/RUNBOOK-试运行部署.md)。

### 配置优先级

1. 容器环境变量（`docker compose` / `.env`）
2. Nacos 配置
3. `application-{profile}.properties`
4. `application.properties`

### 试运行安全默认值

- Compose 服务固定 `SPRING_PROFILES_ACTIVE=prod`。
- `BOBBUY_SECURITY_JWT_SECRET` 必须显式填写，模板不再提供可直接上线的默认值。
- `BOBBUY_SECURITY_HEADER_AUTH_ENABLED` 默认 `false`，公网 / 共享部署不得开启。
- `BOBBUY_SECURITY_SERVICE_TOKEN` 必须显式填写；留空时不会信任内部服务 header，`/internal/**` 也不会放行。
- WebSocket `/ws` 必须通过 STOMP `CONNECT` header `Authorization: Bearer <access-token>` 建立连接；未登录或 token 无效时前端退回既有 REST 刷新路径。
- `BOBBUY_SEED_ENABLED` 默认 `false`；demo 账号仅限本地演示。
- `core-service` 为唯一 Flyway migration 执行者，其余服务固定禁用 Flyway。
- 服务器部署推荐 Ollama / 私有兼容 gateway；不要把个人 Codex CLI 当默认服务器方案。
- `BOBBUY_AI_LLM_MAIN_URL` / `BOBBUY_AI_LLM_EDGE_URL` / `BOBBUY_OCR_URL` 留空时，前端会进入人工接管/重试路径，日志与响应 trace 会显示 `unconfigured`。

### 数据库迁移

- Compose 试运行固定使用 PostgreSQL 15，避免继续承受 PostgreSQL 18 与 Flyway 10.15.2 的兼容性提示风险。
- Flyway migration 目录：`backend/src/main/resources/db/migration`
- 空库初始化（本地 PostgreSQL / Docker Compose `postgres`）：
  ```bash
  cd backend
  mvn -Dflyway.url=jdbc:postgresql://localhost:5432/bobbuy -Dflyway.user=bobbuy -Dflyway.password=bobbuypassword flyway:migrate
  ```
- 现有非空库首次纳管前必须先备份；仅在确认库结构与 `V1__baseline_schema.sql` 对齐后，才可显式设置 `BOBBUY_FLYWAY_BASELINE_ON_MIGRATE=true` 做一次性基线登记。
- Docker / Nacos 短期约定由 `core-service` 负责执行 migration，避免多服务并发迁移竞态。

### 网关路由
- `/api/auth/**` → `auth-service`
- `/api/chat/**` → `im-service`
- `/api/ai/**` → `ai-service`
- `/ws` / `/ws/**` → `im-service`
- 其余 `/api/**` → `core-service`

说明：

- 上述路由目标当前是**服务外壳**，并非已完成独立数据所有权的微服务。
- `auth-service` 通过专门路由承载 `/api/auth/**`；`core-service` 仍是其余业务 API 的默认事实源。

## 当前认证方案

- **登录模型**：后端提供 `POST /api/auth/login` 与 `GET /api/auth/me`，使用用户名/密码登录并返回 HMAC JWT access token。
- **前端登录态**：前端统一保存 Bearer token，并按 `/api/auth/me` 返回的真实角色驱动路由与菜单。
- **本地演示账号**：仅在显式开启 `BOBBUY_SEED_ENABLED=true` 时提供 `agent / agent-pass`、`customer / customer-pass`。
- **兼容策略**：`X-BOBBUY-ROLE` / `X-BOBBUY-USER` 仅在显式开启 `bobbuy.security.header-auth.enabled=true` 时可用，默认仅供 dev/test 过渡。
- **生产要求**：公网部署必须配置 `BOBBUY_SECURITY_JWT_SECRET`，且不得开启 `BOBBUY_SECURITY_HEADER_AUTH_ENABLED=true`。
- **WebSocket 鉴权**：前端 STOMP 客户端通过 `Authorization: Bearer <access-token>` 连接 `/ws`，后端在 STOMP `CONNECT`/`SUBSCRIBE` 阶段校验 JWT，并限制 customer 仅能访问本人订单/行程聊天上下文。
- **前端降级行为**：token 缺失时不建立 WebSocket；token 失效/连接被拒绝时停止重连，继续使用既有 REST 刷新/轮询路径，不清空本地未发送消息。
- **服务间鉴权现状**：gateway-service 会清理外部伪造的 `X-BOBBUY-SERVICE-TOKEN` / `X-BOBBUY-INTERNAL-SERVICE`，并在配置 `BOBBUY_SECURITY_SERVICE_TOKEN` 后向下游附带可信内部身份；后端仅对 `/internal/**` 路径信任该 token，且 service token 不等同于最终用户 JWT。
- **refresh token 取舍**：本阶段继续暂缓；默认 access token TTL 由 `BOBBUY_SECURITY_JWT_TTL_SECONDS` 控制（默认 3600 秒），过期后需重新登录。
- **服务壳 smoke test**：可通过 `cd /home/runner/work/bobbuy/bobbuy && mvn -pl bobbuy-core,bobbuy-ai,bobbuy-im,bobbuy-auth,bobbuy-gateway -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest='*SmokeTest,*InternalServiceHeaderFilterTest' test` 验证五个服务壳最小启动与 gateway header 清理。
- **当前安全边界**：暂未实现 refresh token、第三方 OAuth/SSO、mTLS / service mesh、独立 schema / 数据所有权、契约测试与拆分后独立 CI/CD；旧库升级仍需按 Flyway 基线/备份流程执行。

## 验收门禁

默认门禁（每个 PR / `main` push 必跑）：
- `cd backend && mvn test`
- `cd frontend && npm ci && npm test`
- `cd frontend && npm run build`

AI / OCR 默认测试边界：
- 默认单测只走 fake/mock/H2，不连接真实 Ollama、Codex CLI、OCR service、MinIO。
- 真实 AI/OCR 验收仅在专用环境执行，见 `docs/reports/TEST-MATRIX-本地与CI执行矩阵.md`。
- `cd backend && mvn -DskipTests package && cd /home/runner/work/bobbuy/bobbuy && docker build backend -t bobbuy-backend-test`
- `cd /home/runner/work/bobbuy/bobbuy && docker build frontend -t bobbuy-frontend-test`

专用环境门禁（不进入默认 Hosted CI）：
- `cd backend && mvn -Dflyway.url=jdbc:postgresql://localhost:5432/bobbuy -Dflyway.user=bobbuy -Dflyway.password=bobbuypassword -Dflyway.cleanDisabled=false flyway:clean flyway:migrate flyway:validate`
- `cd /home/runner/work/bobbuy/bobbuy && mvn -pl bobbuy-core,bobbuy-ai,bobbuy-im,bobbuy-auth,bobbuy-gateway -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest='*SmokeTest,*InternalServiceHeaderFilterTest' test`
- `cd frontend && npm run e2e`
- `cd frontend && npm run e2e:ai`

风险登记 / 独立安全门禁：
- CodeQL / 安全扫描
- 依赖审计
- 若本次 PR / Release 未执行，必须明确登记为风险项，不得写成“已通过”。

## 试运行最小运维基线

- 日志：统一使用 `docker compose logs` + `X-Trace-Id` + 应用请求日志排障；关键字段包含 `method/path/status/cost/trace_id/user/role/internal_service`
- 指标：`/api/metrics` 提供轻量 endpoint 请求次数、`p95/p99`、`4xx/5xx`、登录失败次数与全局 `5xx` 比率
- AI / OCR：继续以 trace、`fallbackReason`、`FAILED_RECOGNITION`、`PENDING_MANUAL_REVIEW` 为主，不在本阶段强行引入 Prometheus
- 告警 / 故障处置：统一见 `RUNBOOK-监控告警与故障处置.md`
- 备份恢复：统一见 `RUNBOOK-备份恢复演练.md`；恢复验证必须先在新库 / 独立 bucket / 独立目录完成

最近本地验证：
- `cd /home/runner/work/bobbuy/bobbuy && docker compose config`：通过。
- `cd backend && mvn test`：通过。
- `cd frontend && npm ci && npm test`：通过。
- `cd frontend && npm run build`：通过。
- `cd backend && mvn -DskipTests package`：通过。
- `cd /home/runner/work/bobbuy/bobbuy && docker build backend -t bobbuy-backend-test`：通过。
- `cd /home/runner/work/bobbuy/bobbuy && docker build frontend -t bobbuy-frontend-test`：通过。
- `cd backend && mvn -Dflyway.url=jdbc:postgresql://localhost:5432/bobbuy -Dflyway.user=bobbuy -Dflyway.password=bobbuypassword -Dflyway.cleanDisabled=false flyway:clean flyway:migrate flyway:validate`：通过。

详细矩阵见 [docs/reports/TEST-MATRIX-本地与CI执行矩阵.md](docs/reports/TEST-MATRIX-本地与CI执行矩阵.md)。

当前边界执行摘要见 [docs/reports/REPORT-01-试运行服务边界执行报告.md](docs/reports/REPORT-01-试运行服务边界执行报告.md)。
