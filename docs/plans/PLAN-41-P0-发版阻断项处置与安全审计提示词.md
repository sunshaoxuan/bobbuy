# PLAN-41: P0 发版阻断项处置与安全审计提示词

**生效日期**: 2026-04-30
**状态**: 待执行
**关联计划**:
- [PLAN-40: P1 发版候选门禁与专用环境验收提示词](PLAN-40-P1-发版候选门禁与专用环境验收提示词.md)
- [PLAN-24: 稳定上线差距收口优先级](PLAN-24-稳定上线差距收口优先级.md)
**事实基线**:
- [REPORT-04: 发版候选门禁验收报告](../reports/REPORT-04-发版候选门禁验收报告.md)
- [CURRENT-STATE-2026-04-28](../reports/CURRENT-STATE-2026-04-28.md)

---

## 1. 任务背景

PLAN-40 已完成默认门禁、sample 脚本字段别名修复、本地 dry-run、PostgreSQL 空库迁移与恢复演练，并形成 `REPORT-04`。但 `REPORT-04` 仍明确登记了发版阻断项：

1. 真实 `/api/ai/onboard/scan` sample 实扫未执行。
2. 真实 `RUN_AI_VISION_E2E=1 npm run e2e:ai` 未执行。
3. 仓库级 CodeQL / 等效扫描未执行。
4. Maven 依赖审计未执行。
5. `npm audit` 已发现 `3 critical / 10 high / 4 moderate`，尚未分类处置。
6. 真实旧库副本 Flyway adoption / baseline 演练未执行。

本轮评审还发现一个新的门禁问题：`scripts/verify-ai-onboarding-samples.ps1` 当前即使生成 `FAIL` / `SCAN_FAIL` 结果，也不会以非零退出码结束。它可以生成报告，但作为门禁命令时会让 CI 或人工脚本误判通过。下一轮必须先修正这个门禁语义，再处理安全和专用环境阻断项。

---

## 2. 核心 Prompt 文本（请审查）

您可以直接将以下内容作为下一次开发任务的输入：

```text
### [任务: P0 发版阻断项处置与安全审计收口]

任务背景：
BOBBuy 已完成 PLAN-40 的默认门禁、sample 验证脚本字段别名修复、本地 dry-run、Flyway 空库迁移与恢复演练，并生成 REPORT-04。但 REPORT-04 仍明确说明当前不能放行发版候选：真实 AI/OCR sample 实扫、RUN_AI_VISION_E2E=1 e2e:ai、仓库级 CodeQL、Maven 依赖审计、npm audit 高危依赖处置、真实旧库 adoption 演练都还没有形成通过证据。此外，sample 验证脚本虽然能生成字段级报告，但遇到 FAIL/SCAN_FAIL 时仍返回 0，会让门禁命令误判通过。请把这些阻断项逐项处置到“通过、豁免或明确环境阻塞”的可审计状态。

目标：
1. 修复 sample 验证脚本的门禁退出码语义：字段 FAIL / SCAN_FAIL / MISSING_FILE 默认必须返回非零。
2. 对 npm audit 中的 critical/high/moderate 依赖进行分类、修复或形成可审查豁免。
3. 补齐 Maven 依赖审计工具或执行等效 Maven 依赖扫描，并输出结果。
4. 在仓库级或可用替代环境执行 CodeQL / 等效代码扫描，并归档结果。
5. 在专用环境完成真实 AI sample 实扫与 `RUN_AI_VISION_E2E=1 npm run e2e:ai`，或给出不可执行的具体环境阻塞清单。
6. 在真实旧库副本或历史 schema dump 上执行 Flyway adoption / baseline / validate / restore 演练，或给出不可执行的具体数据阻塞清单。
7. 更新 REPORT-04 或新增 REPORT-05，明确发版候选是否可放行。

必须先修复的已知问题：
1. 文件：`scripts/verify-ai-onboarding-samples.ps1`
2. 现象：脚本生成 `FAIL`、`SCAN_FAIL`、`MISSING_FILE` 时仍以 0 退出。
3. 修复要求：
   - 默认行为：只要存在 `FAIL`、`SCAN_FAIL`、`MISSING_FILE`，脚本退出码必须非零。
   - `NEEDS_HUMAN_GOLDEN` 在未传 `-IncludeNeedsHumanGolden` 时可作为非阻断跳过状态。
   - 增加可选参数，例如 `-AllowFailures` 或 `-ReportOnly`，仅用于人工生成报告，不得用于门禁命令。
   - JSON / Markdown report 仍必须完整输出，即使最终退出非零。
   - 增加 dry-run 负例，证明 mock 失败字段会返回非零。
   - README / TEST-MATRIX 必须写清门禁命令与 report-only 命令的区别。

必须阅读：
1. `scripts/verify-ai-onboarding-samples.ps1`
2. `docs/fixtures/ai-onboarding-sample-golden.json`
3. `docs/fixtures/ai-onboarding-sample-scan-mock.json`
4. `docs/reports/REPORT-04-发版候选门禁验收报告.md`
5. `docs/reports/TEST-MATRIX-本地与CI执行矩阵.md`
6. `docs/runbooks/RUNBOOK-试运行部署.md`
7. `docs/runbooks/RUNBOOK-备份恢复演练.md`
8. `frontend/package.json`、`frontend/package-lock.json`
9. 根目录和后端 Maven `pom.xml`
10. `.github/workflows/ci.yml`

修复范围 A：sample 脚本门禁语义
1. 增加脚本退出码：
   - PASS-only：exit 0
   - 含 FAIL / SCAN_FAIL / MISSING_FILE：exit 1
   - 参数错误 / report 写入失败：exit 2 或抛出 PowerShell 错误
2. 增加 report-only 参数：
   - 允许发报告但不作为门禁。
   - 文档必须标注不能用于 release gate。
3. 增加 mock 负例：
   - 可新增 `docs/fixtures/ai-onboarding-sample-scan-mock-fail.json`
   - 或在脚本测试命令中传入会失败的 sample/mock。
4. 报告摘要增加：
   - total / pass / fail / skipped / scanFail / missingFile
   - gatePassed: true/false

修复范围 B：前端依赖审计
1. 执行 `cd frontend && npm audit --json`。
2. 将每个 critical/high/moderate 归类：
   - direct / transitive
   - runtime / dev-only
   - reachable / not-reachable
   - fix available / requires major / no fix
3. 优先修复不破坏构建和测试的依赖问题。
4. 对无法修复项输出豁免说明：
   - 包名
   - CVE / advisory
   - 风险等级
   - 是否运行时可达
   - 临时缓解
   - 到期日期和责任人
5. 修复后必须重跑：
   - `cd frontend && npm test`
   - `cd frontend && npm run build`
   - `cd frontend && npm run e2e`
   - `cd frontend && npm audit --json`

修复范围 C：Maven 依赖审计
1. 优先使用项目已有能力；如没有，评估加入 OWASP Dependency-Check Maven plugin 或 GitHub dependency review。
2. 不得为了扫描大改构建结构。
3. 输出后端依赖审计摘要：
   - critical / high / moderate 数量
   - 直接依赖与传递依赖
   - 修复或豁免建议
4. 如工具因网络、NVD API 或沙箱限制不可用，必须写明替代执行命令和发版前责任边界。

修复范围 D：CodeQL / 等效扫描
1. 检查 `.github/workflows` 是否已具备 CodeQL workflow。
2. 如果没有，新增手动 `workflow_dispatch` CodeQL workflow，覆盖 Java/Kotlin、JavaScript/TypeScript、Actions。
3. 如果本地或沙箱不能执行 CodeQL，必须至少提交 workflow 与执行说明，并在报告中标明仓库管理员需要在 GitHub Actions 中运行。
4. 若已有可执行工具，执行并记录 0 alerts 或具体告警。

修复范围 E：真实 AI/OCR 专用环境门禁
1. 在具备真实 provider 的环境执行：
   - `pwsh scripts/verify-ai-onboarding-samples.ps1 -IncludeNeedsHumanGolden`
   - `cd frontend && RUN_AI_VISION_E2E=1 npm run e2e:ai`
2. 产物归档：
   - sample JSON report
   - sample Markdown report
   - Playwright HTML report / trace / screenshot / video
3. 若无法执行，必须列出具体缺口：
   - 后端服务地址
   - OCR provider
   - LLM provider
   - seed 数据
   - MinIO / 存储
   - 授权或网络限制

修复范围 F：真实旧库 Flyway adoption 演练
1. 获取真实旧库副本或历史 schema dump。
2. 在隔离数据库执行：
   - backup
   - baseline / migrate / validate
   - restore drill
3. 若没有旧库副本，更新 Runbook 与报告，明确发版前必须由 DBA / 发布负责人完成，不能把空库 migrate 当成 adoption 通过。

修复范围 G：报告与看板
1. 更新或新增：
   - `docs/reports/REPORT-05-发版阻断项处置报告.md`
2. 同步更新：
   - `README.md`
   - `docs/reports/CURRENT-STATE-2026-04-28.md`
   - `docs/reports/TEST-MATRIX-本地与CI执行矩阵.md`
   - `docs/plans/PLAN-00-任务看板总览.md`
   - `docs/plans/PLAN-24-稳定上线差距收口优先级.md`
   - 必要 Runbook
3. REPORT-05 必须给出最终结论：
   - 可放行发版候选
   - 不可放行，并列出剩余 blocker
   - 条件放行，并列出豁免项、到期日期、责任人

验收命令：
1. `pwsh scripts/verify-ai-onboarding-samples.ps1 -MockScanResponsePath docs/fixtures/ai-onboarding-sample-scan-mock.json -SampleIds IMG_1484.jpg,IMG_1638.jpg -IncludeNeedsHumanGolden`
2. mock 负例脚本命令必须返回非零。
3. `cd backend && mvn test`
4. `cd frontend && npm test`
5. `cd frontend && npm run build`
6. `cd frontend && npm run e2e`
7. `docker compose config`
8. `cd frontend && npm audit --json`
9. Maven 依赖审计命令
10. CodeQL / 等效扫描命令或 GitHub Actions workflow 执行记录
11. 专用环境：`RUN_AI_VISION_E2E=1 npm run e2e:ai`
12. 专用环境：真实 sample scan
13. 旧库副本：Flyway adoption / baseline / validate / restore drill

交付要求：
1. 修复脚本门禁退出码，不允许 FAIL 仍返回 0。
2. npm audit 高危项必须有修复、豁免或明确 blocker。
3. Maven / CodeQL 不得只写“未执行”，必须给出可执行路径和责任边界。
4. REPORT-05 明确发版候选是否可放行。
5. 不做新业务功能，不扩大微服务拆分范围。
6. 不回滚非本任务相关的用户改动。
```

---

## 3. 审核重点

请重点审核以下问题：

1. sample 验证脚本是否应默认在失败时返回非零退出码。
2. `npm audit` 的 critical/high 是否全部作为发版阻断，还是允许 dev-only / 不可达项带期限豁免。
3. CodeQL 是否应新增手动 workflow，还是只要求仓库级安全负责人执行。
4. Maven 依赖审计是否允许先用报告型工具，还是必须纳入 CI。
5. 真实旧库 adoption 是否必须在真实旧库副本完成后才允许发版候选放行。

---

## 4. 预期交付物

1. 修复后的 sample 验证脚本及正/负 dry-run 证据。
2. npm audit 分类、修复或豁免记录。
3. Maven 依赖审计结果。
4. CodeQL / 等效扫描执行记录或 workflow。
5. 真实 AI/OCR sample 与 `e2e:ai` 专用环境结果。
6. 真实旧库 adoption / baseline / restore 演练记录。
7. `REPORT-05-发版阻断项处置报告.md`。
8. README、CURRENT STATE、TEST MATRIX、PLAN-00、PLAN-24、Runbook 同步更新。

---

## 5. 核准状态

**当前状态**: 待用户审查批准后执行。
