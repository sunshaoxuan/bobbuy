# PLAN-44: P0 默认 CI 恢复、真实环境放行证据与 REPORT-07 复判提示词

**生效日期**: 2026-05-01  
**状态**: 执行中
**关联计划**:
- [PLAN-43: P0 NO-GO 阻断项执行解阻提示词](PLAN-43-P0-NO-GO阻断项执行解阻提示词.md)
- [PLAN-42: P0 专用环境发版证据执行提示词](PLAN-42-P0-专用环境发版证据执行提示词.md)
- [PLAN-24: 稳定上线差距收口优先级](PLAN-24-稳定上线差距收口优先级.md)

**当前事实基线**:
- PLAN-43 已完成：CodeQL JS/TS build mode、重复触发、Maven dependency-check workflow、AI evidence workflow 已落地。
- `REPORT-07` 已形成复判，但结论仍为 **NO_GO**。
- 最新 CodeQL run `25177727147` 已成功。
- 最新 main `BOBBuy CI` run `25177731792` 中 `backend-test`、`frontend-quality` 通过，但 `docker-build` 的 frontend image 在 `npm install` 阶段因 `ECONNRESET` 失败。
- 已将 `frontend/Dockerfile` 切换为 `npm ci` 并增加 npm fetch retry；本地 `docker build frontend -t bobbuy-frontend-test` 已通过，仍需重跑 GitHub Actions 确认默认 CI。
- main 上 Maven dependency-check run `25177731775` 已触发，当前需要等待完成并归档 HTML/JSON artifact。
- AI release evidence workflow 尚无 run；真实 AI/OCR sample、真实 `e2e:ai` 与真实旧库 adoption 仍未形成可审计通过证据。

---

## 1. 任务背景

刚合入 PR #60 后，原先“workflow 不存在 / 无法触发”的问题已经转化为更具体的放行问题：

1. 默认 CI 必须重新恢复为全绿，当前阻断点是 frontend Docker build 网络抖动。
2. CodeQL 已通过，但需要把最新 run 与 alerts 结论写入放行证据。
3. Maven dependency-check 已有独立 workflow 和 NVD key/cache，仍需等待 main run 完成并归档报告。
4. 真实 AI/OCR sample gate 和真实 `e2e:ai` 尚未执行。
5. 真实旧库 Flyway adoption / restore drill 尚未执行。

下一轮不能只补文档；必须先恢复默认 CI，再补齐真实运行证据，并更新 `REPORT-07` 给出新的 GO / CONDITIONAL_GO / NO_GO 复判。

---

## 2. 核心 Prompt 文本（请审查后执行）

```text
### [任务: P0 默认 CI 恢复、真实环境放行证据补齐与 REPORT-07 复判]

任务背景：
BOBBuy 最新 main 已合入 PLAN-43 相关自动化，但当前发版候选仍以 REPORT-07 的 NO_GO 为正式基线。CodeQL 已成功，Maven dependency-check 已触发，AI evidence workflow 已存在；但 main 默认 CI 的 docker-build 曾因 frontend Docker build 内部 npm 网络 ECONNRESET 失败，真实 AI/OCR、真实 e2e:ai 与真实旧库 adoption 仍缺证据。请先恢复默认 CI，再补齐真实环境证据，最终更新 REPORT-07。

目标：
1. 恢复最新 main 默认 CI 全绿，尤其是 frontend Docker image build。
2. 归档最新 CodeQL run 与 code scanning alerts 结论。
3. 等待并归档 main 上 Maven dependency-check HTML/JSON artifact。
4. 在真实后端 + 真实 OCR + 真实 LLM/Codex Bridge + 真实 seed 环境执行 sample 字段级 gate。
5. 在同一套 AI/OCR/seed 环境执行 `RUN_AI_VISION_E2E=1 npm run e2e:ai`。
6. 在真实旧库副本或历史 schema dump 上完成 Flyway adoption / restore drill。
7. 更新 `docs/reports/REPORT-07-NO-GO阻断项解阻与放行复判.md`，给出 GO / CONDITIONAL_GO / NO_GO。
8. 同步 README、CURRENT STATE、TEST MATRIX、PLAN-00、PLAN-24。

执行范围 A：默认 CI 恢复
1. 检查最新 `BOBBuy CI` main run：
   - run URL
   - commit SHA
   - `backend-test`
   - `frontend-quality`
   - `docker-build`
2. 若 frontend Docker build 仍失败：
   - 查看 Docker build 日志。
   - 优先使用 `npm ci` 而不是 `npm install`。
   - 增加 npm fetch retry 参数，避免短时 registry 抖动导致门禁失败。
   - 保持 Dockerfile 仍使用 lockfile，不引入未锁定依赖。
3. 本地/CI 验证：
   ```bash
   cd frontend && npm ci && npm test && npm run build
   docker build frontend -t bobbuy-frontend-test
   docker build backend -t bobbuy-backend-test
   ```
4. 默认 CI 未全绿前，不得写 GO 或 CONDITIONAL_GO。

执行范围 B：安全扫描证据归档
1. 记录最新 CodeQL：
   - run URL
   - commit SHA
   - java-kotlin / javascript-typescript / actions 结果
   - code scanning alerts 数量
2. 记录最新 Maven dependency-check：
   - run URL
   - artifact 名称
   - HTML/JSON report 路径
   - critical/high/moderate 摘要
3. 如果 dependency-check 仍在运行：
   - 记录当前状态为 `in_progress`。
   - 不得将其写成通过。
4. 如果发现 critical/high：
   - 默认阻断发版。
   - 修复或形成明确豁免，豁免必须包含责任人、到期时间和不可利用理由。

执行范围 C：真实 AI/OCR sample 字段级 gate
1. 确认环境变量和服务：
   - 后端 URL
   - OCR provider 与版本
   - LLM provider：`codex-bridge` / Ollama / 其他兼容 gateway
   - `BOBBUY_AI_LLM_CODEX_BRIDGE_URL` 是否指向 OpenAI-compatible `/v1`
   - seed 数据版本与 sample 图片目录版本
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
4. 如果失败：
   - 保留 JSON/Markdown 报告。
   - 对每个失败字段标出 expectedPath、actualPath、expected、actual、reason。
   - 判断根因属于 OCR、LLM prompt、字段归一化、seed、供应商规则、网络、权限、golden 漂移或产品代码。
   - 只允许修复真实根因，不允许把 golden 改到掩盖错误。

执行范围 D：真实 AI E2E
1. 使用与 sample gate 相同的 backend/OCR/LLM/seed 环境。
2. 执行：
   ```bash
   cd frontend
   RUN_AI_VISION_E2E=1 npm run e2e:ai
   ```
3. 或手动触发 `.github/workflows/ai-release-evidence.yml`，输入真实 `backend_base_url` 并配置 `AI_E2E_AGENT_PASSWORD`。
4. 必须归档：
   - `frontend/playwright-report/`
   - `frontend/test-results/`
   - trace / screenshot / video
5. 如果失败：
   - 保留 artifact。
   - 和 sample 字段级报告交叉分析，避免只修 UI 不修识别事实链。

执行范围 E：真实旧库 Flyway adoption / restore drill
1. 获取真实旧库副本或历史 schema dump，必须脱敏并在隔离数据库执行。
2. 记录：
   - 旧库来源
   - dump 时间
   - 脱敏方式
   - PostgreSQL 版本
   - migration 版本
   - `flyway_schema_history` 状态
3. 执行：
   - pre-backup
   - restore 到隔离库
   - baseline 或 baseline-on-migrate 方案评估
   - migrate
   - validate
   - restore drill
4. 如果没有真实旧库副本：
   - 明确负责人、数据来源、预计交付日期。
   - REPORT-07 必须保持 NO_GO 或最多 CONDITIONAL_GO，不能写 GO。

执行范围 F：REPORT-07 放行复判
1. 更新：
   - `docs/reports/REPORT-07-NO-GO阻断项解阻与放行复判.md`
2. REPORT-07 必须包含：
   - 执行日期、分支、提交 SHA
   - 默认 CI 最新 run
   - CodeQL 证据
   - Maven dependency-check 证据
   - AI sample gate 证据
   - AI E2E 证据
   - 旧库 adoption / restore drill 证据
   - 最终判定：GO / CONDITIONAL_GO / NO_GO
3. 判定规则：
   - GO：默认 CI、CodeQL、Maven dependency-check、真实 AI sample、真实 e2e:ai、真实旧库 adoption 全部通过，且无 critical/high 未处置风险。
   - CONDITIONAL_GO：仅剩非 runtime 阻断项，且已有负责人、期限与批准记录。
   - NO_GO：默认 CI、真实 AI/OCR、真实 e2e:ai、旧库 adoption 任一未执行、失败或无可信证据。

执行范围 G：文档同步
1. 更新：
   - README.md
   - docs/reports/CURRENT-STATE-2026-04-28.md
   - docs/reports/TEST-MATRIX-本地与CI执行矩阵.md
   - docs/plans/PLAN-00-任务看板总览.md
   - docs/plans/PLAN-24-稳定上线差距收口优先级.md
2. 文档必须明确：
   - 哪些 blocker 已解阻。
   - 哪些仍未完成。
   - 当前是否允许试运行发版。

验收命令：
1. `docker compose config`
2. `cd backend && mvn test`
3. `cd frontend && npm ci && npm test && npm run build`
4. `docker build backend -t bobbuy-backend-test`
5. `docker build frontend -t bobbuy-frontend-test`
6. 真实 sample gate 命令
7. `RUN_AI_VISION_E2E=1 npm run e2e:ai`
8. 真实旧库 Flyway adoption / restore drill 命令

禁止事项：
1. 不得用 mock sample dry-run 替代真实 sample gate。
2. 不得用默认 `npm run e2e` 替代 `RUN_AI_VISION_E2E=1 npm run e2e:ai`。
3. 不得用空库 migrate/validate 替代真实旧库 adoption。
4. 不得把无法执行写成通过。
5. 不得提交明文 API key、数据库 dump 或任何可识别个人数据。
6. 不得回滚非本任务相关的用户改动。
```

---

## 3. 审核重点

1. 是否把默认 CI 全绿重新列为 GO 前置条件。
2. 是否把真实 AI/OCR sample、真实 `e2e:ai`、真实旧库 adoption 放在 GO 前置条件。
3. 是否明确 CodeQL 已通过，但 Maven dependency-check 仍需等待最新 main run 和 artifact。
4. 是否防止用 mock、dry-run、空库 migration 替代真实证据。
5. 是否要求 REPORT-07 给出可审计的 GO / CONDITIONAL_GO / NO_GO 判定。
6. 是否明确禁止提交明文 key、旧库 dump 或个人数据。

---

## 4. 预期交付物

1. 最新 main 默认 CI 全绿 run URL。
2. `REPORT-07-NO-GO阻断项解阻与放行复判.md` 更新。
3. Maven dependency-check HTML/JSON artifact 引用。
4. 真实 AI sample JSON/Markdown 报告。
5. 真实 AI E2E Playwright artifact。
6. 真实旧库 adoption / restore drill 记录。
7. README、CURRENT STATE、TEST MATRIX、PLAN-00、PLAN-24 同步更新。

---

## 5. 当前核准状态

**当前状态**: 已按 2026-05-01 merge 后事实重写，等待执行。
