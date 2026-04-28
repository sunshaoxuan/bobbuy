# PLAN-25: P0 后端测试基线恢复开发提示词

**生效日期**: 2026-04-28
**状态**: 待评审
**关联计划**: [PLAN-24: 稳定上线差距收口优先级](PLAN-24-稳定上线差距收口优先级.md)
**事实基线**: [CURRENT-STATE-2026-04-28](../reports/CURRENT-STATE-2026-04-28.md)

---

## 1. 开发目标

恢复后端测试门禁，让 `backend` 模块的默认测试命令重新成为稳定上线判断依据。

本任务只处理 P0 阶段第一个任务：**后端 `mvn test` 全绿**。当前已知失败集中在两类问题：

1. AI 商品上架测试仍按旧视觉直连路径断言，当前实现已切到 OCR-first。
2. `BobbuyStore`、`ProcurementHudService`、`SecurityAuthorizationIntegrationTest` 等测试之间存在数据隔离污染，导致 seed 数据、容量、冻结状态、账本状态互相影响。

---

## 2. 核心 Prompt 文本（请审查）

您可以直接将以下内容作为下一次开发任务的输入：

```text
### [任务: P0 后端测试基线恢复]

任务背景：
BOBBuy 当前代码可以完成后端编译与前端构建，但 `cd backend && mvn test` 尚未恢复为稳定门禁。根据 CURRENT-STATE-2026-04-28 与 PLAN-24，稳定上线前第一优先级是恢复后端测试基线。请直接在代码库中完成修复，并以默认测试命令全绿作为交付标准。

目标：
1. 让 `cd backend && mvn test` 稳定通过。
2. 默认测试不得依赖真实 Ollama、Codex CLI、OpenAI、MinIO、外网服务或本机特殊登录态。
3. 测试修复必须匹配当前业务实现，不能通过删除关键断言、扩大 mock 到失去业务意义、跳过测试、降低 Surefire 门禁来制造绿灯。
4. 修复完成后，补充必要的测试说明或文档更新，确保后续开发者知道默认测试与真实 AI/E2E 测试的边界。

必须先做的排查：
1. 在 `backend` 目录执行：
   - `mvn test`
2. 阅读 Surefire 报告，定位所有失败与错误：
   - `backend/target/surefire-reports`
3. 重点检查以下测试文件和相关生产代码：
   - `backend/src/test/java/com/bobbuy/api/AiProductOnboardingServiceTest.java`
   - `backend/src/test/java/com/bobbuy/service/BobbuyStoreTest.java`
   - `backend/src/test/java/com/bobbuy/service/ProcurementHudServiceTest.java`
   - `backend/src/test/java/com/bobbuy/api/SecurityAuthorizationIntegrationTest.java`
   - `backend/src/test/resources/application.properties`
   - 与上述测试直接相关的 Service、Repository、Controller、seed 初始化代码

修复范围 A：AI 商品上架测试
1. 将测试口径从旧的“视觉模型直连识别”更新为当前实现的 OCR-first 链路：
   - 上传/输入图片或文本
   - OCR 层返回文本或识别失败
   - LLM 层对 OCR 文本做结构化解析
   - supplier onboarding rules 参与提示或约束
   - source filter / web research / source governance 保持当前实现语义
   - 人工确认后才进入商品主数据或可发布状态
2. 默认测试中，OCR、LLM、web research、source filter 必须使用 mock 或 fake，不允许调用真实外部服务。
3. 补齐失败分支：
   - OCR 失败时应进入可解释的失败状态或异常路径。
   - LLM 结构化失败时不应产生脏商品数据。
   - supplier rules 存在时，应验证规则确实被传入当前 AI 上架链路。
4. 保留真实 AI 视觉验收的门控约定：
   - 真实外部 AI/OCR/E2E 测试只能在显式环境变量如 `RUN_AI_VISION_E2E=1` 时运行。
   - 默认 `mvn test` 不运行真实 AI 测试。

修复范围 B：测试数据隔离
1. 识别当前失败是否来自共享 Spring Context、H2 数据未清理、seed 只在 `count() == 0` 时执行、测试执行顺序依赖、或冻结/结算状态泄漏。
2. 对集成测试建立稳定隔离策略，优先选择对业务影响最小的方案：
   - 在测试 `@BeforeEach` 中显式清理相关 repository，并按依赖顺序删除数据。
   - 为测试数据使用唯一 businessId / tripId / customerId / supplierId，避免与 seed 数据冲突。
   - 对确实需要完整上下文重建的测试，谨慎使用 `@DirtiesContext`，避免让测试套件无谓变慢。
   - 如需调整 seed 逻辑，只能修复测试稳定性或初始化幂等问题，不能改变生产业务含义。
3. 修复以下症状：
   - 列表数量因前序测试新增数据而变化。
   - 行程容量因前序测试预留或订单状态变化而不足。
   - `COMPLETED` / `SETTLED` 冻结状态污染后续测试。
   - 财务账本、采购 HUD、审计日志因共享数据导致断言不稳定。
4. 不允许通过给测试方法加执行顺序来掩盖污染问题。

修复范围 C：测试配置与门禁
1. 检查 `backend/src/test/resources/application.properties`，确保测试 profile 使用内存库或隔离库。
2. 确认默认测试不会读取开发机 `.env` 中的真实服务地址后直接外连。
3. 如有必要，为 AI provider、storage、OCR provider 增加 test profile 默认 fake/mock 配置。
4. 保持 Maven Surefire/Failsafe 的失败即失败语义，不允许跳过失败测试。

验收命令：
1. `cd backend && mvn test`
2. 修复完成后再次执行 `cd backend && mvn test`，至少连续 2 次通过。
3. 可选但建议执行：
   - `cd backend && mvn -DskipTests compile`

交付要求：
1. 提交代码前提供失败根因摘要：
   - 哪些测试失败。
   - 根因属于 AI 测试口径漂移、数据隔离污染、配置问题，还是生产代码真实缺陷。
2. 提供修改清单：
   - 修改了哪些测试。
   - 是否修改了生产代码。
   - 是否修改了测试配置或文档。
3. 提供验证结果：
   - `mvn test` 第一次通过结果。
   - `mvn test` 第二次通过结果。
   - 如仍有残留失败，必须列出原因和下一步，不得宣称完成。

禁止事项：
1. 不得删除核心业务断言。
2. 不得把失败测试整体 `@Disabled`，除非它本来就是显式真实外部 E2E，并且必须迁移到环境变量门控。
3. 不得通过 `-DskipTests`、修改 pom 跳过测试、降低门禁来交付。
4. 不得让默认测试依赖真实 Ollama、Codex CLI、MinIO、OpenAI、浏览器或公网。
5. 不得回滚非本任务相关的用户改动。
6. 不得把测试顺序固定为解决方案。

建议实现顺序：
1. 先跑完整 `mvn test`，保存失败列表。
2. 单独修 `AiProductOnboardingServiceTest`，让它匹配 OCR-first 当前链路。
3. 单独修 `BobbuyStoreTest` 的数据隔离和 seed 假设。
4. 再修 `ProcurementHudServiceTest` 与 `SecurityAuthorizationIntegrationTest` 的共享数据污染。
5. 跑完整 `mvn test`。
6. 若出现新的真实业务缺陷，优先修生产代码，并补测试证明。
7. 连续两次完整测试通过后，更新必要文档并提交。
```

---

## 3. 审核重点

请重点审核以下问题：

1. 是否接受“默认后端测试不访问真实 AI/OCR/MinIO/外网”的门禁原则。
2. 是否接受真实 AI 视觉验收继续通过环境变量单独启用，而不是进入默认 `mvn test`。
3. 是否要求本任务同时恢复覆盖率报告，还是只先恢复测试全绿。
4. 是否允许为隔离集成测试使用 `@DirtiesContext`，即使它会增加测试耗时。

---

## 4. 预期交付物

1. 后端测试修复代码。
2. 必要的测试配置调整。
3. 如测试边界变化，更新 `docs/reports/TEST-MATRIX-本地与CI执行矩阵.md` 或相关文档。
4. 一份简短执行报告，包含失败根因、修改点、验证命令和结果。

---

## 5. 核准状态

**当前状态**: 待用户审查批准后执行。
