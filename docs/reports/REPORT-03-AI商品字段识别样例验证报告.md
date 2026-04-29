# REPORT-03: AI 商品字段识别样例验证报告

**日期**: 2026-04-29  
**范围**: PLAN-39 Sample 图片 AI 商品字段识别与档案落库

---

## 1. 字段 schema 摘要

### OCR / LLM 输出

- 顶层字段：`name`、`brand`、`itemNumber`、`basePrice`、`currency`、`categoryId`、`description`
- 结构化字段：`attributes.netContent`、`attributes.pricePerUnit`、`attributes.packSize`、`attributes.flavor`、`attributes.origin`、`attributes.storageHint`
- 审计辅助：`confidence.fieldScores`、`evidence.fieldSources`

### Product 落库字段

- 商品主档：`name`、`description`、`brand`、`basePrice`、`categoryId`、`itemNumber`、`storageCondition`、`orderMethod`
- 结构化属性：`Product.attributes`（JSONB / `Map<String,String>`）
- 其余 AI 相关字段：`mediaGallery`、`priceTiers`

### 前端展示 / 编辑

- `AiQuickAddModal`：类目、货号、价格、净含量、单位价格、包装规格、储存提示
- `StockMaster`：结构化字段在抽屉中可编辑，关键字段在移动卡片中可快速核对

---

## 2. Sample golden 覆盖

- Golden 文件：`docs/fixtures/ai-onboarding-sample-golden.json`
- 已收口样例：
  - `IMG_1484.jpg`：新商品样例（已登记字段级预期）
  - `IMG_1638.jpg`：既有商品命中样例（等待人工黄金值补全）
  - `IMG_1510.jpg`：货号/重量/价格易混淆样例（等待人工黄金值补全）

> 标记 `needsHumanGolden=true` 的样例不会在默认脚本里当作失败，而是提醒补人工黄金值。

---

## 3. 自动化验证入口

- 默认 mock 回归：
  - `cd /home/runner/work/bobbuy/bobbuy/backend && mvn test`
  - `cd /home/runner/work/bobbuy/bobbuy/frontend && npm ci && npm test`
  - `cd /home/runner/work/bobbuy/bobbuy/frontend && npm run build`
- Playwright 专用 AI 样例：
  - `cd /home/runner/work/bobbuy/bobbuy/frontend && RUN_AI_VISION_E2E=1 npm run e2e:ai`
- Sample 专用脚本：
  - `pwsh /home/runner/work/bobbuy/bobbuy/scripts/verify-ai-onboarding-samples.ps1`

脚本输出：

- JSON：`/tmp/ai-onboarding-sample-report.json`
- Markdown：`/tmp/ai-onboarding-sample-report.md`

---

## 4. 当前结果

- 本次已完成默认 mock 自动化回归与结构化字段落库测试。
- 真实 AI/OCR 样例验证仍要求专用环境（本地/专用 runner + 已配置 OCR/LLM 服务）。
- 当前未在本沙箱内执行真实样例脚本，因此真实识别准确率仍以专用环境复核结果为准。

---

## 5. 人工复核边界

- 低置信匹配不允许直接覆盖既有正式商品。
- `attributes`、`categoryId`、`itemNumber` 不再在 confirm 阶段静默丢弃。
- `needsHumanGolden=true` 的样例必须人工补齐后，才可提升为强制字段级门禁。
