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
- **采购小票核销工作台 V1**：支持上传多张采购小票，保存原图/缩略图/上传时间/处理状态；优先调用真实 AI 识别小票内容，并在 AI 不可用时降级为规则回退结果；展示 AI / RULE_FALLBACK、置信度、复核状态，并保留人工核销审计。
- **账本精算修正**：历史余额排除取消单、未生效单、未来无效单；线下收款方式后端强校验为 `CASH / BANK_TRANSFER / OTHER`。
- **配送准备与地址清单**：采购 HUD 与客户端账单展示默认地址摘要；支持待配送客户列表与地址 / 经纬度 CSV 导出。
- **拣货确认闭环**：`/procurement` 与 `/picking` 共用 reviewed receipt + picking checklist 单一数据源，按 `businessId` 展示 `PENDING_DELIVERY` / `READY_FOR_DELIVERY`，保留 `SHORT_SHIPPED` / `ON_SITE_REPLENISHED` / `SELF_USE` 标签，并在冻结后统一只读。
- **聊天协作**：聊天已升级为 REST 持久化 + WebSocket(STOMP) 实时推送；客户侧聊天保持“订单上下文优先，Trip 次级筛选”。
- **AI 商品上架**：支持 OCR-first 识别、LLM 结构化、供应商规则、来源治理、既有商品匹配与人工确认。
- **数据库迁移治理**：已引入 Flyway；`backend/src/main/resources/db/migration` 提供 PostgreSQL 基线 schema，`backend` / `core-service` 通过 migration 初始化空库，生产/试运行不再依赖 Hibernate `ddl-auto=update`。
- **LLM 兜底**：主文本 LLM 支持 `auto` 路由，优先 Ollama，不可用时可切换到 Codex CLI；服务器生产环境不假定 Codex CLI 可用。

## 当前未实现 / 不宣称
- 消息队列驱动的非聊天业务闭环
- 真实第三方支付网关
- 社交 OAuth 登录
- 真实地图路径规划 / 实时配送追踪
- 无人值守 AI 小票识别
- refresh token / OAuth / WebSocket 鉴权（当前仍未实现）

## 技术栈
- **Backend**: Spring Boot 3 / Spring Cloud / Nacos / OpenFeign / Resilience4j / Spring Security / Spring Data JPA
- **Frontend**: React 18 / Ant Design / Vite / Vitest / Playwright
- **Storage**: PostgreSQL 15 / MinIO

## 微服务模块
- `bobbuy-common`：复用现有后端代码与共享 DTO / 响应模型
- `bobbuy-core`：订单、行程、采购、财务等核心业务
- `bobbuy-ai`：AI 解析、翻译、商品引导、收据识别
- `bobbuy-im`：聊天 REST + WebSocket(STOMP)
- `bobbuy-auth`：预留认证服务注册节点
- `bobbuy-gateway`：Spring Cloud Gateway 路由层

## 快速开始
```bash
docker-compose -p bobbuy up -d
```

- Frontend: http://localhost
- Gateway API: http://localhost/api
- MinIO Console: http://localhost:9001
- Nacos Console: http://localhost:8848/nacos

### 数据库迁移
- Flyway migration 目录：`/home/runner/work/bobbuy/bobbuy/backend/src/main/resources/db/migration`
- 空库初始化（本地 PostgreSQL / Docker Compose `postgres`）：
  ```bash
  cd /home/runner/work/bobbuy/bobbuy/backend
  mvn -Dflyway.url=jdbc:postgresql://localhost:5432/bobbuy -Dflyway.user=bobbuy -Dflyway.password=bobbuypassword flyway:migrate
  ```
- 现有非空库首次纳管前必须先备份；仅在确认库结构与 `V1__baseline_schema.sql` 对齐后，才可显式设置 `BOBBUY_FLYWAY_BASELINE_ON_MIGRATE=true` 做一次性基线登记。
- Docker / Nacos 短期约定由 `core-service` 负责执行 migration，避免多服务并发迁移竞态。

### 网关路由
- `/api/chat/**` → `im-service`
- `/api/ai/**` → `ai-service`
- `/ws` / `/ws/**` → `im-service`
- 其余 `/api/**` → `core-service`

## 当前认证方案

- **登录模型**：后端提供 `POST /api/auth/login` 与 `GET /api/auth/me`，使用用户名/密码登录并返回 HMAC JWT access token。
- **前端登录态**：前端统一保存 Bearer token，并按 `/api/auth/me` 返回的真实角色驱动路由与菜单。
- **本地演示账号**：seed 数据默认提供 `agent / agent-pass`、`customer / customer-pass`。
- **兼容策略**：`X-BOBBUY-ROLE` / `X-BOBBUY-USER` 仅在显式开启 `bobbuy.security.header-auth.enabled=true` 时可用，默认仅供 dev/test 过渡。
- **生产要求**：公网部署必须配置 `BOBBUY_SECURITY_JWT_SECRET`，且不得开启 `BOBBUY_SECURITY_HEADER_AUTH_ENABLED=true`。
- **当前安全边界**：暂未实现 refresh token、第三方 OAuth/SSO、WebSocket `/ws` 鉴权；旧库升级仍需按 Flyway 基线/备份流程执行。

## 验收门禁

默认门禁（每个 PR / `main` push 必跑）：
- `cd backend && mvn test`
- `cd frontend && npm test`
- `cd frontend && npm run build`
- `cd backend && mvn -DskipTests package && cd /home/runner/work/bobbuy/bobbuy && docker build backend -t bobbuy-backend-test`
- `cd /home/runner/work/bobbuy/bobbuy && docker build frontend -t bobbuy-frontend-test`

专用环境门禁（不进入默认 Hosted CI）：
- `cd backend && mvn -Dflyway.url=jdbc:postgresql://localhost:5432/bobbuy -Dflyway.user=bobbuy -Dflyway.password=bobbuypassword -Dflyway.cleanDisabled=false flyway:clean flyway:migrate flyway:validate`
- `cd frontend && npm run e2e`
- `cd frontend && npm run e2e:ai`

风险登记 / 独立安全门禁：
- CodeQL / 安全扫描
- 依赖审计
- 若本次 PR / Release 未执行，必须明确登记为风险项，不得写成“已通过”。

最近本地验证：
- `cd backend && mvn test`：通过。
- `cd frontend && npm test`：通过。
- `cd frontend && npm run build`：通过。
- `cd backend && mvn -Dflyway.url=jdbc:postgresql://localhost:5432/bobbuy -Dflyway.user=bobbuy -Dflyway.password=bobbuypassword -Dflyway.cleanDisabled=false flyway:clean flyway:migrate flyway:validate`：通过。

详细矩阵见 [docs/reports/TEST-MATRIX-本地与CI执行矩阵.md](docs/reports/TEST-MATRIX-本地与CI执行矩阵.md)。
