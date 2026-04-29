# PLAN-31: P1 AI 与 OCR 可靠性治理开发提示词

**生效日期**: 2026-04-29
**状态**: 待评审
**关联计划**: [PLAN-24: 稳定上线差距收口优先级](PLAN-24-稳定上线差距收口优先级.md)
**前置任务**:
- [PLAN-28: P1 认证与权限生产化开发提示词](PLAN-28-P1认证与权限生产化开发提示词.md)
- [PLAN-29: P1 数据库迁移治理开发提示词](PLAN-29-P1数据库迁移治理开发提示词.md)
- [PLAN-30: P1 部署与配置收口开发提示词](PLAN-30-P1部署与配置收口开发提示词.md)
**事实基线**: [CURRENT-STATE-2026-04-28](../reports/CURRENT-STATE-2026-04-28.md)

---

## 1. 开发目标

让商品 AI 上架、小票 OCR 识别与采购核销链路具备可观测、可重试、可人工接管的能力，并确保 AI/OCR 不可用时不破坏订单、账单、采购主链路。

当前事实：

1. 默认测试已明确关闭真实 AI/OCR/Codex/MinIO 外连。
2. 服务器部署默认不依赖个人 Codex CLI，推荐 Ollama、OpenAI 兼容 API 或私有 gateway。
3. AI 上架链路已转向 OCR-first，并包含 supplier rules、source governance、语义比对等分支。
4. 小票识别链路已有 fallback，但 provider、模型、耗时、错误码、fallback 原因和人工复核状态尚未形成统一追踪口径。
5. 内部试运行需要用户在 AI 失败时看到可操作状态，而不是静默失败或误发布。

---

## 2. 核心 Prompt 文本（请审查）

您可以直接将以下内容作为下一次开发任务的输入：

```text
### [任务: P1 AI/OCR 可靠性治理]

任务背景：
BOBBuy 已完成测试门禁、JWT 认证、Flyway 迁移治理、部署配置收口。下一步需要把 AI 商品上架、小票 OCR 识别、采购核销中的 AI/OCR 链路治理到试运行可接受状态：失败可见、结果可追溯、可重试、可人工接管，且 AI/OCR 故障不能破坏订单与账单主链路。

目标：
1. 统一记录 AI/OCR 调用轨迹：provider、active provider、model、stage、latencyMs、errorCode、fallbackReason、retryCount、inputRef、outputRef、createdAt、updatedAt。
2. 明确 AI/OCR 业务状态：识别失败、来源治理失败、人工待复核、可发布、已确认、已丢弃。
3. AI/OCR 不可用时，前端显示可操作状态，并允许人工编辑、保存、复核或重试。
4. 小票识别与商品上架结果可追溯到原始输入、provider 与人工处理记录。
5. 默认单测继续使用 fake/mock，不连接真实 Ollama、Codex、OCR service、MinIO。

必须先做的排查：
1. 阅读 AI provider 与 OCR 链路：
   - `backend/src/main/java/com/bobbuy/service/LlmGateway.java`
   - `backend/src/main/java/com/bobbuy/service/AiProductOnboardingService.java`
   - `backend/src/main/java/com/bobbuy/service/ProcurementReceiptRecognitionService.java`
   - `backend/src/main/java/com/bobbuy/service/ImageStorageService.java`
2. 阅读采购与核销链路：
   - `backend/src/main/java/com/bobbuy/service/ProcurementHudService.java`
   - `backend/src/main/java/com/bobbuy/controller/ProcurementController.java`
   - 相关 receipt / reconciliation / ledger 模型与 DTO
3. 阅读 AI 入口：
   - `backend/src/main/java/com/bobbuy/controller/AiAgentController.java`
   - `backend/src/main/java/com/bobbuy/controller/AiInternalController.java`
4. 阅读前端交互：
   - `frontend/src/components/AiQuickAddModal.tsx`
   - `frontend/src/pages/StockMaster.tsx`
   - `frontend/src/pages/ProcurementDashboard.tsx`
   - 如存在 chat publish / AI publish 入口，也一并检查
5. 阅读测试与文档：
   - `backend/src/test/java/com/bobbuy/api/AiProductOnboardingServiceTest.java`
   - 采购 OCR / HUD / reconciliation 相关后端测试
   - `frontend/src/**/*.test.tsx`
   - `docs/reports/TEST-MATRIX-本地与CI执行矩阵.md`
   - `docs/runbooks/RUNBOOK-试运行部署.md`

修复范围 A：AI/OCR 调用轨迹
1. 为商品上架和小票识别统一沉淀 trace 信息，至少包含：
   - provider / activeProvider
   - model
   - stage，例如 OCR、LLM_STRUCTURING、SOURCE_GOVERNANCE、SUPPLIER_RULES、SEMANTIC_COMPARE、FALLBACK、MANUAL_REVIEW
   - latencyMs
   - errorCode
   - errorMessage 摘要，避免存储敏感原文
   - fallbackReason
   - retryCount / attemptNo
   - inputRef，例如 image object key、receipt id、onboarding request id
   - outputRef，例如 product id、receipt recognition id、manual review id
2. 优先复用现有 JSON 字段或业务实体中的 trace/raw result 字段；只有现有模型无法表达时才新增表。
3. 如果新增表或字段，必须补 Flyway migration，并更新实体/DTO/测试。
4. 日志要能定位一次 AI/OCR 调用，但不要把完整图片、完整票据、token、secret 写进日志。

修复范围 B：状态口径
1. 梳理并统一状态命名，建议覆盖：
   - `PENDING`
   - `RECOGNIZED`
   - `FAILED_RECOGNITION`
   - `FAILED_SOURCE_GOVERNANCE`
   - `PENDING_MANUAL_REVIEW`
   - `REVIEWED`
   - `PUBLISHABLE`
   - `CONFIRMED`
   - `DISCARDED`
2. 可以使用现有状态名，但必须能区分：
   - OCR 调用失败
   - LLM 结构化失败
   - provider 未配置
   - provider 超时
   - source governance 拒绝
   - fallback 已使用
   - 人工待复核
   - 人工已确认
3. 不得把低置信度、来源不合规或结构化失败的结果标为可自动发布。

修复范围 C：用户可见行为
1. 商品 AI 上架：
   - OCR/LLM/source governance 失败时，前端显示原因摘要与下一步动作。
   - 支持人工编辑后保存为草稿或继续上架。
   - 低置信度结果必须明显标记，不得静默自动发布。
2. 小票 OCR：
   - 识别失败时保留原始输入引用。
   - 用户可进入人工复核或重新识别。
   - fallback 结果必须显示为 fallback / 待确认，而不是等同真实识别成功。
3. 采购核销：
   - AI 失败不得阻断订单、账单、钱包等主链路读取。
   - 未确认识别结果不得自动改账。

修复范围 D：重试与幂等
1. 同一图片、同一小票或同一 AI 上架请求重试时，不得重复创建正式商品、订单、账本记录。
2. 重试应保留历史 attempt trace，至少能看到最后一次失败原因和当前有效 attempt。
3. 若当前业务模型只支持单次结果，先实现“覆盖当前结果 + 保留摘要历史”的轻量方案，并在文档登记后续增强。

修复范围 E：配置与私有 gateway 文档
1. 更新 AI provider 文档：
   - Ollama
   - OpenAI API 或兼容 API
   - 私有 Codex gateway
   - 本地 Codex CLI
2. 明确生产默认不依赖 Windows 桌面 Codex 登录态。
3. 私有 Codex gateway 如未实现，只写清接口期望、网络边界和风险，不得宣称已可用。
4. provider 未配置时，用户状态与日志都要显示 `unconfigured` 或等价含义。

修复范围 F：测试
1. 后端至少覆盖：
   - provider 未配置
   - OCR 调用失败
   - LLM 结构化失败
   - source governance 拒绝
   - fallback 被使用并记录原因
   - 人工复核接管
   - 重试不重复创建正式业务数据
2. 前端至少覆盖：
   - AI/OCR 失败显示可操作状态
   - fallback / 待复核状态展示
   - 人工编辑后保存
   - 重试按钮或重新识别入口
3. 默认测试仍必须 mock AI/OCR，不得依赖真实外部服务。

修复范围 G：文档同步
1. 更新：
   - `README.md`
   - `docs/reports/CURRENT-STATE-2026-04-28.md`
   - `docs/reports/TEST-MATRIX-本地与CI执行矩阵.md`
   - `docs/runbooks/RUNBOOK-试运行部署.md`
   - 如配置有变化，同步 `.env.template`
2. 明确：
   - 默认单测边界
   - 真实 AI/OCR 验收如何开启
   - AI 失败时的人工处理流程
   - provider 追踪字段与排障路径

验收命令：
1. `cd backend && mvn test`
2. `cd frontend && npm test`
3. `cd frontend && npm run build`
4. 如修改配置或 Compose：`docker compose config`
5. 如新增 Flyway migration：使用本地 PostgreSQL 执行 `flyway:migrate flyway:validate` 或说明未执行原因

交付要求：
1. 提供 AI/OCR 可靠性治理摘要：
   - 新增状态
   - trace 字段
   - fallback 策略
   - 人工接管流程
2. 提供修改清单：
   - 后端代码
   - 前端代码
   - migration
   - 测试
   - 文档
3. 提供验证结果。
4. 登记仍未完成的边界：
   - 完整监控告警
   - AI 成功率长期指标
   - 多 attempt 历史表
   - 私有 Codex gateway 生产实现

禁止事项：
1. 不得把真实 AI/OCR 外连放进默认单测。
2. 不得在 AI 失败、低置信度或来源治理失败时自动发布商品。
3. 不得让 AI/OCR 失败破坏订单、账单、钱包主链路。
4. 不得把个人 Codex CLI 暴露为服务器默认方案。
5. 不得把完整图片、secret、token 或敏感票据内容写入日志。
6. 不得回滚非本任务相关的用户改动。

建议实现顺序：
1. 先画清当前 AI/OCR 调用路径和状态流。
2. 设计最小 trace DTO/字段，优先复用现有 JSON 结果字段。
3. 后端先补 trace、状态、失败分类与测试。
4. 前端补失败态、待复核态、人工编辑与重试入口。
5. 同步 README、CURRENT STATE、TEST MATRIX、Runbook。
6. 跑默认门禁，最后再评估是否需要真实 AI/OCR 专项验收。
```

---

## 3. 审核重点

请重点审核以下问题：

1. Trace 数据先复用现有 JSON 字段，还是立即新增独立表。
2. 状态命名是否沿用现有业务状态，还是引入统一 AI/OCR 状态枚举。
3. 重试历史是否本阶段只保留最后一次摘要，还是完整保留 attempts。
4. 私有 Codex gateway 是否只写文档边界，还是同步实现最小 HTTP provider。
5. 可观测性是否先以结构化日志和响应 trace 为主，监控告警留到 P2。

---

## 4. 预期交付物

1. AI/OCR trace 字段、状态口径和 fallback 策略。
2. 商品上架与小票识别失败时的用户可见处理路径。
3. 人工复核或人工编辑接管流程。
4. 后端与前端自动化测试。
5. README / CURRENT STATE / TEST-MATRIX / Runbook 同步。
6. 一份简短执行报告，包含验证命令、未完成边界和后续建议。

---

## 5. 核准状态

**当前状态**: 待用户审查批准后执行。
