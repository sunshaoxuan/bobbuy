# CURRENT STATE: BOBBuy 当前实现基线

**日期**: 2026-04-28（2026-04-29 部署与配置收口增量更新）
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

- Docker Compose 当前包含 PostgreSQL 15、MinIO、Redis、RabbitMQ、Nacos、core-service、ai-service、im-service、auth-service、gateway-service、frontend、nginx gateway、ocr-service。
- Compose 服务固定 `SPRING_PROFILES_ACTIVE=prod`；配置优先级收敛为：环境变量 > Nacos > `application-{profile}.properties` > `application.properties`。
- 共享 PostgreSQL schema 的 migration 由 `core-service` 独占执行；其余服务通过 Compose + Nacos 保持 `spring.flyway.enabled=false`，避免并发迁移。
- 对宿主机开放的默认端口改为仅绑定 `127.0.0.1`，用于网关、PostgreSQL、MinIO Console/API、Nacos Console、OCR 调试入口。
- Gateway 路由目标：
  - `/api/chat/**`、`/ws/**` -> `im-service`
  - `/api/ai/**` -> `ai-service`
  - 其余 `/api/**` -> `core-service`
- 当前 Compose 更接近集成/试运行部署，不等同于生产高可用方案；试运行操作手册见 `docs/runbooks/RUNBOOK-试运行部署.md`。

---

## 4. 当前明确边界

1. 不提供真实第三方支付网关。
2. 不提供社交 OAuth / SSO。
3. 不提供真实地图路径规划或实时配送追踪。
4. 不承诺无人值守 AI 自动改账或自动发布商品。
5. 不承诺 Codex CLI 可在 Linux 服务器容器内直接使用。
6. 当前单体后端已切换为用户名/密码登录 + HMAC JWT access token；header auth 仅在 dev/test 显式开关开启时保留兼容，Compose / 试运行默认固定为 `false`。
7. WebSocket `/ws` 当前仍保持 permitAll，聊天鉴权需后续任务补齐。
8. 已引入 Flyway 基线 migration；`backend` / `core-service` 负责 PostgreSQL schema 初始化，旧库 adoption 仍需备份与一次性 baseline 评估。

---

## 5. 质量与测试状态

### 5.1 最近验证

- `cd backend && mvn test`：通过。
- `cd frontend && npm test`：通过。
- `cd frontend && npm run build`：通过。
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

### 5.3 当前已知事项

- 前端单测仍存在非阻塞噪声：Ant Design `useForm` warning、预期失败路径的 console error。
- AI 真实视觉链路仍需专用环境，不进入默认 GitHub Hosted CI。
- 安全扫描与依赖审计尚未固化到默认 CI，当前仍按独立执行 / 风险登记处理。
- WebSocket 聊天鉴权、refresh token 与服务间鉴权尚未纳入当前交付范围，需在后续安全任务中继续跟进。

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

### P2: 生产化

1. 为 WebSocket、服务间调用与 refresh token 补齐后续鉴权方案。
2. 固化旧库 adoption、回滚评估与数据修复脚本。
3. 完善 secret 管理、日志、指标、告警、备份恢复。
4. 接入支付、OAuth、地图路径规划等后续能力。
