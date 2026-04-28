# PLAN-26: P0 前端测试基线恢复开发提示词

**生效日期**: 2026-04-28
**状态**: 待评审
**关联计划**: [PLAN-24: 稳定上线差距收口优先级](PLAN-24-稳定上线差距收口优先级.md)
**前置任务**: [PLAN-25: P0 后端测试基线恢复开发提示词](PLAN-25-P0后端测试基线恢复开发提示词.md)
**事实基线**: [CURRENT-STATE-2026-04-28](../reports/CURRENT-STATE-2026-04-28.md)

---

## 1. 开发目标

恢复前端测试门禁，让 `frontend` 模块的默认测试命令重新成为稳定上线判断依据。

本任务只处理 P0 阶段第二个任务：**前端 `npm test` 稳定完成**。当前事实基线显示：`npm run build` 已通过，但 `npm test` 本地执行超过 3 分钟未完成，无法作为上线门禁。

---

## 2. 核心 Prompt 文本（请审查）

您可以直接将以下内容作为下一次开发任务的输入：

```text
### [任务: P0 前端测试基线恢复]

任务背景：
BOBBuy 后端测试基线已经恢复，下一步必须恢复前端默认测试门禁。当前 `frontend/package.json` 中默认测试脚本为 `vitest run`，但最近本地执行 `npm test` 超过 3 分钟未完成。请直接在代码库中完成修复，让前端单元/组件测试稳定、可重复、可作为 CI 门禁。

目标：
1. 让 `cd frontend && npm test` 在可接受时间内稳定完成。
2. 保持 `cd frontend && npm run build` 继续通过。
3. 默认前端测试不得依赖真实后端、真实 AI/OCR、真实 MinIO、Playwright 浏览器、外网或长轮询等待。
4. 区分 Vitest 单元/组件测试与 Playwright E2E 测试边界，不允许把 E2E 行为塞进默认 `npm test`。
5. 补齐当前新增/高风险页面的基础测试，尤其是供应商规则、商品删除、AI 识别结果人工编辑确认等近期变更。

必须先做的排查：
1. 在 `frontend` 目录执行：
   - `npm test`
2. 如果超时或卡住，使用更细粒度命令定位问题：
   - `npx vitest run --reporter=verbose`
   - `npx vitest run src/components --reporter=verbose`
   - `npx vitest run src/pages --reporter=verbose`
   - 必要时按单文件执行，例如 `npx vitest run src/pages/StockMaster.test.tsx --reporter=verbose`
3. 检查以下配置与测试辅助代码：
   - `frontend/package.json`
   - `frontend/vitest.config.ts`
   - `frontend/src/test/*`
   - 现有 `src/components/*.test.tsx`
   - 现有 `src/pages/*.test.tsx`
   - `frontend/src/pages/__tests__/*`

修复范围 A：定位并消除测试超时
1. 找出导致 `npm test` 超时的具体测试文件或测试用例。
2. 处理常见卡住原因：
   - 未 mock 的 `fetch` / WebSocket / STOMP / 定时器。
   - `setInterval`、`setTimeout`、轮询逻辑未清理。
   - React Testing Library 的 `waitFor` 等待条件永远不满足。
   - Ant Design 弹窗、通知、Portal 未正确 cleanup。
   - 测试实际启动了 Playwright 或真实浏览器。
3. 对轮询、聊天、AI 识别、采购 HUD 等异步页面，使用 fake timers、明确 mock 和 cleanup。
4. 不允许通过无限放大测试超时时间来掩盖问题；只有在定位清楚且必要时，才可做小幅、局部的超时调整。

修复范围 B：单测与 E2E 边界
1. 默认 `npm test` 只运行 Vitest 单元/组件测试。
2. Playwright 场景继续放在：
   - `npm run e2e`
   - `npm run e2e:ai`
3. 真实 AI 视觉链路只允许在显式 `RUN_AI_VISION_E2E=1` 的 E2E 命令中运行。
4. 前端单测中所有后端 API、AI/OCR、对象存储、WebSocket 都必须 mock。

修复范围 C：近期高风险功能补测试
请在不扩大范围到完整 E2E 的前提下，为以下近期变更补基础组件/页面测试：

1. 供应商规则：
   - 供应商页面可展示/编辑 onboarding rules。
   - 保存时提交结构化规则字段。
   - 缺失规则时页面不崩溃。
2. 商品删除：
   - 商品列表存在删除入口。
   - 用户确认后调用正确 API。
   - 删除成功后列表刷新或本地移除。
   - 删除失败时显示错误状态，不静默吞掉。
3. AI 识别结果人工编辑确认：
   - AI 识别结果可被用户修改。
   - 确认时提交用户修改后的值，而不是原始 AI 值。
   - 识别失败或来源治理失败时显示可操作状态。
4. i18n：
   - 新增页面或按钮不显示裸 key。
   - 中/日/英至少默认语言路径不破坏渲染。

修复范围 D：测试基础设施
1. 如现有测试重复 mock API，可抽出轻量测试 helper，但不要重构过大。
2. 确保每个测试后清理：
   - `vi.restoreAllMocks()`
   - fake timers
   - localStorage/sessionStorage
   - document body 中的 portal 残留
3. 如使用 MSW 或统一 fetch mock，必须保持配置简单、默认不访问网络。
4. 不要因为测试方便修改生产组件行为；如发现真实 UI bug，应修生产代码并用测试证明。

验收命令：
1. `cd frontend && npm test`
2. 修复完成后再次执行 `cd frontend && npm test`，至少连续 2 次通过。
3. `cd frontend && npm run build`
4. 可选但建议执行：
   - `cd frontend && npm run e2e`，如果本机 Playwright 环境可用。

交付要求：
1. 提供失败根因摘要：
   - 哪些测试卡住或失败。
   - 根因属于未 mock 网络、计时器泄漏、异步断言错误、Portal cleanup、真实 UI bug，还是测试配置问题。
2. 提供修改清单：
   - 修改了哪些测试。
   - 是否修改了生产前端代码。
   - 是否修改了测试配置或文档。
3. 提供验证结果：
   - `npm test` 第一次通过结果与耗时。
   - `npm test` 第二次通过结果与耗时。
   - `npm run build` 结果。
   - 如 E2E 未执行，说明原因。

禁止事项：
1. 不得删除关键业务测试来换取通过。
2. 不得把失败测试整体 `skip` / `todo`，除非它确实属于 Playwright E2E 或真实外部 AI 验收，并且必须迁移到正确脚本。
3. 不得让默认 `npm test` 依赖真实后端、AI、OCR、MinIO、WebSocket 服务、浏览器或公网。
4. 不得用大幅提高全局 timeout 代替修复泄漏。
5. 不得回滚非本任务相关的用户改动。

建议实现顺序：
1. 先跑 `npm test`，确认卡住位置。
2. 用 verbose 和单文件执行定位最慢/卡死测试。
3. 修复网络、计时器、Portal、异步等待泄漏。
4. 补供应商规则、商品删除、AI 人工确认的最小基础测试。
5. 跑完整 `npm test`。
6. 跑 `npm run build`。
7. 连续两次完整 `npm test` 通过后，更新测试矩阵或当前状态文档。
```

---

## 3. 审核重点

请重点审核以下问题：

1. 是否接受默认 `npm test` 只覆盖 Vitest 单元/组件测试，Playwright E2E 独立执行。
2. 是否要求本任务必须补齐近期变更的基础测试，还是先只解决超时。
3. 是否把 `npm run e2e` 纳入本任务硬性验收，还是作为可选验证。
4. 是否需要引入统一 fetch mock/MSW，或先沿用现有轻量 mock。

---

## 4. 预期交付物

1. 前端测试超时修复。
2. 必要的组件/页面测试补充。
3. 必要的测试 helper 或 Vitest 配置调整。
4. 如测试边界变化，更新 `docs/reports/TEST-MATRIX-本地与CI执行矩阵.md` 或相关文档。
5. 一份简短执行报告，包含失败根因、修改点、验证命令、耗时和结果。

---

## 5. 核准状态

**当前状态**: 待用户审查批准后执行。
