# PLAN-44: P0 真实环境放行证据与 REPORT-07 复判提示词

**生效日期**: 2026-05-01  
**状态**: 待执行  
**关联计划**:
- [PLAN-43: P0 NO-GO 阻断项执行解阻提示词](PLAN-43-P0-NO-GO阻断项执行解阻提示词.md)
- [PLAN-42: P0 专用环境发版证据执行提示词](PLAN-42-P0-专用环境发版证据执行提示词.md)
- [PLAN-24: 稳定上线差距收口优先级](PLAN-24-稳定上线差距收口优先级.md)

**当前事实基线**:
- CodeQL JS/TS build mode 已修正，PR #60 最新 CodeQL run `25148614578` 已通过。
- Maven dependency-check 已配置 NVD API key 与 data cache，PR #60 最新 `Maven dependency-check / dependency-check` 已通过。
- Sample 脚本已修复 `basePrice -> price` 字段别名、gate/report-only 语义与失败非零退出。
- Refresh token 并发轮换已通过悲观写锁与并发集成测试收口。
- Codex Bridge 已接入 LLM fallback，并完成 `/v1/models` 与 `/v1/chat/completions` 最小连通性验证。
- `REPORT-06` 的正式结论仍为 `NO_GO`，因为真实 AI/OCR、真实 `e2e:ai` 与真实旧库 adoption 尚未形成可审计证据。

---

## 1. 任务背景

当前已经不应继续把工作重点放在“workflow 是否存在”或“安全扫描能否启动”上。CodeQL 与 Maven dependency-check 已经进入可执行状态，剩余发版差距集中在真实运行环境：

1. 真实 `/api/ai/onboard/scan` sample 字段级 gate 尚未通过。
2. 真实 `RUN_AI_VISION_E2E=1 npm run e2e:ai` 尚未通过。
3. 真实旧库副本或历史 schema dump 的 Flyway adoption / restore drill 尚未完成。
4. `REPORT-07` 尚未把最新 CodeQL、Maven、真实 AI/OCR、旧库 adoption 证据统一成 GO / CONDITIONAL_GO / NO_GO 复判。

下一轮任务的目标是拿到真实证据，而不是继续补 mock、dry-run 或口头说明。

---

## 2. 核心 Prompt 文本（请审查后执行）

```text
### [任务: P0 真实环境放行证据补齐与 REPORT-07 复判]

任务背景：
BOBBuy 当前发版候选仍以 REPORT-06 的 NO_GO 为正式基线。CodeQL 与 Maven dependency-check 已经解阻并在 PR #60 形成通过证据；sample 验证脚本、Refresh Token 并发、Codex Bridge 也已完成代码与最小验证。下一步必须集中补齐真实 AI/OCR、真实 AI E2E、真实旧库 adoption，并新增 REPORT-07 给出放行复判。

目标：
1. 在真实后端 + 真实 OCR + 真实 LLM/Codex Bridge + 真实 seed 环境执行 sample 字段级 gate。
2. 在同一套 AI/OCR/seed 环境执行 `RUN_AI_VISION_E2E=1 npm run e2e:ai`。
3. 在真实旧库副本或历史 schema dump 上完成 Flyway adoption / restore drill。
4. 归档 CodeQL 与 Maven dependency-check 最新通过证据。
5. 新增 `docs/reports/REPORT-07-NO-GO阻断项解阻与放行复判.md`，给出 GO / CONDITIONAL_GO / NO_GO。
6. 同步 README、CURRENT STATE、TEST MATRIX、PLAN-00、PLAN-24。

执行范围 A：真实 AI/OCR sample 字段级 gate
1. 确认环境变量和服务：
   - 后端 URL
   - OCR provider 与版本
   - LLM provider：优先记录 `codex-bridge` / Ollama / 其他兼容 gateway
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

执行范围 B：真实 AI E2E
1. 使用与 sample gate 相同的 backend/OCR/LLM/seed 环境。
2. 执行：
   ```bash
   cd frontend
   RUN_AI_VISION_E2E=1 npm run e2e:ai
   ```
3. 必须归档：
   - `frontend/playwright-report/`
   - `frontend/test-results/`
   - trace / screenshot / video
4. 如果失败：
   - 保留 artifact。
   - 和 sample 字段级报告交叉分析，避免只修 UI 不修识别事实链。

执行范围 C：真实旧库 Flyway adoption / restore drill
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

执行范围 D：安全扫描证据归档
1. 记录 PR #60 或最新 main/PR 的 CodeQL run：
   - run URL
   - commit SHA
   - java-kotlin / javascript-typescript / actions 结果
   - code scanning alerts 数量
2. 记录 Maven dependency-check：
   - run URL
   - artifact 名称
   - HTML/JSON 报告路径
   - critical/high/moderate 摘要
3. 如果发现 critical/high：
   - 默认阻断发版。
   - 修复或形成明确豁免，豁免必须包含责任人、到期时间和不可利用理由。

执行范围 E：REPORT-07 放行复判
1. 新增：
   - `docs/reports/REPORT-07-NO-GO阻断项解阻与放行复判.md`
2. REPORT-07 必须包含：
   - 执行日期、分支、提交 SHA
   - CodeQL 证据
   - Maven dependency-check 证据
   - AI sample gate 证据
   - AI E2E 证据
   - 旧库 adoption / restore drill 证据
   - 最终判定：GO / CONDITIONAL_GO / NO_GO
3. 判定规则：
   - GO：五类证据全部通过，且无 critical/high 未处置风险。
   - CONDITIONAL_GO：仅剩非 runtime 阻断项，且已有负责人、期限与批准记录。
   - NO_GO：真实 AI/OCR、真实 e2e:ai、旧库 adoption 任一未执行、失败或无可信证据。

执行范围 F：文档同步
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
3. `cd frontend && npm test`
4. `cd frontend && npm run build`
5. 真实 sample gate 命令
6. `RUN_AI_VISION_E2E=1 npm run e2e:ai`
7. 真实旧库 Flyway adoption / restore drill 命令

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

1. 是否把真实 AI/OCR sample、真实 `e2e:ai`、真实旧库 adoption 放在 GO 前置条件。
2. 是否明确 CodeQL / Maven 已从“阻断执行”转为“证据归档与风险处置”。
3. 是否防止用 mock、dry-run、空库 migration 替代真实证据。
4. 是否要求 REPORT-07 给出可审计的 GO / CONDITIONAL_GO / NO_GO 判定。
5. 是否明确禁止提交明文 key、旧库 dump 或个人数据。

---

## 4. 预期交付物

1. `REPORT-07-NO-GO阻断项解阻与放行复判.md`
2. 真实 AI sample JSON/Markdown 报告
3. 真实 AI E2E Playwright artifact
4. 真实旧库 adoption / restore drill 记录
5. CodeQL run 与 Maven dependency-check artifact 引用
6. README、CURRENT STATE、TEST MATRIX、PLAN-00、PLAN-24 同步更新

---

## 5. 当前核准状态

**当前状态**: 待用户审查批准后执行。
