# CURRENT STATE: BOBBuy 当前实现基线

**日期**: 2026-04-28（2026-04-29 部署、配置、微服务边界与 WebSocket 鉴权增量更新）
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
- 商品主数据支持多语言名称/描述、品牌、价格、媒体、品番、价格层级。

### 2.6 LLM 与 OCR

- 主文本 LLM provider 支持 `auto`：启动时探测 Ollama `/api/tags`，可用则走 Ollama，不可用则走 Codex CLI 兜底。
- 运行中 Ollama 主节点失败时会切换到 Codex CLI。
- 视觉/图片任务仍走 edge 模型路径。
- Codex CLI 兜底只适合本地或私有 gateway 场景；Linux 服务器容器内默认不可假定可用。
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
| `core-service` | 部分（复用共享 `backend` 代码） | 是 | 是 | 是 | 是 | 否 | 订单、行程、采购、账单、钱包、审计等核心业务事实源 |
| `ai-service` | 部分（复用共享 `backend` 代码） | 是 | 是 | 否 | 是 | 否 | AI/OCR 服务外壳，不独立持有核心业务事实 |
| `im-service` | 部分（复用共享 `backend` 代码） | 是 | 是 | 否 | 是 | 否 | 聊天 / WebSocket 服务外壳，共享数据库与 RabbitMQ |
| `auth-service` | 部分（复用共享 `backend` 代码） | 是 | 是 | 否 | 是 | 否 | 登录 / JWT 服务外壳，不独立持有身份事实 |
| `gateway-service` | 否（路由层） | 是 | 否 | 否 | 否 | 否 | Spring Cloud Gateway 路由与健康检查 |

> 注：`im-service` 当前只有共享代码复用，没有模块级独立测试；默认测试仍以 `backend` 与前端门禁为主。

### 3.3 部署形态

- Docker Compose 当前包含 PostgreSQL 15、MinIO、Redis、RabbitMQ、Nacos、core-service、ai-service、im-service、auth-service、gateway-service、frontend、nginx gateway、ocr-service。
- Compose 服务固定 `SPRING_PROFILES_ACTIVE=prod`；配置优先级收敛为：环境变量 > Nacos > `application-{profile}.properties` > `application.properties`。
- 共享 PostgreSQL schema 的 migration 由 `core-service` 独占执行；其余服务通过 Compose + Nacos 保持 `spring.flyway.enabled=false`，避免并发迁移。
- 对宿主机开放的默认端口改为仅绑定 `127.0.0.1`，用于网关、PostgreSQL、MinIO Console/API、Nacos Console、OCR 调试入口。
- Gateway 路由目标：
  - `/api/auth/**` -> `auth-service`
  - `/api/chat/**`、`/ws/**` -> `im-service`
  - `/api/ai/**` -> `ai-service`
  - 其余 `/api/**` -> `core-service`
- 当前 Compose 更接近集成/试运行部署，不等同于生产高可用方案；虽然默认会启动多个服务外壳，但这不等于完成独立微服务事实源拆分。
- 在网关路由与服务壳启动测试补齐前，当前不把 `ai-service` / `im-service` / `auth-service` 安全降级为 optional/profile。

### 3.4 最小运维基础

- 已新增 [`RUNBOOK-监控告警与故障处置`](../runbooks/RUNBOOK-监控告警与故障处置.md)，统一日志入口、健康检查、关键指标、告警阈值与故障处置清单。
- 已新增 [`RUNBOOK-备份恢复演练`](../runbooks/RUNBOOK-备份恢复演练.md)，覆盖 PostgreSQL、MinIO、Nacos 配置与 `.env` 边界。
- `RequestLoggingInterceptor` 当前会记录 `method/path/status/cost/trace_id/user/role`，并回写 `X-Trace-Id` 响应头。
- `/api/metrics` 当前提供轻量 endpoint 请求次数、`p95/p99`、`4xx/5xx`、登录失败次数、全局 `5xx` 比率；AI/OCR、RabbitMQ、Redis、Nacos 仍以日志 + 人工巡检为主。
- 当前仍未接入 Prometheus/Grafana、集中日志平台、自动化备份任务与服务级 SLO。

---

## 4. 当前明确边界

1. 不提供真实第三方支付网关。
2. 不提供社交 OAuth / SSO。
3. 不提供真实地图路径规划或实时配送追踪。
4. 不承诺无人值守 AI 自动改账或自动发布商品。
5. 不承诺 Codex CLI 可在 Linux 服务器容器内直接使用。
6. 当前单体后端已切换为用户名/密码登录 + HMAC JWT access token；header auth 仅在 dev/test 显式开关开启时保留兼容，Compose / 试运行默认固定为 `false`。
7. WebSocket 握手仍允许匿名进入 `/ws`，但 STOMP `CONNECT`/`SUBSCRIBE` 已要求 Bearer access token；未登录、过期或伪造 token 会在消息通道阶段被拒绝。
8. refresh token 本阶段继续暂缓；默认 access token TTL 为 `BOBBUY_SECURITY_JWT_TTL_SECONDS`（默认 3600 秒），过期后需重新登录。
9. 服务间鉴权仍未独立落地；`core-service` / `ai-service` / `im-service` / `auth-service` 仍依赖内网边界与共享 JWT 配置，未完成 service token / mTLS，不得据此继续真实微服务深拆。
10. 已引入 Flyway 基线 migration；`backend` 提供 migration 源码，`core-service` 负责试运行部署中的 PostgreSQL schema 初始化，旧库 adoption 仍需备份与一次性 baseline 评估。

---

## 5. 质量与测试状态

### 5.1 最近验证

- `cd /home/runner/work/bobbuy/bobbuy && docker compose config`：通过。
- `cd backend && mvn test`：通过。
- `cd frontend && npm ci && npm test`：通过。
- `cd frontend && npm run build`：通过。
- `cd backend && mvn -DskipTests package`：通过。
- `cd /home/runner/work/bobbuy/bobbuy && docker build backend -t bobbuy-backend-test`：通过。
- `cd /home/runner/work/bobbuy/bobbuy && docker build frontend -t bobbuy-frontend-test`：通过。
- `cd backend && mvn -Dflyway.url=jdbc:postgresql://localhost:5432/bobbuy -Dflyway.user=bobbuy -Dflyway.password=bobbuypassword -Dflyway.cleanDisabled=false flyway:clean flyway:migrate flyway:validate`：通过。

### 5.2 当前门禁分层

1. **默认 CI 门禁（每个 PR / `main` push 必跑）**
   - `backend-test`：`cd backend && mvn test`
   - `frontend-quality`：`cd frontend && npm test`、`cd frontend && npm run build`
   - `docker-build`：后端 / 前端镜像构建验证
2. **专用环境门禁（手动 / 条件执行）**
   - `cd backend && mvn -Dflyway.url=jdbc:postgresql://localhost:5432/bobbuy -Dflyway.user=bobbuy -Dflyway.password=bobbuypassword -Dflyway.cleanDisabled=false flyway:clean flyway:migrate flyway:validate`
   - `cd frontend && npm run e2e`
   - `cd frontend && npm run e2e:ai`
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
- 安全扫描与依赖审计尚未固化到默认 CI，当前仍按独立执行 / 风险登记处理。
- refresh token 与独立服务间鉴权仍未纳入当前交付范围，需在后续安全任务中继续跟进。
- `ai-service`、`im-service`、`auth-service` 仍缺少独立启动测试、服务间鉴权、独立 schema、Tracing、SLO 与拆分后 CI/CD。
- 当前运维能力仍停留在最小基础：日志 / 指标 / 告警 / 备份恢复以 Runbook + 轻量接口 + 手工巡检为主，尚未接入真实监控与自动备份平台。

### 5.4 测试结论

当前代码的后端测试、前端单测、前端构建已恢复为稳定默认门禁；数据库空库初始化改由 Flyway 基线 migration 验证；上线判断需要同时参考默认门禁、专用环境门禁与风险登记项。

---

## 6. 下一阶段优先级

### P0: 文档与测试基线

1. 持续保持默认 CI 门禁（后端测试、前端测试、前端构建、Docker 构建）稳定。
2. 在专用环境执行 Playwright / AI 真实视觉链路，并把结果写入 PR / Release 验证记录。
3. 把 CodeQL / 安全扫描 / 依赖审计继续收敛为可追踪的独立门禁或明确风险登记流程。
4. 后续文档更新必须引用本文件或更新本文件，避免 Release/Plan 漂移。

稳定上线差距的排序与任务拆解见 [PLAN-24: 稳定上线差距收口优先级](../plans/PLAN-24-稳定上线差距收口优先级.md)。

### P1: 试运行收口

1. 给供应商规则、商品删除、AI 上架人工修正确认补测试。
2. 继续补私有 AI gateway / Ollama 的专用部署与观测策略，避免服务器依赖个人 Codex CLI。
3. 持续执行 `.env`、Nacos、backend profile、Docker Compose 的命名一致性审查。
4. 为旧库 adoption、回滚评估与数据修复脚本补专项方案，并补备份恢复演练记录。
5. 在尝试把服务外壳降级为 optional/profile 前，先补服务壳 smoke test 与网关路由回归。

### P2: 生产化

1. 为 refresh token、独立服务间 service token / mTLS 与更细粒度聊天授权补齐后续鉴权方案。
2. 固化旧库 adoption、回滚评估与数据修复脚本。
3. 在当前最小运维基础上继续完善 secret 管理、日志、指标、告警、备份恢复自动化。
4. 接入支付、OAuth、地图路径规划等后续能力。
5. 仅在独立 schema / 数据所有权、服务间鉴权、Tracing、SLO、拆分后 CI/CD 准备完成后，再推进真实微服务拆分。
