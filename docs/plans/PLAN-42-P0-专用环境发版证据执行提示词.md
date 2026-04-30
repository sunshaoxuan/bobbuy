# PLAN-42: P0 专用环境发版证据执行提示词

**生效日期**: 2026-04-30
**状态**: 待执行
**关联计划**:
- [PLAN-41: P0 发版阻断项处置与安全审计提示词](PLAN-41-P0-发版阻断项处置与安全审计提示词.md)
- [PLAN-40: P1 发版候选门禁与专用环境验收提示词](PLAN-40-P1-发版候选门禁与专用环境验收提示词.md)
- [PLAN-24: 稳定上线差距收口优先级](PLAN-24-稳定上线差距收口优先级.md)
**事实基线**:
- [REPORT-05: 发版阻断项处置报告](../reports/REPORT-05-发版阻断项处置报告.md)
- [REPORT-04: 发版候选门禁验收报告](../reports/REPORT-04-发版候选门禁验收报告.md)

---

## 1. 任务背景

PLAN-41 已在沙箱内完成可诚实完成的发版 blocker 收口：

1. sample 验证脚本已具备 gate / report-only 语义，失败时默认返回非零。
2. 前端依赖审计已从 `3 critical / 10 high / 4 moderate` 降到 `0 critical / 0 high / 6 moderate`。
3. 剩余 `6 moderate` 已登记为 dev-only、非 runtime 可达、带期限豁免。
4. 已新增手动 CodeQL workflow。
5. 默认后端、前端、构建、Playwright smoke、Compose 与 sample 正负例脚本验证已通过。

但 `REPORT-05` 明确说明当前仍不可放行发版候选，因为剩余 blocker 不在普通沙箱内可完成：

1. GitHub Actions 中实际执行 CodeQL workflow 并归档结果。
2. Maven 依赖审计需在可访问 NVD / CISA / 内部镜像缓存的环境形成可信结果。
3. 真实 `/api/ai/onboard/scan` sample 实扫需真实 OCR/LLM/seed/样本目录。
4. `RUN_AI_VISION_E2E=1 npm run e2e:ai` 需真实 AI/OCR/MinIO/seed 专用环境。
5. 真实旧库副本 Flyway adoption / baseline / validate / restore drill 需旧库副本或历史 schema dump。

下一轮不应再在沙箱里继续“模拟完成”，而应组织专用环境执行，把发版证据收集到可审计报告中。

---

## 2. 核心 Prompt 文本（请审查）

您可以直接将以下内容作为下一次执行任务的输入：

```text
### [任务: P0 专用环境发版证据执行与最终放行判定]

任务背景：
BOBBuy 已完成 PLAN-41 中可在沙箱内处理的 blocker：sample 脚本 gate/report-only 语义、前端 high/critical 依赖处置、CodeQL workflow、默认测试与 smoke 验证。但 REPORT-05 仍明确禁止放行发版候选，因为剩余阻断项需要 GitHub Actions、安全扫描可达环境、真实 AI/OCR 专用环境、真实旧库副本。请在具备权限和外部依赖的专用环境中执行这些门禁，归档证据，并给出最终发版候选判定。

目标：
1. 在 GitHub Actions 手动触发 CodeQL workflow，并归档 run URL、结果、alerts 处置结论。
2. 在具备 NVD/CISA/内部镜像缓存的环境执行 Maven 依赖审计，并归档 HTML/JSON 报告。
3. 在真实 AI/OCR 专用环境执行 sample 字段级实扫，并归档 JSON/Markdown report。
4. 在真实 AI/OCR 专用环境执行 `RUN_AI_VISION_E2E=1 npm run e2e:ai`，并归档 Playwright artifact。
5. 在真实旧库副本或历史 schema dump 上执行 Flyway adoption / baseline / validate / restore drill，并归档 DBA/发布负责人确认。
6. 新增最终放行报告，明确“可放行 / 条件放行 / 不可放行”。

必须先确认的权限与环境：
1. GitHub Actions 可手动触发 `.github/workflows/codeql.yml`。
2. GitHub 仓库 Security / Code Scanning 页面可查看结果。
3. Maven dependency-check 可访问 NVD、CISA KEV，或使用内部缓存/镜像。
4. 专用后端可访问 `/api/ai/onboard/scan`。
5. OCR provider、LLM provider、MinIO、seed 数据、sample 图片目录全部可用。
6. 有真实旧库副本或历史 schema dump，且只能在隔离数据库执行 adoption 演练。

执行范围 A：CodeQL 实跑
1. 在 GitHub Actions 中运行 `CodeQL` workflow。
2. 记录：
   - run URL
   - commit SHA
   - 执行时间
   - java-kotlin / javascript-typescript / actions 三个 matrix 状态
   - code scanning alerts 数量
3. 若有告警：
   - 标明 priority / severity / file / line
   - 修复或形成带期限豁免
4. 若 workflow 失败：
   - 必须修复 workflow 或记录平台阻塞
   - 不得写成 CodeQL 通过

执行范围 B：Maven 依赖审计
1. 优先执行：
   ```bash
   cd backend
   mvn -B org.owasp:dependency-check-maven:12.1.8:check \
     -Dformat=HTML,JSON \
     -DoutputDirectory=/tmp/bobbuy-dependency-check \
     -DskipProvidedScope=true \
     -DskipTestScope=true
   ```
2. 如果外网被限制，使用组织内部 NVD mirror / data cache。
3. 归档：
   - dependency-check-report.html
   - dependency-check-report.json
4. 结果处置：
   - critical/high 默认为 blocker
   - moderate 需分类 runtime / dev-only / unreachable
   - 豁免必须包含到期日期和责任人

执行范围 C：真实 AI sample 实扫
1. 确认服务：
   - 后端 base URL
   - `/api/ai/onboard/scan`
   - OCR provider
   - LLM provider
   - seed 数据版本
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
3. 判定：
   - gate 模式必须返回 0 才能通过。
   - `FAIL`、`SCAN_FAIL`、`MISSING_FILE` 均为 blocker。
   - `needsHumanGolden=true` 样例若纳入 `-IncludeNeedsHumanGolden`，必须明确人工确认口径。
4. 归档 JSON/Markdown report。

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
3. 失败时必须保留 artifact，并把失败归因到：
   - OCR
   - LLM
   - seed
   - MinIO
   - 前端 UI
   - 认证/权限

执行范围 E：真实旧库 Flyway adoption
1. 在隔离环境导入真实旧库副本或历史 schema dump。
2. 执行前备份。
3. 执行 baseline / migrate / validate。
4. 执行 restore drill。
5. 记录：
   - 旧库来源与脱敏确认
   - migration 起止版本
   - baseline version
   - validate 结果
   - restore 结果
   - 任何手工数据修复步骤
6. 没有真实旧库副本时，不得宣称 adoption 通过。

执行范围 F：最终报告与看板
1. 新增：
   - `docs/reports/REPORT-06-专用环境发版证据与放行判定.md`
2. 同步更新：
   - `README.md`
   - `docs/reports/CURRENT-STATE-2026-04-28.md`
   - `docs/reports/TEST-MATRIX-本地与CI执行矩阵.md`
   - `docs/plans/PLAN-00-任务看板总览.md`
   - `docs/plans/PLAN-24-稳定上线差距收口优先级.md`
3. REPORT-06 必须给出最终结论：
   - `GO`：全部 blocker 通过，无 critical/high 未处置
   - `CONDITIONAL_GO`：仅剩带期限、责任人的非 runtime / non-blocking 豁免
   - `NO_GO`：仍有真实 AI、CodeQL、Maven、旧库 adoption 任一 blocker 未通过

验收命令/证据：
1. GitHub Actions CodeQL run URL。
2. Maven dependency-check HTML/JSON report。
3. AI sample JSON/Markdown report。
4. `RUN_AI_VISION_E2E=1 npm run e2e:ai` 结果与 Playwright artifact。
5. Flyway adoption / baseline / validate / restore drill 记录。
6. `REPORT-06` 最终判定。

禁止事项：
1. 不得用 mock dry-run 替代真实 AI sample 实扫。
2. 不得用默认 Playwright smoke 替代 `e2e:ai`。
3. 不得把 CodeQL workflow 已提交写成 CodeQL 已通过。
4. 不得把空库 Flyway migrate 当作旧库 adoption 通过。
5. 不得忽略 npm/Maven critical/high。
6. 不得回滚非本任务相关的用户改动。
```

---

## 3. 审核重点

请重点审核以下问题：

1. 是否接受只有专用环境证据齐全后才允许 `GO`。
2. 是否允许 `CONDITIONAL_GO` 带 dev-only moderate 依赖豁免上线。
3. CodeQL 是否必须三类 matrix 全部成功：Java/Kotlin、JavaScript/TypeScript、Actions。
4. 旧库 adoption 是否必须使用真实旧库副本，而不是空库或合成库。
5. AI sample 中 `needsHumanGolden=true` 样例是否纳入最终 gate，还是单独人工复核登记。

---

## 4. 预期交付物

1. CodeQL Actions run 链接与结果。
2. Maven 依赖审计报告。
3. 真实 AI sample 字段级报告。
4. 真实 AI E2E Playwright artifact。
5. 真实旧库 adoption / restore drill 记录。
6. `REPORT-06-专用环境发版证据与放行判定.md`。
7. README、CURRENT STATE、TEST MATRIX、PLAN-00、PLAN-24 同步更新。

---

## 5. 核准状态

**当前状态**: 待用户审查批准后执行。
