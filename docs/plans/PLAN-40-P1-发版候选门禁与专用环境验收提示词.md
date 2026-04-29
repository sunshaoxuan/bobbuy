# PLAN-40: P1 发版候选门禁与专用环境验收提示词

**生效日期**: 2026-04-29
**状态**: 待执行
**关联计划**: [PLAN-24: 稳定上线差距收口优先级](PLAN-24-稳定上线差距收口优先级.md)
**前置任务**:
- [PLAN-27: P0 上线验收矩阵与 CI 固化提示词](PLAN-27-P0上线验收矩阵与CI固化开发提示词.md)
- [PLAN-38: P2 Playwright 端到端试运行验收提示词](PLAN-38-P2-Playwright端到端试运行验收开发提示词.md)
- [PLAN-39: P1 Sample 图片 AI 商品字段识别与档案落库优化提示词](PLAN-39-P1-Sample图片AI商品字段识别与档案落库优化提示词.md)
**事实基线**: [CURRENT-STATE-2026-04-28](../reports/CURRENT-STATE-2026-04-28.md)

---

## 1. 任务背景

PLAN-25 到 PLAN-39 已经把默认测试、认证、迁移、部署配置、AI/OCR 治理、服务边界、运维 Runbook、Playwright smoke 与 sample 字段级落库链路逐步补齐。但从 PLAN-00 看，离真正发版仍有一组不能继续只登记为“风险”的事项：

1. `scripts/verify-ai-onboarding-samples.ps1` 存在字段 schema 漂移：golden 使用 `expected.basePrice`，而 `/api/ai/onboard/scan` 返回 `price`，真实服务正确返回价格时仍可能被脚本判为失败。
2. AI sample 专用实扫尚未在可达 `/api/ai/onboard/scan`、真实 AI/OCR、seed 数据环境中完成。
3. `npm run e2e:ai` 仍未作为发版候选证据执行并归档。
4. CodeQL / 依赖审计仍是风险登记，未进入发版候选证据包。
5. PostgreSQL Flyway 旧库 adoption / baseline / 回滚演练仍需在发版前形成操作记录。

本任务目标不是继续新增业务能力，而是把“可以发版”的判断变成可复现、可审计、可回滚的证据。

---

## 2. 核心 Prompt 文本（请审查）

您可以直接将以下内容作为下一次开发任务的输入：

```text
### [任务: P1 发版候选门禁与专用环境验收收口]

任务背景：
BOBBuy 当前默认质量门禁、认证、迁移、部署配置、AI/OCR trace、Playwright smoke、sample golden 与 Product.attributes 落库已完成。但 PLAN-00 仍显示离发版还有关键差距：真实 AI/OCR 专用环境未完成实扫，e2e:ai 未形成发版证据，CodeQL/依赖审计未固化到发版记录，Flyway 旧库 adoption/回滚演练未完成。另有一个已知 review finding：`scripts/verify-ai-onboarding-samples.ps1` 用 golden 的 `basePrice` 直接从 scan API actual 取同名字段，但 scan API 返回字段是 `price`，会把正确结果误判为失败。请优先修复该验证缺陷，并建立发版候选证据包。

目标：
1. 修复 sample 字段级验证脚本，使 golden schema 与 scan API response schema 可稳定对齐。
2. 在专用环境执行并归档 AI sample 实扫与 `npm run e2e:ai` 结果。
3. 补齐发版候选证据报告，明确默认门禁、专用门禁、安全门禁、迁移演练、剩余风险。
4. 将 CodeQL / 依赖审计纳入发版候选执行记录，不能继续只写“后续登记”。
5. 将 Flyway 旧库 adoption / baseline / 回滚演练步骤落到可执行 Runbook，并完成至少一次演练或明确环境阻塞。

必须先修复的已知问题：
1. 文件：`scripts/verify-ai-onboarding-samples.ps1`
2. 现象：golden 使用 `expected.basePrice`，实际 scan response 使用 `price`。
3. 修复要求：
   - 增加 expected path -> actual path 的字段别名映射，例如 `basePrice -> price`。
   - 字段别名必须只影响实际值读取，不改变 golden 的业务语义。
   - Markdown/JSON report 仍展示 golden 字段名，并可附带 actual path。
   - 补一个最小脚本级测试或 dry-run fixture，证明 `basePrice` 能从 actual `price` 读取。
   - 同步修正 `expected.existingProductId` 这类 optional field 路径前缀不一致问题，避免 `expected.` 前缀导致 optional 判断失效。

必须阅读：
1. `scripts/verify-ai-onboarding-samples.ps1`
2. `docs/fixtures/ai-onboarding-sample-golden.json`
3. `frontend/e2e/ai_onboarding.spec.ts`
4. `backend/src/main/java/com/bobbuy/api/AiOnboardingSuggestion.java`
5. `backend/src/main/java/com/bobbuy/service/AiProductOnboardingService.java`
6. `backend/src/main/java/com/bobbuy/api/AiAgentController.java`
7. `docs/reports/TEST-MATRIX-本地与CI执行矩阵.md`
8. `docs/runbooks/RUNBOOK-试运行部署.md`
9. `docs/runbooks/RUNBOOK-备份恢复演练.md`
10. `docs/plans/PLAN-00-任务看板总览.md`
11. `docs/plans/PLAN-24-稳定上线差距收口优先级.md`

修复范围 A：sample 验证脚本硬化
1. 增加字段路径别名：
   - `basePrice` -> `price`
   - 必要时为后续扩展保留映射表，而不是在比较处写散乱 if。
2. 统一 optional field 路径：
   - golden 中既可能写 `attributes.netContent`，也可能误写 `expected.existingProductId`。
   - 脚本应在比较前把 `expected.` 前缀规范化为实际 expected path。
3. 报告增强：
   - JSON report 中每个字段结果包含 `expectedPath`、`actualPath`、`expected`、`actual`、`passed`、`reason`。
   - Markdown report 展示字段名、实际读取路径、期望、实际、结果。
4. 增加最小本地验证：
   - 不依赖真实 `/api/ai/onboard/scan` 的 helper 测试或 mock report 测试。
   - 至少证明 `expected.basePrice=2698` 可匹配 `actual.price=2698`。

修复范围 B：AI 专用环境验收
1. 准备专用环境前置条件：
   - 后端可访问 `/api/ai/onboard/scan`
   - 真实 OCR provider 可用
   - 真实 LLM provider 可用
   - seed 数据包含商品、类目、供应商规则
   - sample 图片目录可访问
2. 执行：
   - `pwsh scripts/verify-ai-onboarding-samples.ps1 -IncludeNeedsHumanGolden`
   - `cd frontend && RUN_AI_VISION_E2E=1 npm run e2e:ai`
3. 产物：
   - JSON sample report
   - Markdown sample report
   - Playwright HTML report / trace / screenshot / video（失败时必须保留）
4. 判定：
   - 已有人工 golden 的关键字段必须通过。
   - `needsHumanGolden=true` 的样例不能静默当作通过；必须列出人工复核项。
   - 若 provider 输出漂移，报告必须列出字段级差异，而不是只写整体失败。

修复范围 C：安全与依赖门禁
1. 执行 CodeQL 或等效代码扫描，并记录结果。
2. 执行前后端依赖审计：
   - Maven 依赖审计可用项目现有工具或说明缺口。
   - npm audit 至少执行并分类 blocking / non-blocking。
3. 对无法在当前环境执行的安全项，必须写明：
   - 缺少的工具或权限
   - 替代命令
   - 发版前必须由谁在哪个环境执行

修复范围 D：Flyway 旧库 adoption 与回滚演练
1. 更新或补充 Runbook：
   - 备份旧库
   - 检查当前 schema
   - baseline / migrate / validate
   - 回滚策略
   - 数据修复边界
2. 在可用 PostgreSQL 环境执行一次演练：
   - 空库 migrate/validate
   - 模拟旧库 baseline-on-migrate 或说明为什么当前环境不能模拟
   - 回滚恢复校验
3. 把演练结果写入发版候选报告。

修复范围 E：发版候选证据报告
1. 新增报告，建议路径：
   - `docs/reports/REPORT-04-发版候选门禁验收报告.md`
2. 报告至少包含：
   - Git commit / 分支 / 日期
   - 默认门禁结果：backend test、frontend test/build、Playwright smoke、docker compose config、Docker build
   - 专用门禁结果：sample scan、e2e:ai
   - 安全门禁结果：CodeQL、dependency audit
   - 数据迁移结果：Flyway migrate/validate、旧库 adoption/回滚演练
   - 仍未完成项与是否阻断发版
3. 更新：
   - `README.md`
   - `docs/reports/CURRENT-STATE-2026-04-28.md`
   - `docs/reports/TEST-MATRIX-本地与CI执行矩阵.md`
   - `docs/plans/PLAN-00-任务看板总览.md`
   - `docs/plans/PLAN-24-稳定上线差距收口优先级.md`

验收命令：
1. `cd backend && mvn test`
2. `cd frontend && npm test`
3. `cd frontend && npm run build`
4. `cd frontend && npm run e2e`
5. `docker compose config`
6. `pwsh scripts/verify-ai-onboarding-samples.ps1 -IncludeNeedsHumanGolden`
7. `cd frontend && RUN_AI_VISION_E2E=1 npm run e2e:ai`
8. `npm audit` 或项目约定的前端依赖审计命令
9. Maven / CodeQL / dependency scan 按项目可用工具执行
10. PostgreSQL Flyway migrate/validate/adoption 演练

交付要求：
1. 修复 sample 验证脚本的字段别名与 optional path 漂移。
2. 提供发版候选证据报告。
3. 明确哪些项已通过、哪些项未执行、哪些项阻断发版。
4. 对未执行项给出具体环境阻塞，不允许笼统写“未执行”。
5. 不引入新的业务功能；本任务只做门禁、验证、报告和必要脚本修复。

禁止事项：
1. 不得把真实 AI/OCR 外部依赖纳入默认单元测试。
2. 不得把 `needsHumanGolden=true` 样例当作强制通过项。
3. 不得在没有执行的情况下把专用门禁写成通过。
4. 不得为了通过脚本而放宽 golden 关键字段判断。
5. 不得回滚非本任务相关的用户改动。
6. 不得继续只把 CodeQL / 依赖审计写成风险登记，而不提供可执行命令和发版前责任边界。
```

---

## 3. 审核重点

请重点审核以下问题：

1. 下一步是否应以“发版候选证据包”为中心，而不是继续开发新业务能力。
2. `basePrice -> price` 是否作为脚本层别名修复，还是统一调整 API / golden schema。
3. `needsHumanGolden=true` 的样例是否允许进入专用实扫报告但不阻断发版。
4. CodeQL / 依赖审计是否需要提升为发版阻断项。
5. Flyway 旧库 adoption 演练是否必须在真实旧库副本上完成，还是先用模拟旧库演练。

---

## 4. 预期交付物

1. 修复后的 sample 字段级验证脚本。
2. sample 验证脚本的最小自检或 mock 验证。
3. AI sample 专用环境实扫报告。
4. `npm run e2e:ai` 执行结果与失败 artifact 归档。
5. CodeQL / 依赖审计执行记录。
6. Flyway adoption / 回滚演练记录。
7. `REPORT-04-发版候选门禁验收报告.md`。
8. README、CURRENT STATE、TEST MATRIX、PLAN-00、PLAN-24 同步更新。

---

## 5. 核准状态

**当前状态**: 待用户审查批准后执行。
