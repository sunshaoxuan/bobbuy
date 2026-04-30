# REPORT-05: 发版阻断项处置报告

**日期**: 2026-04-29  
**分支**: `copilot/plan-41-p0-release-blocking-issues`

---

## 1. 结论摘要

- **sample 门禁语义**：已修复。`scripts/verify-ai-onboarding-samples.ps1` 默认 gate 模式下，遇到 `FAIL` / `SCAN_FAIL` / `MISSING_FILE` 会返回非零；`-ReportOnly` 仅保留给人工出报告。
- **前端依赖审计**：已将 `npm audit` 从 `3 critical / 10 high / 4 moderate` 降至 `0 critical / 0 high / 6 moderate`。剩余 `6 moderate` 全部位于 Vite/Vitest dev-only 工具链，当前登记为带期限豁免。
- **CodeQL**：已新增手动 workflow `.github/workflows/codeql.yml`，覆盖 Java/Kotlin、JavaScript/TypeScript、Actions；本沙箱未执行仓库级扫描结果。
- **Maven 依赖审计**：已尝试执行 OWASP Dependency-Check Maven，但受沙箱外网解析限制阻塞，未形成可信扫描结果。
- **真实专用环境**：真实 `/api/ai/onboard/scan` sample 实扫、`RUN_AI_VISION_E2E=1 npm run e2e:ai`、真实旧库 adoption/baseline/restore 演练均仍未完成。

**最终结论**：**当前仍不可放行发版候选**。剩余 blocker 主要集中在仓库级安全执行与专用环境证据，不在本沙箱内可诚实宣告通过。

---

## 2. sample 门禁修复结果

### 2.1 代码与产物

- 脚本：`/home/runner/work/bobbuy/bobbuy/scripts/verify-ai-onboarding-samples.ps1`
- 新增负例 fixture：`/home/runner/work/bobbuy/bobbuy/docs/fixtures/ai-onboarding-sample-scan-mock-fail.json`

### 2.2 验证命令

1. 通过用例（gate 模式，预期 `exit 0`）

```bash
pwsh -NoProfile -Command "& '/home/runner/work/bobbuy/bobbuy/scripts/verify-ai-onboarding-samples.ps1' \
  -MockScanResponsePath '/home/runner/work/bobbuy/bobbuy/docs/fixtures/ai-onboarding-sample-scan-mock.json' \
  -SampleIds @('IMG_1484.jpg','IMG_1638.jpg') \
  -IncludeNeedsHumanGolden \
  -JsonReportPath '/tmp/plan41-pass-report.json' \
  -MarkdownReportPath '/tmp/plan41-pass-report.md'"
```

2. 失败用例（gate 模式，预期非零）

```bash
pwsh -NoProfile -Command "& '/home/runner/work/bobbuy/bobbuy/scripts/verify-ai-onboarding-samples.ps1' \
  -MockScanResponsePath '/home/runner/work/bobbuy/bobbuy/docs/fixtures/ai-onboarding-sample-scan-mock-fail.json' \
  -SampleIds @('IMG_1484.jpg') \
  -JsonReportPath '/tmp/plan41-fail-report.json' \
  -MarkdownReportPath '/tmp/plan41-fail-report.md'"
```

3. report-only 用例（预期 `exit 0`，但 `gatePassed=false`）

```bash
pwsh -NoProfile -Command "& '/home/runner/work/bobbuy/bobbuy/scripts/verify-ai-onboarding-samples.ps1' \
  -MockScanResponsePath '/home/runner/work/bobbuy/bobbuy/docs/fixtures/ai-onboarding-sample-scan-mock-fail.json' \
  -SampleIds @('IMG_1484.jpg') \
  -ReportOnly \
  -JsonReportPath '/tmp/plan41-report-only.json' \
  -MarkdownReportPath '/tmp/plan41-report-only.md'"
```

### 2.3 结果

- `/tmp/plan41-pass-report.json`：`summary.gatePassed=true`
- `/tmp/plan41-fail-report.json`：`summary.fail=1`、`summary.gatePassed=false`，命令返回非零
- `/tmp/plan41-report-only.json`：`summary.gatePassed=false`，但命令返回 `0`

脚本现已满足：

1. JSON / Markdown 报告在失败时仍完整输出。
2. `NEEDS_HUMAN_GOLDEN` 在未传 `-IncludeNeedsHumanGolden` 时继续作为非阻断跳过。
3. 报告摘要包含 `total / pass / fail / skipped / scanFail / missingFile / gatePassed`。

---

## 3. 前端依赖审计结果

### 3.1 执行命令

```bash
cd /home/runner/work/bobbuy/bobbuy/frontend
npm audit --json
```

### 3.2 已完成修复

| 类别 | 处置 |
| :-- | :-- |
| Direct runtime | `react-router-dom` 升级到 `^6.30.3`，消除 open redirect/XSS 高危 |
| Direct dev | `@playwright/test` 升级到 `^1.59.1` |
| Direct dev | `vite` 升级到 `^5.4.21`，`@vitejs/plugin-react` 升级到 `^4.7.0` |
| Direct dev | `vitest` / `@vitest/coverage-v8` / `@vitest/ui` 升级到 `^1.6.1` |
| Direct dev | `@testing-library/jest-dom` 升级到 `^6.9.1`，移除旧 `lodash` 链路 |
| Transitive dev | 通过 `overrides` 锁定 `minimatch=3.1.4`、`brace-expansion=1.1.13`、`rollup=4.59.0`、`postcss=8.5.10` |

### 3.3 当前审计摘要

- 修复前：`3 critical / 10 high / 4 moderate`
- 修复后：`0 critical / 0 high / 6 moderate`

### 3.4 剩余豁免（均为 dev-only / not-reachable in runtime）

| 包 | 等级 | direct/transitive | runtime/dev-only | reachable | fix available | 临时缓解 | 到期 | 责任人 |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| `vite` | moderate | direct | dev-only | 否（仅本地 dev server / build tool） | 需升 `8.x`（major） | 试运行/发版不暴露 Vite dev server；仅在本地/CI 临时环境使用 | 2026-05-31 | 前端负责人 |
| `esbuild` | moderate | transitive | dev-only | 否 | 随 `vite 8.x` 解决 | 同上 | 2026-05-31 | 前端负责人 |
| `vitest` | moderate | direct | dev-only | 否（仅测试） | 需升 `4.x`（major） | 仅在本地/CI 使用；不进入生产运行时 | 2026-05-31 | 前端负责人 |
| `vite-node` | moderate | transitive | dev-only | 否 | 随 `vitest 4.x` 解决 | 同上 | 2026-05-31 | 前端负责人 |
| `@vitest/coverage-v8` | moderate | direct | dev-only | 否 | 需升 `4.x`（major） | 同上 | 2026-05-31 | 前端负责人 |
| `@vitest/ui` | moderate | direct | dev-only | 否 | 需升 `4.x`（major） | 同上 | 2026-05-31 | 前端负责人 |

结论：前端当前已无 `critical/high` 发版阻断项；剩余 moderate 需继续跟踪，但不属于线上 runtime 可达面。

---

## 4. Maven 依赖审计结果

### 4.1 尝试执行

```bash
cd /home/runner/work/bobbuy/bobbuy/backend
mvn -B org.owasp:dependency-check-maven:12.1.8:check \
  -Dformat=JSON \
  -DoutputDirectory=/tmp/plan41-dependency-check \
  -DskipProvidedScope=true \
  -DskipTestScope=true
```

### 4.2 当前结果

- **状态**：BLOCKED
- **失败原因**：沙箱无法解析 `www.cisa.gov`，导致 OWASP Dependency-Check 在更新 Known Exploited Vulnerabilities 数据时失败，并报 `No documents exist`
- **责任边界**：需由后端/平台负责人在具备完整外网访问或内部镜像缓存的环境中重跑

### 4.3 发版前执行路径

优先命令：

```bash
cd /home/runner/work/bobbuy/bobbuy/backend
mvn -B org.owasp:dependency-check-maven:12.1.8:check \
  -Dformat=HTML,JSON \
  -DoutputDirectory=/tmp/plan41-dependency-check \
  -DskipProvidedScope=true \
  -DskipTestScope=true
```

若 CISA/KEV 外网仍受限，至少需在可用环境中补齐镜像源或缓存后执行，不得在发版记录中只写“未执行”。

---

## 5. CodeQL / 等效扫描

### 5.1 已交付

- 新增 workflow：`/home/runner/work/bobbuy/bobbuy/.github/workflows/codeql.yml`
- 触发方式：GitHub Actions `workflow_dispatch`
- 覆盖语言：
  - `java-kotlin`
  - `javascript-typescript`
  - `actions`

### 5.2 本地状态

- 本沙箱未持有仓库级 CodeQL 执行能力，未形成 alerts 结果
- 已额外校验 `github/codeql-action@v4.35.2` 无已知 advisory

### 5.3 发版前责任边界

需由仓库管理员或安全负责人在 GitHub Actions 中手动触发 `CodeQL` workflow，并把 0 alerts 或告警处置结论写回 Release/PR 记录。

---

## 6. 专用环境与旧库 blocker

| 项目 | 当前状态 | blocker 原因 | 发版前责任边界 |
| :-- | :-- | :-- | :-- |
| 真实 `/api/ai/onboard/scan` sample 实扫 | BLOCKED | 当前沙箱无可达后端、OCR provider、LLM provider、seed 数据、样本目录联调环境 | 由 AI/OCR 专用环境执行，并归档 JSON/Markdown report |
| `RUN_AI_VISION_E2E=1 npm run e2e:ai` | BLOCKED | 当前沙箱无真实 AI/OCR/MinIO/seed 专用环境 | 由专用环境执行并归档 Playwright HTML/trace/screenshot/video |
| 真实旧库 adoption / baseline / restore drill | BLOCKED | 当前没有真实旧库副本或历史 schema dump | 由 DBA / 发布负责人在隔离库执行并归档结果 |

---

## 7. 本次验证命令

- `cd /home/runner/work/bobbuy/bobbuy && docker compose config`
- `cd /home/runner/work/bobbuy/bobbuy/backend && mvn test`
- `cd /home/runner/work/bobbuy/bobbuy/frontend && npm ci && npm test`
- `cd /home/runner/work/bobbuy/bobbuy/frontend && npm run build`
- `cd /home/runner/work/bobbuy/bobbuy/frontend && npm run e2e`
- sample gate / report-only 三组脚本命令（见第 2 节）
- `cd /home/runner/work/bobbuy/bobbuy/frontend && npm audit --json`
- Maven 依赖审计尝试命令（见第 4 节）

---

## 8. 最终结论

**当前不可放行发版候选。**

剩余 blocker：

1. CodeQL workflow 尚未在 GitHub Actions 实跑并归档结果。
2. Maven 依赖审计尚未在具备外网/缓存的环境形成可信结果。
3. 真实 AI sample 实扫尚未执行。
4. `RUN_AI_VISION_E2E=1 npm run e2e:ai` 尚未执行。
5. 真实旧库 adoption / baseline / validate / restore drill 尚未执行。

非 blocker 但需跟踪：

1. 前端剩余 `6 moderate` dev-only 依赖风险，已登记期限豁免，需在 2026-05-31 前完成 Vite/Vitest major 升级评估。
