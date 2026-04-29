# PLAN-33: P2 生产运维基础开发提示词

**生效日期**: 2026-04-29
**状态**: 待评审
**关联计划**: [PLAN-24: 稳定上线差距收口优先级](PLAN-24-稳定上线差距收口优先级.md)
**前置任务**:
- [PLAN-30: P1 部署与配置收口开发提示词](PLAN-30-P1部署与配置收口开发提示词.md)
- [PLAN-31: P1 AI 与 OCR 可靠性治理开发提示词](PLAN-31-P1-AI与OCR可靠性治理开发提示词.md)
- [PLAN-32: P2 微服务边界决策开发提示词](PLAN-32-P2-微服务边界决策开发提示词.md)
**事实基线**: [CURRENT-STATE-2026-04-28](../reports/CURRENT-STATE-2026-04-28.md)

---

## 1. 开发目标

补齐内部/小范围试运行所需的最小生产运维基础：日志检索、健康检查、关键指标、告警规则、备份恢复演练与故障处置 Runbook。目标是让试运行故障可发现、可定位、可恢复，而不是一次性引入完整平台化运维体系。

当前事实：

1. 默认上线门禁已经恢复：后端测试、前端测试、前端构建与 Compose 渲染均可稳定执行。
2. 试运行阶段已通过 ADR 固定为“主业务单体优先稳定”：`backend` 是源码入口，`core-service` 是部署事实源。
3. Compose 已包含 PostgreSQL、MinIO、Redis、RabbitMQ、Nacos、core/ai/im/auth/gateway 服务外壳、frontend、nginx gateway、ocr-service。
4. AI/OCR 已具备 trace 与人工接管，但缺少长期成功率指标和告警。
5. 当前仍缺少备份恢复演练记录、告警阈值、最小仪表盘口径和故障处置清单。

---

## 2. 核心 Prompt 文本（请审查）

您可以直接将以下内容作为下一次开发任务的输入：

```text
### [任务: P2 生产运维基础]

任务背景：
BOBBuy 已完成测试基线、认证、Flyway、部署配置、AI/OCR 可靠性和试运行服务边界决策。下一步需要补齐内部/小范围真实试运行前的最小运维基础：日志、指标、告警、备份恢复、故障处置与演练记录。任务目标是可运行、可审计、可恢复，不追求一次性引入复杂平台。

目标：
1. 明确试运行环境的日志查看路径、关键日志字段和排障流程。
2. 明确健康检查、关键指标与告警阈值。
3. 补齐 PostgreSQL、MinIO、Nacos 配置的备份与恢复演练 Runbook。
4. 明确 AI/OCR、登录、订单/账单、数据库、MinIO、RabbitMQ、Nacos、Gateway 的故障处置清单。
5. 提供一次可复现的备份恢复演练记录模板；如条件允许，完成本地演练并记录结果。

必须先做的排查：
1. 阅读当前部署和边界文档：
   - `docs/runbooks/RUNBOOK-试运行部署.md`
   - `docs/architecture/ADR-01-试运行阶段服务边界决策.md`
   - `docs/reports/CURRENT-STATE-2026-04-28.md`
   - `docs/reports/TEST-MATRIX-本地与CI执行矩阵.md`
   - `docker-compose.yml`
   - `.env.template`
2. 阅读健康检查和指标相关代码：
   - `backend/src/main/java/com/bobbuy/api/HealthController.java`
   - `backend/src/main/java/com/bobbuy/api/MetricsController.java`
   - `backend/src/main/java/com/bobbuy/config/RequestLoggingInterceptor.java`
   - `backend/src/main/resources/application*.properties`
3. 阅读关键链路：
   - 认证：`AuthController`、JWT/security 配置
   - AI/OCR：`LlmGateway`、`AiProductOnboardingService`、`ProcurementReceiptRecognitionService`
   - 采购/账单/钱包：`ProcurementHudService`、账单/ledger 相关模型与服务
4. 阅读基础设施配置：
   - `infra/nginx/prod.conf`
   - `infra/nacos/config/*.yaml`
   - RabbitMQ、Redis、MinIO、PostgreSQL 在 Compose 中的卷和端口配置

修复范围 A：最小日志与排障口径
1. 明确每个服务的日志查看命令：
   - `docker compose logs -f gateway`
   - `docker compose logs -f gateway-service`
   - `docker compose logs -f core-service`
   - `docker compose logs -f ai-service`
   - `docker compose logs -f im-service`
   - `docker compose logs -f auth-service`
   - `docker compose logs -f postgres minio rabbitmq nacos redis ocr-service`
2. 文档化关键日志字段：
   - request path / method / status / latency
   - authenticated user / role（不得记录 token）
   - AI provider / activeProvider / model / stage / errorCode / fallbackReason
   - trace id 或等价 request correlation 字段
3. 如代码中已有请求日志但缺少必要字段，可以做小范围补齐；不得引入大规模日志框架重构。
4. 不得把 JWT、密码、MinIO secret、完整小票图片或敏感票据原文写入日志。

修复范围 B：健康检查与关键指标
1. 梳理并文档化现有健康检查：
   - gateway nginx
   - gateway-service
   - core-service
   - ai-service
   - im-service
   - auth-service
   - postgres
   - minio
   - rabbitmq
   - nacos
   - redis
   - ocr-service
2. 明确最小指标集：
   - HTTP 5xx 数量 / 比率
   - 登录失败次数
   - 订单创建/更新失败
   - 账单/钱包关键操作失败
   - AI/OCR 成功率、fallback 次数、人工复核待处理数
   - PostgreSQL 连接失败
   - MinIO 上传失败
   - RabbitMQ 连接失败
   - WebSocket 连接/断开异常
3. 如现有 `MetricsController` 可承载轻量指标，补充必要字段；如不适合，先以 Runbook 和日志查询口径交付，不强行上 Prometheus。

修复范围 C：告警规则
1. 新增或更新告警文档，例如：
   - `docs/runbooks/RUNBOOK-监控告警与故障处置.md`
2. 至少定义：
   - 服务不可用：readiness 连续失败
   - 登录异常：401/403 或登录失败突增
   - 业务错误：订单/账单/钱包 5xx
   - AI/OCR 降级：fallback 激增、`unconfigured`、`FAILED_RECOGNITION`
   - 数据库异常：连接失败、migration 失败
   - MinIO 异常：上传/预览失败
   - RabbitMQ 异常：连接失败、消息堆积（如当前无法采集，先写人工检查）
3. 每条告警必须包含：
   - 触发条件
   - 影响范围
   - 首查命令
   - 缓解动作
   - 升级条件

修复范围 D：备份与恢复
1. 新增或更新备份恢复 Runbook，例如：
   - `docs/runbooks/RUNBOOK-备份恢复演练.md`
2. 覆盖：
   - PostgreSQL 备份：`pg_dump`
   - PostgreSQL 恢复：新库恢复验证，不覆盖线上库
   - MinIO 数据备份：对象数据目录或 `mc mirror` 策略
   - Nacos 配置备份：`infra/nacos/config` 与运行态配置导出策略
   - `.env` / secret 备份边界：不得提交真实 secret
3. 明确恢复验收：
   - 服务可启动
   - 登录可用
   - 订单/账单核心数据可读
   - 小票/商品图片可预览
4. 如本地条件允许，执行一次备份恢复演练并新增记录；如不能执行，提供可执行命令和未执行原因。

修复范围 E：故障处置清单
1. 至少覆盖：
   - 服务启动失败
   - core-service migration 失败
   - JWT secret 缺失
   - header auth 被误开
   - PostgreSQL 不可用
   - MinIO 不可用
   - RabbitMQ 不可用
   - Nacos 不可用
   - AI/OCR 不可用
   - WebSocket 聊天不可用
   - 前端空白页或 API 401/403
2. 每个故障项包含：
   - 现象
   - 首查命令
   - 常见原因
   - 缓解步骤
   - 回滚/恢复步骤
   - 事后记录要求

修复范围 F：文档同步
1. 更新：
   - `README.md`
   - `docs/reports/CURRENT-STATE-2026-04-28.md`
   - `docs/runbooks/RUNBOOK-试运行部署.md`
   - `docs/reports/TEST-MATRIX-本地与CI执行矩阵.md`
   - `docs/plans/PLAN-24-稳定上线差距收口优先级.md`
2. 如新增 Runbook，需要在 README 或 Runbook 索引处可被发现。

验收命令：
1. `docker compose config`
2. `cd backend && mvn test`
3. `cd frontend && npm test`
4. `cd frontend && npm run build`
5. 如果改动 Docker / Compose / Nacos 配置，补充相关 build 或配置渲染验证。
6. 如果执行备份恢复演练，记录实际命令、环境和结果。

交付要求：
1. 提供运维基础摘要：
   - 日志入口
   - 指标口径
   - 告警规则
   - 备份恢复策略
   - 故障处置清单
2. 提供修改清单。
3. 提供验证结果。
4. 登记仍未完成的边界：
   - 真实告警平台接入
   - Prometheus/Grafana 或云监控
   - 集中日志系统
   - 自动化备份任务
   - 定期恢复演练
   - 服务级 SLO

禁止事项：
1. 不得提交真实 `.env`、JWT secret、数据库密码、MinIO secret。
2. 不得在未验证的情况下宣称已完成恢复演练。
3. 不得把备份恢复命令设计成默认覆盖线上数据。
4. 不得引入重型运维平台导致默认测试/部署门禁失稳。
5. 不得回滚非本任务相关的用户改动。

建议实现顺序：
1. 先盘点现有健康检查、日志、卷和数据目录。
2. 写监控告警与故障处置 Runbook。
3. 写备份恢复 Runbook 与演练记录模板。
4. 如必要，小范围补 MetricsController 或请求日志字段。
5. 同步 README、CURRENT STATE、TEST MATRIX、PLAN-24。
6. 跑默认门禁并提交执行报告。
```

---

## 3. 审核重点

请重点审核以下问题：

1. 本阶段是否只做最小运维基础，不引入 Prometheus/Grafana/ELK。
2. 是否要求实际执行一次本地 PostgreSQL 备份恢复演练。
3. AI/OCR 成功率是否先通过日志/轻量指标统计，还是立即持久化指标。
4. RabbitMQ / Redis / Nacos 是否只做人工检查 Runbook，还是接入自动指标。
5. 是否把 WebSocket 鉴权继续登记为安全任务，而不是放进本运维任务。

---

## 4. 预期交付物

1. 监控告警与故障处置 Runbook。
2. 备份恢复 Runbook 与演练记录模板。
3. README / CURRENT STATE / TEST-MATRIX / RUNBOOK / PLAN-24 同步。
4. 如需要，轻量补充 metrics 或日志字段。
5. 一份简短执行报告，包含验证命令、演练结果和未完成运维边界。

---

## 5. 核准状态

**当前状态**: 待用户审查批准后执行。
