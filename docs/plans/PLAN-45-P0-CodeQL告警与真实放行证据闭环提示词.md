# PLAN-45: P0 CodeQL 告警与真实放行证据闭环提示词

**生效日期**: 2026-05-01
**状态**: 待执行
**关联计划**:
- [PLAN-44: P0 默认 CI 恢复、真实环境放行证据与 REPORT-07 复判提示词](PLAN-44-P0-真实环境放行证据与REPORT07复判提示词.md)
- [PLAN-41: P0 发版阻断项处置与安全审计提示词](PLAN-41-P0-发版阻断项处置与安全审计提示词.md)
- [PLAN-24: 稳定上线差距收口优先级](PLAN-24-稳定上线差距收口优先级.md)

**当前事实基线**:
- 最新 main 默认 CI run `25192905348` 已成功。
- 最新 main CodeQL run `25192905342` 已成功，matrix `actions` / `javascript-typescript` / `java-kotlin` 均通过。
- Code scanning alerts API 当前返回 3 个 open high alert：
  - `backend/src/main/java/com/bobbuy/SecurityConfig.java`: `java/spring-disabled-csrf-protection`
  - `docs/design/assets/ui-merchant-framework.js`: `js/xss-through-dom` x2
- Maven dependency-check main run `25177731775` 已成功，但报告实际写入 `backend/target/dependency-check-report.*`；原 workflow 上传 `/tmp/bobbuy-dependency-check`，导致无 artifact。本轮已修正 workflow 上传路径，需重跑确认。
- 真实 AI/OCR sample、真实 `RUN_AI_VISION_E2E=1 npm run e2e:ai`、真实旧库 Flyway adoption / restore drill 仍缺可信证据。
- `REPORT-07` 当前仍为 **NO_GO**。

---

## 1. 任务背景

PLAN-44 已把默认 CI、CodeQL main push 与 Maven dependency-check 执行链路推进到可运行状态，但发版仍不能放行。原因已经从“门禁无法跑起来”转为“门禁跑出了必须处置的证据缺口”：

1. CodeQL 有 3 个 open high alert。
2. Maven dependency-check 成功但 artifact 归档失败，无法审计报告内容。
3. 真实 AI/OCR、真实 AI E2E、真实旧库 adoption 仍未执行。

下一轮必须先处理可审计安全风险，再补真实运行环境证据；否则 `REPORT-07` 不能从 `NO_GO` 变为 `GO` 或 `CONDITIONAL_GO`。

---

## 2. 核心 Prompt 文本（请审查后执行）

```text
### [任务: P0 CodeQL 告警处置、依赖审计 artifact 归档与真实放行证据闭环]

任务背景：
BOBBuy 最新 main 默认 CI、CodeQL main push 与 Maven dependency-check 已能成功执行，但 CodeQL 仍有 3 个 open high alert，dependency-check 成功 run 没有上传 artifact，真实 AI/OCR、真实 e2e:ai 与真实旧库 adoption 仍缺证据。当前 REPORT-07 仍为 NO_GO。请优先修复可审计安全缺口，再补真实环境证据，并更新 REPORT-07 做新一轮放行复判。

目标：
1. 修复或正式豁免 3 个 CodeQL open high alert。
2. 重跑 Maven dependency-check，确保 HTML/JSON artifact 可下载，并记录严重级别摘要。
3. 执行真实 AI/OCR sample 字段级 gate。
4. 执行真实 `RUN_AI_VISION_E2E=1 npm run e2e:ai` 或 AI release evidence workflow。
5. 执行真实旧库 Flyway adoption / restore drill。
6. 更新 REPORT-07、PLAN-00、PLAN-24、CURRENT STATE、TEST MATRIX 与 README。
7. 给出新的 GO / CONDITIONAL_GO / NO_GO 结论。

执行范围 A：CodeQL open high alert 处置
1. 拉取最新 main 并确认 CodeQL run：
   - run URL: https://github.com/sunshaoxuan/bobbuy/actions/runs/25192905342
   - commit: a66d0866cfe805d0c322bc508aa044e493fa48d4
2. 通过 GitHub code scanning alerts API 或页面确认当前 open alerts：
   - `backend/src/main/java/com/bobbuy/SecurityConfig.java` / `java/spring-disabled-csrf-protection`
   - `docs/design/assets/ui-merchant-framework.js` / `js/xss-through-dom` x2
3. 对 Spring CSRF alert：
   - 先判断当前 REST/JWT + HttpOnly refresh cookie + double-submit CSRF 设计是否确实覆盖 cookie 写操作。
   - 优先实现 Spring Security 层面的 CSRF 配置，只忽略不需要 CSRF 的 bearer-token/stateless API，保护 `/api/auth/refresh`、`/api/auth/logout` 等 cookie 驱动端点。
   - 如果选择豁免，必须说明为什么应用层 double-submit 已充分覆盖，且豁免必须包含负责人、到期时间和复核计划。
4. 对 docs/design DOM XSS alert：
   - 优先去掉 `outerHTML` / HTML 字符串注入，改为 `template` + `content.cloneNode(true)`、DOM API 构造节点，或对动态字段进行严格转义。
   - 如果该文件只是离线设计资产，也不得简单忽略 high alert；需要修复或从 CodeQL 扫描范围中以明确理由排除，并在文档中登记。
5. 修复后重跑：
   - `cd backend && mvn test`
   - `cd frontend && npm test && npm run build`
   - CodeQL workflow
6. 只有 open high alert 清零或形成正式批准豁免后，安全门禁才可视为通过。

执行范围 B：Maven dependency-check artifact 闭环
1. 使用已修正的 `.github/workflows/dependency-check.yml` 重跑 Maven dependency-check。
2. 确认 artifact `dependency-check-report` 存在，并包含：
   - `dependency-check-report.html`
   - `dependency-check-report.json`
3. 读取 JSON 摘要并记录：
   - critical 数量
   - high 数量
   - moderate 数量
   - 是否存在 runtime dependency 风险
4. 如果 critical/high 大于 0：
   - 默认阻断发版。
   - 修复依赖或给出正式豁免。豁免必须有负责人、到期时间、不可利用理由和复测计划。

执行范围 C：真实 AI/OCR sample 字段级 gate
1. 使用真实后端、真实 OCR、真实 LLM/Codex Bridge、真实 seed。
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
3. gate 模式必须返回 0。
4. 失败时保留报告并定位 OCR、LLM、字段归一化、seed、供应商规则、网络、权限或 golden 漂移根因。

执行范围 D：真实 AI E2E
1. 使用与 sample gate 相同的后端/OCR/LLM/seed。
2. 执行：
   ```bash
   cd frontend
   RUN_AI_VISION_E2E=1 npm run e2e:ai
   ```
3. 或手动触发 `.github/workflows/ai-release-evidence.yml`。
4. 归档 Playwright HTML report、trace、screenshot、video 和 test-results。

执行范围 E：真实旧库 Flyway adoption / restore drill
1. 准备脱敏真实旧库副本或历史 schema dump。
2. 在隔离 PostgreSQL 环境执行：
   - pre-backup
   - restore
   - baseline / baseline-on-migrate 方案评估
   - migrate
   - validate
   - restore drill
3. 记录旧库来源、dump 时间、脱敏方式、PostgreSQL 版本、migration 版本、`flyway_schema_history` 状态。

执行范围 F：REPORT-07 复判与文档同步
1. 更新：
   - `docs/reports/REPORT-07-NO-GO阻断项解阻与放行复判.md`
   - `docs/plans/PLAN-00-任务看板总览.md`
   - `docs/plans/PLAN-24-稳定上线差距收口优先级.md`
   - `docs/reports/CURRENT-STATE-2026-04-28.md`
   - `docs/reports/TEST-MATRIX-本地与CI执行矩阵.md`
   - `README.md`
2. REPORT-07 必须包含：
   - 默认 CI run
   - CodeQL run、alert 数、修复或豁免结论
   - Maven dependency-check run、artifact URL、严重级别摘要
   - 真实 AI sample gate 报告
   - 真实 AI E2E artifact
   - 真实旧库 adoption / restore drill 记录
   - 最终 GO / CONDITIONAL_GO / NO_GO 判定
3. 判定规则：
   - GO：默认 CI、CodeQL、Maven dependency-check、真实 AI sample、真实 e2e:ai、真实旧库 adoption 全部通过，且无未处置 critical/high 风险。
   - CONDITIONAL_GO：仅剩非 runtime 阻断项，且已有负责人、期限与批准记录。
   - NO_GO：任一真实运行证据缺失、失败，或存在未处置 critical/high 风险。

验收命令：
1. `docker compose config`
2. `cd backend && mvn test`
3. `cd frontend && npm ci && npm test && npm run build`
4. `docker build backend -t bobbuy-backend-test`
5. `docker build frontend -t bobbuy-frontend-test`
6. CodeQL workflow 成功且 open high alert 清零或正式豁免
7. Maven dependency-check artifact 可下载且摘要已登记
8. 真实 sample gate 命令
9. `RUN_AI_VISION_E2E=1 npm run e2e:ai`
10. 真实旧库 Flyway adoption / restore drill

禁止事项：
1. 不得把 CodeQL high alert 简单标记为通过。
2. 不得把 dependency-check run success 当作 artifact 已归档。
3. 不得用 mock sample dry-run 替代真实 sample gate。
4. 不得用默认 `npm run e2e` 替代 `RUN_AI_VISION_E2E=1 npm run e2e:ai`。
5. 不得用空库 migration 替代真实旧库 adoption。
6. 不得提交明文 API key、数据库 dump、旧库原始数据或可识别个人数据。
7. 不得回滚非本任务相关的用户改动。
```

---

## 3. 审核重点

1. 是否把 CodeQL open high alert 放在 GO 前置条件。
2. 是否区分 dependency-check “run 成功”和“artifact 可审计归档成功”。
3. 是否继续禁止 mock/dry-run 替代真实 AI/OCR 与旧库证据。
4. 是否为豁免设置负责人、期限、不可利用理由和复测计划。
5. 是否明确当前在所有 blocker 完成前仍为 `NO_GO`。

---

## 4. 预期交付物

1. CodeQL alert 修复 PR 或正式豁免记录。
2. Maven dependency-check HTML/JSON artifact。
3. 真实 AI sample JSON/Markdown 报告。
4. 真实 AI E2E Playwright artifact。
5. 真实旧库 adoption / restore drill 记录。
6. 更新后的 `REPORT-07` 与相关文档。

---

## 5. 当前核准状态

**当前状态**: 待用户审查批准后执行。
