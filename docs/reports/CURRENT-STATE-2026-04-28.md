# CURRENT STATE: BOBBuy 当前实现基线

**日期**: 2026-04-28
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
- `/procurement` 与 `/picking` 共用 reviewed receipt + picking checklist 单一事实来源。
- 支持待配送客户列表与地址/经纬度 CSV 导出。

### 2.5 AI 商品上架

- 支持 OCR-first 商品识别、LLM 结构化、Web research、来源治理、既有商品匹配、人工确认。
- 支持供应商档案中的 `onboardingRules`，用于不同商家商品编号/品番规则提示。
- 集中上架页面支持 AI 识别结果人工修正后确认。
- 商品主数据支持多语言名称/描述、品牌、价格、媒体、品番、价格层级。

### 2.6 LLM 与 OCR

- 主文本 LLM provider 支持 `auto`：启动时探测 Ollama `/api/tags`，可用则走 Ollama，不可用则走 Codex CLI 兜底。
- 运行中 Ollama 主节点失败时会切换到 Codex CLI。
- 视觉/图片任务仍走 edge 模型路径。
- Codex CLI 兜底只适合本地或私有 gateway 场景；Linux 服务器容器内默认不可假定可用。

### 2.7 聊天实时化

- 聊天已具备 REST 持久化 + WebSocket/STOMP 推送能力。
- 前端 `useChatWebSocket` 订阅 `/ws`，失败时仍可依赖页面刷新/轮询补偿。
- Docker 形态下 `im-service` 承载聊天与 WebSocket，RabbitMQ STOMP 可作为 broker relay。

---

## 3. 当前架构事实

### 3.1 代码形态

- `backend` 仍是主业务单体，包含主要 Controller、Service、Model、Repository、测试。
- 根级多模块服务已经存在：`bobbuy-core`、`bobbuy-ai`、`bobbuy-im`、`bobbuy-auth`、`bobbuy-gateway`。
- 多模块目前主要通过共享 `com.bobbuy` 包和 `ComponentScan excludeFilters` 切分 Controller，尚未完成真正领域级物理拆分。

### 3.2 部署形态

- Docker Compose 当前包含 PostgreSQL 18、MinIO、Redis、RabbitMQ、Nacos、core-service、ai-service、im-service、auth-service、gateway-service、frontend、nginx gateway、ocr-service。
- Gateway 路由目标：
  - `/api/chat/**`、`/ws/**` -> `im-service`
  - `/api/ai/**` -> `ai-service`
  - 其余 `/api/**` -> `core-service`
- 当前 Compose 更接近集成/试运行部署，不等同于生产高可用方案。

---

## 4. 当前明确边界

1. 不提供真实第三方支付网关。
2. 不提供社交 OAuth / SSO。
3. 不提供真实地图路径规划或实时配送追踪。
4. 不承诺无人值守 AI 自动改账或自动发布商品。
5. 不承诺 Codex CLI 可在 Linux 服务器容器内直接使用。
6. 当前认证机制仍是试运行级 header 角色注入，不适合公网生产。
7. 当前数据库迁移仍依赖 Hibernate `ddl-auto` 路径，未引入 Flyway/Liquibase。

---

## 5. 质量与测试状态

### 5.1 最近验证

- `cd backend && mvn -DskipTests compile`：通过。
- `cd frontend && npm run build`：通过。

### 5.2 当前不健康项

- `cd backend && mvn test` 当前失败：`135 tests, 9 failures, 17 errors`。
- 失败集中在：
  - AI 上架测试仍按旧视觉路径预期，当前代码已改为 OCR-first。
  - `BobbuyStore` / `ProcurementHudService` / `SecurityAuthorizationIntegrationTest` 存在测试数据隔离问题，seed 数据与状态互相污染。
- `cd frontend && npm test` 在当前本地执行超过 3 分钟未完成，需要独立排查或拆分执行。

### 5.3 测试结论

当前代码可以编译和构建，但完整测试门禁未恢复。后续任何生产化结论必须以测试基线修复为前提。

---

## 6. 下一阶段优先级

### P0: 文档与测试基线

1. 修复后端测试数据隔离，让 `mvn test` 全绿。
2. 重写 AI 上架服务测试，匹配 OCR-first + LLM + supplier rules 的当前流程。
3. 拆分或修复前端 `npm test` 超时问题。
4. 后续文档更新必须引用本文件或更新本文件，避免 Release/Plan 漂移。

稳定上线差距的排序与任务拆解见 [PLAN-24: 稳定上线差距收口优先级](../plans/PLAN-24-稳定上线差距收口优先级.md)。

### P1: 试运行收口

1. 给供应商规则、商品删除、AI 上架人工修正确认补测试。
2. 为 Codex fallback 增加 remote gateway 模式文档与配置约束。
3. 收敛 `.env`、Nacos、backend profile、Docker Compose 的 AI 配置命名。
4. 明确单体优先还是微服务优先，避免双轨长期并存。

### P2: 生产化

1. 引入真实认证：JWT/session、用户登录、角色绑定、服务间鉴权。
2. 引入 Flyway/Liquibase。
3. 完善 secret 管理、日志、指标、告警、备份恢复。
4. 接入支付、OAuth、地图路径规划等后续能力。
