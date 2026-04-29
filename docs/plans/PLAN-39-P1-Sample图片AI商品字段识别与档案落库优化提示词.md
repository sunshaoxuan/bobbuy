# PLAN-39: P1 Sample 图片 AI 商品字段识别与档案落库优化提示词

**生效日期**: 2026-04-29
**状态**: 待评审
**关联计划**: [PLAN-24: 稳定上线差距收口优先级](PLAN-24-稳定上线差距收口优先级.md)
**前置任务**:
- [PLAN-31: P1 AI 与 OCR 可靠性治理提示词](PLAN-31-P1-AI与OCR可靠性治理开发提示词.md)
- [PLAN-38: P2 Playwright 端到端试运行验收提示词](PLAN-38-P2-Playwright端到端试运行验收开发提示词.md)
**事实基线**: [CURRENT-STATE-2026-04-28](../reports/CURRENT-STATE-2026-04-28.md)

---

## 1. 问题复盘

当前 AI 商品上架链路已经具备 OCR-first、LLM 结构化、source governance、人工接管、trace 与 Playwright smoke，但 sample 图片真实测试仍暴露一个核心质量问题：OCR 或后续大模型分析即使能返回部分文本，也没有稳定把信息填入正确的商品档案字段。

从现有代码看，问题不只是模型准确率：

1. `sample/IMG_*.jpg` 缺少字段级 golden 预期，现有 `frontend/e2e/ai_onboarding.spec.ts` 只断言 `name`、`brand`、`itemNumber`、`price` 等少数字段存在，没有验证字段是否正确。
2. `AiProductOnboardingService` 提示词要求输出 `basePrice`、`pricePerUnit`、`netWeight`、`description`，但解析后只有少数字段进入 `AiOnboardingSuggestion` 顶层，结构化属性只放进 `attributes`。
3. `AiAgentController.confirmOnboard` 新建商品时没有把 `suggestion.attributes()` 落到 `Product` 的结构化字段；覆盖既有商品时也只 patch 价格、价格阶梯、可见性、图库和品牌。
4. `Product` 当前没有通用商品属性 JSON 字段；`Category.attributeTemplate` 有类目属性模板，但商品实例缺少对应属性值承载。
5. `test-all-images.ps1` 能批量跑 sample，但没有生成机器可比对的字段级报告，也没有进入测试矩阵。

因此下一轮应先建立“样例图片 -> OCR/LLM -> 字段契约 -> 商品档案落库”的可验证闭环，再讨论模型或 prompt 细调。

---

## 2. 核心 Prompt 文本（请审查）

您可以直接将以下内容作为下一次开发任务的输入：

```text
### [任务: P1 Sample 图片 AI 商品字段识别与档案落库优化]

任务背景：
BOBBuy 的 AI 商品上架已完成 OCR-first、LLM 结构化、source governance、人工接管和 Playwright smoke。但使用 `sample/IMG_*.jpg` 真实样例测试时，OCR 与 LLM 仍不能稳定把商品信息填入正确商品档案字段。当前问题不能只用“模型不准”解释：代码中缺少 sample 字段级 golden 验收，LLM 输出 schema 与 Product 档案字段不完全对齐，confirm 落库也会丢弃 `attributes` 等结构化结果。请围绕 sample 图片建立字段级识别、归一化、落库和回归验证闭环。

目标：
1. 为 `sample/IMG_*.jpg` 建立字段级 golden 数据集，明确每张图期望识别的商品字段。
2. 优化 OCR/LLM 结构化 prompt 和解析逻辑，使字段输出稳定、可校验、可追踪。
3. 补齐商品档案承载字段：至少让 `attributes`、规格/净含量、单位价格、包装规格等结构化信息可以落库并通过 `/api/mobile/products` 查询。
4. 修复 `confirmOnboard` 新建和覆盖既有商品时丢字段的问题。
5. 增加自动化验证：mock 单测覆盖字段映射，sample 专用验证脚本输出 pass/fail 报告；真实 AI/OCR 验收仍作为专用环境门禁。

必须先做的排查：
1. 阅读 AI 上架链路：
   - `backend/src/main/java/com/bobbuy/service/AiProductOnboardingService.java`
   - `backend/src/main/java/com/bobbuy/api/AiAgentController.java`
   - `backend/src/main/java/com/bobbuy/api/AiOnboardingSuggestion.java`
   - `backend/src/main/java/com/bobbuy/api/AiOnboardingTrace.java`
   - `backend/src/main/java/com/bobbuy/service/LlmGateway.java`
2. 阅读商品档案模型与 API：
   - `backend/src/main/java/com/bobbuy/model/Product.java`
   - `backend/src/main/java/com/bobbuy/model/ProductPatch.java`
   - `backend/src/main/java/com/bobbuy/model/Category.java`
   - `backend/src/main/java/com/bobbuy/api/MobileProductController.java`
   - `backend/src/main/java/com/bobbuy/api/MobileProductResponse.java`
   - `backend/src/main/resources/db/migration/**`
3. 阅读前端上架 UI：
   - `frontend/src/components/AiQuickAddModal.tsx`
   - `frontend/src/pages/StockMaster.tsx`
   - `frontend/src/api.ts`
   - `frontend/e2e/ai_onboarding.spec.ts`
4. 阅读样例与现有脚本：
   - `sample/IMG_*.jpg`
   - `test-ai.ps1`
   - `test-all-images.ps1`
   - `scratch/verify_products.py`（如仍需，纳入正式脚本或删除临时依赖）

修复范围 A：Sample golden 数据集
1. 新增字段级 golden 文件，建议路径：
   - `docs/fixtures/ai-onboarding-sample-golden.json`
   - 或 `backend/src/test/resources/ai-onboarding/sample-golden.json`
2. 每张样例至少定义：
   - `sampleId`
   - `expected.name`
   - `expected.brand`
   - `expected.itemNumber`
   - `expected.basePrice`
   - `expected.categoryId`
   - `expected.attributes.netContent`
   - `expected.attributes.pricePerUnit`
   - `expected.attributes.packSize`
   - `expected.storageCondition`
   - `expected.orderMethod`
   - `tolerance`：价格容忍、可选字段、同义词、允许为空字段
3. 如果某张图片无法人工确认字段，必须标记为 `needsHumanGolden=true`，不要把不确定字段写成确定结论。
4. Golden 先覆盖至少 3 张代表性图片：
   - 一个新商品样例
   - 一个既有商品命中样例
   - 一个容易混淆货号/重量/价格的样例

修复范围 B：字段 schema 与解析归一化
1. 明确 LLM 输出 schema，建议改为稳定 JSON：
   - `name`
   - `brand`
   - `itemNumber`
   - `basePrice`
   - `currency`
   - `categoryId`
   - `description`
   - `attributes.netContent`
   - `attributes.pricePerUnit`
   - `attributes.packSize`
   - `attributes.flavor`
   - `attributes.origin`
   - `attributes.storageHint`
   - `confidence.fieldScores`
   - `evidence.fieldSources`
2. Prompt 必须明确：
   - 不得把重量、单位价格、条形码误判为货号。
   - 货号、价格、规格各自独立字段输出。
   - 不确定字段输出 `null`，不得编造。
   - 每个关键字段给出来自 OCR 行号或文本片段的 evidence。
3. 解析逻辑要支持常见别名：
   - `price` -> `basePrice`
   - `netWeight` / `netContent` -> `attributes.netContent`
   - `pricePerUnit` -> `attributes.pricePerUnit`
   - `specification` / `size` -> `attributes.packSize` 或 `attributes.specification`
4. 增加归一化：
   - 价格去千分位、币种符号、税前/税后标记。
   - 规格统一大小写和单位，如 `705 g` -> `705g`。
   - 货号剔除明显重量/价格/条码候选。
   - 类目映射到现有 `Category.id`，不能只保留自然语言 `Food`。

修复范围 C：商品档案落库
1. 评估并实现商品属性承载：
   - 推荐在 `Product` 增加 `attributes` JSONB 字段，类型 `Map<String, String>`。
   - 同步 `ProductPatch`、`MobileProductResponse`、前端 `MobileProductResponse` 类型。
   - 如新增字段，必须新增 Flyway migration。
2. `confirmOnboard` 新建商品时必须落库：
   - `name`
   - `description`
   - `brand`
   - `basePrice`
   - `categoryId`
   - `itemNumber`
   - `storageCondition`
   - `orderMethod`
   - `mediaGallery`
   - `priceTiers`
   - `attributes`
3. `confirmOnboard` 覆盖既有商品时必须按安全策略 patch：
   - 高置信字段可自动覆盖。
   - 低置信字段进入 draft/manual review，不直接覆盖正式商品。
   - `attributes` 和 `categoryId` 不得被静默丢弃。
4. 前端 `AiQuickAddModal` 必须展示并允许人工编辑结构化字段：
   - 净含量/规格
   - 单位价格
   - 包装规格
   - 类目
   - 货号
   - 价格
5. `StockMaster` 商品列表或详情至少能看到/验证关键结构化字段，不要求一次性做完整商品详情页。

修复范围 D：验证脚本与自动化测试
1. 后端单测：
   - 给 `AiProductOnboardingServiceTest` 增加“字段级解析与归一化”用例。
   - 覆盖别名字段、货号/重量/价格混淆、类目映射、attributes 输出。
2. 后端确认落库测试：
   - 新建商品时 `attributes`、`categoryId`、`itemNumber`、`basePrice` 可从 `/api/mobile/products` 查回。
   - 覆盖既有商品 patch 不丢 `attributes`。
3. 前端测试：
   - `AiQuickAddModal` 展示结构化字段并允许人工修正。
   - 保存草稿/确认上架时 payload 包含 `attributes`。
4. Sample 专用验证：
   - 将 `test-all-images.ps1` 改造成可输出 JSON/Markdown 报告的正式脚本，例如 `scripts/verify-ai-onboarding-samples.ps1`。
   - 报告包含每张图片字段级 pass/fail、实际值、期望值、trace stage、OCR/LLM provider、fallbackReason。
   - 真实 AI/OCR 验证仍需专用环境，不纳入默认 `mvn test`。
5. Playwright：
   - `npm run e2e:ai` 中增加字段级断言，但继续用 `RUN_AI_VISION_E2E=1` 门控。

修复范围 E：文档同步
1. 更新：
   - `README.md`
   - `docs/reports/CURRENT-STATE-2026-04-28.md`
   - `docs/reports/TEST-MATRIX-本地与CI执行矩阵.md`
   - `docs/plans/PLAN-24-稳定上线差距收口优先级.md`
   - 必要时新增 `docs/reports/REPORT-03-AI商品字段识别样例验证报告.md`
2. 文档必须明确：
   - sample golden 覆盖哪些图片。
   - 默认测试与真实 AI/OCR 验证的边界。
   - 当前字段识别准确率和未达标样例。
   - 人工复核策略如何防止错误字段直接发布。

验收命令：
1. `cd backend && mvn test`
2. `cd frontend && npm test`
3. `cd frontend && npm run build`
4. `cd frontend && npm run e2e`
5. `docker compose config`
6. 如新增 migration：执行 PostgreSQL Flyway migrate/validate，或说明当前环境无法执行原因。
7. 专用环境执行：`RUN_AI_VISION_E2E=1 cd frontend && npm run e2e:ai`
8. 专用环境执行 sample 验证脚本，并提交报告摘要。

交付要求：
1. 提供字段 schema 摘要：
   - OCR/LLM 输出字段
   - Product 落库字段
   - 前端展示字段
2. 提供 sample 验证结果：
   - 每张图片通过/失败
   - 失败字段与原因
   - 是否需要人工 golden 修正
3. 提供修改清单和验证结果。
4. 登记仍未完成边界：
   - 真实 AI/OCR 专用环境稳定性
   - 模型 provider 差异导致的输出漂移
   - 商品类目体系与属性模板的长期治理
   - OAuth/SSO、mTLS/service mesh、契约测试、独立 CI/CD

禁止事项：
1. 不得只调整 prompt 而不补字段级测试。
2. 不得把不确定字段编造成确定结果。
3. 不得把重量、单位价格、条形码误写为货号。
4. 不得在 confirm 阶段静默丢弃 `attributes`。
5. 不得让低置信字段直接覆盖正式商品。
6. 不得把真实 AI/OCR 外部依赖纳入默认单元测试。
7. 不得回滚非本任务相关的用户改动。

建议实现顺序：
1. 先人工整理 3 张 sample 图片 golden。
2. 写字段级失败测试，复现当前丢字段问题。
3. 增加 Product attributes 承载与 migration。
4. 调整 LLM 输出 schema、解析归一化与类目映射。
5. 修复 confirm 新建/覆盖落库。
6. 补前端展示与 payload。
7. 建 sample 验证脚本和报告。
8. 跑默认门禁与专用 AI sample 验证。
```

---

## 3. 审核重点

请重点审核以下问题：

1. `Product` 是否新增通用 `attributes` JSONB 字段，还是先把字段写入现有 description/merchantSkus。
2. 第一批 golden 覆盖哪些 sample 图片，是否需要人工先标注每张图片。
3. 字段级验收的最低通过标准：关键字段全对，还是允许非关键字段人工复核。
4. 类目映射是先用规则表，还是引入 LLM + 后端白名单校验。
5. `npm run e2e:ai` 是否作为本任务必须执行的专用验收。

---

## 4. 预期交付物

1. Sample golden 数据集。
2. 商品字段 schema、解析归一化和落库修复。
3. `Product` / API / 前端类型对结构化属性的支持。
4. 字段级后端/前端测试与 sample 验证脚本。
5. 文档和验证报告同步。

---

## 5. 核准状态

**当前状态**: 待用户审查批准后执行。
