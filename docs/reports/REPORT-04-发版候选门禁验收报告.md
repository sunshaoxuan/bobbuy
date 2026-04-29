# REPORT-04: 发版候选门禁验收报告

**日期**: 2026-04-29  
**分支**: `copilot/plan-40-release-candidate-prompts`  
**提交**: `48f1bdc`

---

## 1. 结论摘要

- **默认门禁**：当前沙箱内已完成并通过 `docker compose config`、`backend mvn test`、`frontend npm ci && npm test`、`frontend npm run build`、`frontend npm run e2e`。
- **脚本硬化**：`scripts/verify-ai-onboarding-samples.ps1` 已修复 `basePrice -> price` 实际字段别名、`expected.` optional path 规范化，并补了 mock dry-run fixture。
- **迁移/回滚**：已完成 PostgreSQL 15 本地空库 Flyway `clean/migrate/validate` 与 `pg_dump -> 新库恢复` 演练。
- **阻断项**：真实 `/api/ai/onboard/scan` sample 实扫、真实 `npm run e2e:ai`、仓库级 CodeQL、Maven 依赖审计、真实旧库副本 baseline/adoption 演练仍未在本沙箱完成，**当前不能宣告发版候选已放行**。

---

## 2. Git 与执行上下文

- 仓库：`sunshaoxuan/bobbuy`
- 分支：`copilot/plan-40-release-candidate-prompts`
- 提交：`48f1bdc`
- 参考计划：
  - `docs/plans/PLAN-40-P1-发版候选门禁与专用环境验收提示词.md`
  - `docs/plans/PLAN-24-稳定上线差距收口优先级.md`
  - `docs/reports/CURRENT-STATE-2026-04-28.md`

---

## 3. 默认门禁结果

| 门禁项 | 命令 | 结果 | 备注 |
| :-- | :-- | :-- | :-- |
| Compose 配置渲染 | `cd /home/runner/work/bobbuy/bobbuy && docker compose config` | PASS | 配置可成功展开 |
| 后端测试 | `cd /home/runner/work/bobbuy/bobbuy/backend && mvn test` | PASS | 本次沙箱执行通过 |
| 前端单测 | `cd /home/runner/work/bobbuy/bobbuy/frontend && npm ci && npm test` | PASS | `22` 个测试文件、`74` 个用例通过；保留已知 warning 噪声 |
| 前端构建 | `cd /home/runner/work/bobbuy/bobbuy/frontend && npm run build` | PASS | 生产构建通过 |
| Playwright smoke | `cd /home/runner/work/bobbuy/bobbuy/frontend && npm run e2e` | PASS | `46 passed / 2 skipped`；`2 skipped` 为 AI 专用门控用例 |

Playwright 产物路径：

- `frontend/playwright-report/`
- `frontend/test-results/`

---

## 4. AI sample 脚本硬化与本地自检

### 4.1 脚本修复

已完成：

1. 为字段级比对增加实际路径别名映射：
   - `basePrice -> price`
2. 规范化 optional field path：
   - `expected.existingProductId` 会在比较前归一到 `existingProductId`
3. JSON report 字段结果现在包含：
   - `expectedPath`
   - `actualPath`
   - `expected`
   - `actual`
   - `passed`
   - `reason`
4. Markdown report 新增“实际读取路径”列。

### 4.2 Mock dry-run 自检

命令：

```bash
pwsh -NoProfile -Command "& '/home/runner/work/bobbuy/bobbuy/scripts/verify-ai-onboarding-samples.ps1' \
  -MockScanResponsePath '/home/runner/work/bobbuy/bobbuy/docs/fixtures/ai-onboarding-sample-scan-mock.json' \
  -SampleIds @('IMG_1484.jpg','IMG_1638.jpg') \
  -IncludeNeedsHumanGolden \
  -JsonReportPath '/tmp/plan40-ai-sample-report.json' \
  -MarkdownReportPath '/tmp/plan40-ai-sample-report.md'"
```

结果：

- `IMG_1484.jpg`：PASS，证明 `expected.basePrice=2698` 可从实际 `price=2698` 读取。
- `IMG_1638.jpg`：PASS，证明 `expected.existingProductId` optional path 规范化后可正确识别为 `optional-missing`，不会因 `expected.` 前缀漂移误判。

产物：

- `/tmp/plan40-ai-sample-report.json`
- `/tmp/plan40-ai-sample-report.md`

### 4.3 真实专用环境 sample 实扫

命令（未在本沙箱完成）：

```bash
pwsh /home/runner/work/bobbuy/bobbuy/scripts/verify-ai-onboarding-samples.ps1 -IncludeNeedsHumanGolden
```

阻塞原因：

1. 当前沙箱没有可达的真实 `/api/ai/onboard/scan` 服务。
2. 未配置真实 OCR provider / LLM provider / seed 数据。
3. 因此本次只能提供脚本级 dry-run 证据，不能把真实 sample 实扫写成通过。

---

## 5. AI 专用环境 E2E 结果

命令（未在本沙箱作为真实门禁执行）：

```bash
cd /home/runner/work/bobbuy/bobbuy/frontend
RUN_AI_VISION_E2E=1 npm run e2e:ai
```

状态：**BLOCKED**

阻塞原因：

1. PLAN-40 要求真实 AI/OCR provider、可达后端 `/api/ai/onboard/scan`、seed 数据与样本图片。
2. 当前沙箱只验证了默认 Playwright smoke；若在无真实 provider 的本地 mock 上执行，会形成误导性“通过”。
3. 因此本报告继续将 `e2e:ai` 记为发版阻断项，需由专用环境执行并归档 HTML report / trace / screenshot / video。

---

## 6. 安全与依赖门禁

### 6.1 npm audit

命令：

```bash
cd /home/runner/work/bobbuy/bobbuy/frontend
npm audit --json
```

结果摘要：

- `critical`: 3
- `high`: 10
- `moderate`: 4
- `low`: 0

结论：

- 前端依赖审计**未通过放行标准**，需在发版前由负责同学分类 blocking / non-blocking，并输出修复或豁免结论。

### 6.2 CodeQL / 仓库级安全扫描

状态：**BLOCKED**

说明：

1. 当前仓库 `.github/workflows` 仅发现 `ci.yml`，未发现现成 CodeQL workflow。
2. 本沙箱未持有仓库级 GitHub Advanced Security 配置权限，不能补造“已执行”的仓库级结果。
3. 发版前需要在 GitHub 仓库安全能力已开启的环境中执行 CodeQL 或等效代码扫描，并把结果写入 Release/PR 记录。

### 6.3 Maven 依赖审计

状态：**BLOCKED**

说明：

1. 当前项目未内置 Maven dependency-check / OWASP 审计插件。
2. 本次未新增第三方依赖，因此没有额外依赖引入风险变更。
3. 发版前仍需在具备扫描工具的环境中执行 Maven 依赖审计，并登记高危依赖处置结论。

---

## 7. Flyway 迁移、adoption 与回滚演练

### 7.1 空库 migrate / validate

命令：

```bash
cd /home/runner/work/bobbuy/bobbuy
docker compose up -d postgres

cd /home/runner/work/bobbuy/bobbuy/backend
mvn -Dflyway.url=jdbc:postgresql://localhost:5432/bobbuy \
  -Dflyway.user=bobbuy \
  -Dflyway.password=bobbuypassword \
  -Dflyway.cleanDisabled=false \
  flyway:clean flyway:migrate flyway:validate
```

结果：PASS

- 成功校验并执行 `3` 个 migration
- schema 版本到达 `v3`

### 7.2 备份 / 恢复验证

命令摘要：

```bash
cd /home/runner/work/bobbuy/bobbuy
docker compose exec -T postgres pg_dump -U "${POSTGRES_USER:-bobbuy}" -d "${POSTGRES_DB:-bobbuy}" --clean --if-exists --no-owner --no-privileges > /tmp/plan40-backup/bobbuy.sql
docker compose exec -T postgres psql -U "${POSTGRES_USER:-bobbuy}" -d postgres -c "CREATE DATABASE bobbuy_restore_verify_plan40;"
cat /tmp/plan40-backup/bobbuy.sql | docker compose exec -T postgres psql -U "${POSTGRES_USER:-bobbuy}" -d bobbuy_restore_verify_plan40
docker compose exec -T postgres psql -U "${POSTGRES_USER:-bobbuy}" -d bobbuy_restore_verify_plan40 -c "\dt"
```

结果：PASS

- 恢复库 `bobbuy_restore_verify_plan40` 可成功导入
- 恢复后可见 `19` 张表，包括 `flyway_schema_history`

### 7.3 旧库 adoption / baseline-on-migrate

状态：**BLOCKED**

阻塞原因：

1. 当前沙箱没有真实旧库副本或历史 schema dump。
2. 无法在不知道真实旧库差异的前提下，诚实宣告 baseline-on-migrate 已完成。
3. 本次已补 Runbook 步骤与责任边界，但发版前仍需在旧库副本上执行一次 adoption 演练并记录结果。

---

## 8. 发版阻断项与责任边界

| 项目 | 当前状态 | 是否阻断发版 | 发版前责任边界 |
| :-- | :-- | :-- | :-- |
| 真实 AI sample 实扫 | 未执行 | 是 | 由具备真实 `/api/ai/onboard/scan`、OCR/LLM、seed 数据的专用环境执行 |
| `npm run e2e:ai` 真实视觉验收 | 未执行 | 是 | 由专用环境执行并归档 Playwright artifact |
| CodeQL / 等效代码扫描 | 未执行 | 是 | 由仓库管理员或安全负责人在 GitHub 安全能力可用环境执行 |
| Maven 依赖审计 | 未执行 | 是 | 由后端/平台负责人在具备扫描工具的环境执行 |
| npm audit 高危依赖处置 | 已发现高危项 | 是 | 由前端负责人分类 blocking / non-blocking 并完成修复或豁免 |
| 真实旧库 adoption 演练 | 未执行 | 是 | 由 DBA / 发布负责人在旧库副本环境执行 baseline / validate / restore drill |

---

## 9. 本次交付物

1. `scripts/verify-ai-onboarding-samples.ps1`
2. `docs/fixtures/ai-onboarding-sample-scan-mock.json`
3. `docs/reports/REPORT-04-发版候选门禁验收报告.md`
4. README、CURRENT STATE、TEST MATRIX、PLAN-00、PLAN-24、Runbook 同步更新
