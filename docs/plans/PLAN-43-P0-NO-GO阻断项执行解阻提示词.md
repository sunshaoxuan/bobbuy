# PLAN-43: P0 NO-GO 阻断项执行解阻提示词

**生效日期**: 2026-04-30
**状态**: 待执行
**关联计划**:
- [PLAN-42: P0 专用环境发版证据执行提示词](PLAN-42-P0-专用环境发版证据执行提示词.md)
- [PLAN-41: P0 发版阻断项处置与安全审计提示词](PLAN-41-P0-发版阻断项处置与安全审计提示词.md)
- [PLAN-24: 稳定上线差距收口优先级](PLAN-24-稳定上线差距收口优先级.md)
**事实基线**:
- [REPORT-06: 专用环境发版证据与放行判定](../reports/REPORT-06-专用环境发版证据与放行判定.md)

---

## 1. 任务背景

REPORT-06 已给出诚实的最终判定：**NO_GO**。默认 CI、本地 smoke、sample dry-run gate、前端依赖 high/critical 清零都已经完成，但以下五项仍阻断发版：

1. CodeQL workflow 已存在，但 GitHub Actions run 数量仍为 `0`。
2. Maven OWASP Dependency-Check 因 `www.cisa.gov` DNS 解析失败，未生成可信 HTML/JSON 报告。
3. 真实 `/api/ai/onboard/scan` sample 实扫未执行。
4. 真实 `RUN_AI_VISION_E2E=1 npm run e2e:ai` 未执行。
5. 真实旧库副本 Flyway adoption / baseline / validate / restore drill 未执行。

下一轮任务必须从“记录 NO_GO”转为“执行解阻”。如果某项仍无法完成，必须把阻塞具体化到权限、账号、网络、数据、环境或负责人，不能继续只写“未执行”。

---

## 2. 核心 Prompt 文本（请审查）

您可以直接将以下内容作为下一次执行任务的输入：

```text
### [任务: P0 NO-GO 阻断项执行解阻与放行复判]

任务背景：
BOBBuy 当前发版候选结论为 NO_GO，详见 REPORT-06。默认 CI、本地测试、Playwright smoke、sample 脚本 gate、npm audit high/critical 处置都已完成；剩余 blocker 全部集中在仓库级安全执行、Maven 可信依赖审计、真实 AI/OCR 专用环境、真实旧库 adoption。请不要再只补文档，而要逐项执行、修复或拿到明确阻塞证据，并最终更新放行判定。

目标：
1. 触发并完成 GitHub Actions CodeQL workflow，归档 run URL 与 code scanning 结果。
2. 解决 Maven dependency-check 的 CISA/NVD 网络数据源问题，生成可信 HTML/JSON 报告。
3. 在真实 AI/OCR 环境执行 sample 字段级 gate，归档 JSON/Markdown report。
4. 在真实 AI/OCR 环境执行 `RUN_AI_VISION_E2E=1 npm run e2e:ai`，归档 Playwright artifact。
5. 在真实旧库副本或历史 schema dump 上完成 Flyway adoption / restore drill。
6. 新增或更新 `REPORT-07`，给出 GO / CONDITIONAL_GO / NO_GO 复判。

执行范围 A：CodeQL 实跑与结果处置
1. 在 GitHub Actions 手动触发 `.github/workflows/codeql.yml`。
2. 必须记录：
   - workflow run URL
   - commit SHA
   - 触发人
   - java-kotlin / javascript-typescript / actions 三个 matrix 结果
   - code scanning alerts 数量
3. 如果 workflow 失败：
   - 修复 workflow 配置并重跑。
   - 如果缺少权限，记录具体 GitHub 权限缺口和需要操作的仓库管理员。
4. 如果发现告警：
   - critical/high 必须修复或阻断发版。
   - moderate/low 可带期限豁免，但必须有责任人、到期日期和不可达/低风险理由。

执行范围 B：Maven 依赖审计解阻
1. 先尝试在可访问外网的环境执行：
   ```bash
   cd backend
   mvn -B org.owasp:dependency-check-maven:12.1.8:check \
     -Dformat=HTML,JSON \
     -DoutputDirectory=/tmp/bobbuy-dependency-check \
     -DskipProvidedScope=true \
     -DskipTestScope=true
   ```
2. 如果 `www.cisa.gov` / NVD 仍不可达，必须二选一：
   - 使用组织内部 NVD / CISA KEV mirror 或 dependency-check data cache。
   - 在可联网 CI/安全扫描机上执行，并归档报告。
3. 产物必须包括：
   - HTML report
   - JSON report
   - critical/high/moderate 摘要
4. critical/high 默认为发版 blocker；不得只用“网络失败”长期替代依赖审计。

执行范围 C：真实 AI sample 字段级 gate
1. 确认并记录：
   - 后端 URL
   - OCR provider
   - LLM provider
   - seed 数据版本
   - sample 图片目录版本
2. 执行：
   ```bash
   pwsh scripts/verify-ai-onboarding-samples.ps1 \
     -GoldenPath docs/fixtures/ai-onboarding-sample-golden.json \
     -SampleDir sample \
     -ScanEndpoint <真实后端>/api/ai/onboard/scan \
     -IncludeNeedsHumanGolden \
     -JsonReportPath /tmp/bobbuy-ai-sample-report.json \
     -MarkdownReportPath /tmp/bobbuy-ai-sample-report.md
   ```
3. gate 模式必须返回 `0` 才算通过。
4. 若失败，必须列出字段级失败原因，并判断是 OCR、LLM、prompt、seed、网络、权限还是 golden 本身问题。

执行范围 D：真实 AI E2E
1. 执行：
   ```bash
   cd frontend
   RUN_AI_VISION_E2E=1 npm run e2e:ai
   ```
2. 归档：
   - `frontend/playwright-report/`
   - `frontend/test-results/`
   - trace / screenshot / video
3. 若失败，必须保留 artifact，并与 sample 字段级报告交叉分析。

执行范围 E：真实旧库 Flyway adoption / restore drill
1. 获取真实旧库副本或历史 schema dump，必须确认脱敏和隔离。
2. 在隔离数据库执行：
   - pre-backup
   - baseline 或 baseline-on-migrate 方案
   - migrate
   - validate
   - restore drill
3. 记录：
   - 旧库来源
   - baseline version
   - migration 版本
   - 表数量 / flyway_schema_history 状态
   - restore 目标库校验结果
   - 任何人工数据修复
4. 没有真实旧库副本时，必须明确 DBA / 发布负责人和交付日期。

执行范围 F：放行复判报告
1. 新增：
   - `docs/reports/REPORT-07-NO-GO阻断项解阻与放行复判.md`
2. 同步更新：
   - `README.md`
   - `docs/reports/CURRENT-STATE-2026-04-28.md`
   - `docs/reports/TEST-MATRIX-本地与CI执行矩阵.md`
   - `docs/plans/PLAN-00-任务看板总览.md`
   - `docs/plans/PLAN-24-稳定上线差距收口优先级.md`
3. REPORT-07 必须给出：
   - GO：五项 blocker 全部通过
   - CONDITIONAL_GO：仅剩已批准、带期限、非 runtime 可达的豁免
   - NO_GO：任一 blocker 未执行、失败或无可信证据

验收证据：
1. CodeQL run URL 与 alerts 结果。
2. Maven dependency-check HTML/JSON report。
3. 真实 AI sample JSON/Markdown report。
4. 真实 `e2e:ai` Playwright artifact。
5. 真实旧库 adoption / restore drill 记录。
6. REPORT-07 最终复判。

禁止事项：
1. 不得用 CodeQL workflow 文件存在替代 workflow 实跑结果。
2. 不得用 mock sample dry-run 替代真实 AI sample gate。
3. 不得用默认 `npm run e2e` 替代 `RUN_AI_VISION_E2E=1 npm run e2e:ai`。
4. 不得用空库 migration 替代旧库 adoption。
5. 不得把无法执行写成通过或条件通过。
6. 不得回滚非本任务相关的用户改动。
```

---

## 3. 审核重点

请重点审核以下问题：

1. 是否把五项 blocker 全部列为 GO 前置条件。
2. Maven 审计是否必须产出 HTML/JSON 才算可信完成。
3. 如果 CodeQL workflow 因权限无法触发，是否允许继续保持 NO_GO，并只登记明确责任人。
4. AI sample 与 `e2e:ai` 是否必须在同一套 seed/provider 环境执行。
5. 旧库 adoption 是否允许用历史 schema dump 替代真实旧库副本。

---

## 4. 预期交付物

1. CodeQL run 与 code scanning 结果。
2. Maven dependency-check 报告。
3. 真实 AI sample 字段级报告。
4. 真实 AI E2E artifact。
5. 真实旧库 adoption / restore drill 记录。
6. `REPORT-07-NO-GO阻断项解阻与放行复判.md`。
7. README、CURRENT STATE、TEST MATRIX、PLAN-00、PLAN-24 同步更新。

---

## 5. 核准状态

**当前状态**: 待用户审查批准后执行。
