# PLAN-46: P0 依赖高危处置与真实环境证据闭环提示词

**生效日期**: 2026-05-01
**状态**: 待执行
**关联计划**:
- [PLAN-45: P0 CodeQL 告警与真实放行证据闭环提示词](PLAN-45-P0-CodeQL告警与真实放行证据闭环提示词.md)
- [PLAN-44: P0 默认 CI 恢复、真实环境放行证据与 REPORT-07 复判提示词](PLAN-44-P0-真实环境放行证据与REPORT07复判提示词.md)
- [PLAN-24: 稳定上线差距收口优先级](PLAN-24-稳定上线差距收口优先级.md)

**当前事实基线**:
- 最新 main 默认 CI run `25198280095` 已成功。
- 最新 main CodeQL run `25198280107` 已成功，3 个 high alert 均已标记 `fixed`。
- 最新 main Maven dependency-check run `25198280108` 已成功，artifact `dependency-check-report`（id `6744112430`）可下载，包含 HTML/JSON。
- dependency-check 摘要仍为 `8 critical / 21 high / 19 moderate`（unique CVE 口径），尚未完成修复、升级或正式豁免。
- 真实 compose 栈尝试仍被 `Dockerfile.service` 容器内 Maven 访问 `repo.maven.apache.org` 的 `PKIX path building failed` 阻塞。
- 真实 AI/OCR sample、真实 `RUN_AI_VISION_E2E=1 npm run e2e:ai`、真实旧库 Flyway adoption / restore drill 仍缺可信证据。
- `REPORT-07` 当前仍为 **NO_GO**。

---

## 1. 任务背景

PLAN-45 已把 CodeQL high alert 和 dependency-check artifact 的“证据链可执行性”推进到可审计状态。下一轮不应继续把重点放在 CodeQL workflow 本身，而应处理已经暴露出来的真实发布阻断：

1. Maven dependency-check 报告存在大量 critical/high 风险。
2. 真实 compose 栈无法拉起，原因是 service 镜像 Maven-in-Docker 出现 PKIX 证书链问题。
3. AI/OCR sample gate、真实 AI E2E、真实旧库 adoption 仍未执行。

下一轮目标是把安全风险和真实环境阻塞同时往前推进，避免只修文档或只跑 mock。

---

## 2. 核心 Prompt 文本（请审查后执行）

```text
### [任务: P0 依赖高危风险处置、Compose PKIX 解阻与真实环境证据闭环]

任务背景：
BOBBuy 最新 main 默认 CI、CodeQL、Maven dependency-check 均已成功。CodeQL 3 个 high alert 已在 main 上标记 fixed；dependency-check artifact 已可下载。但 dependency-check 仍报告 8 critical / 21 high / 19 moderate，真实 compose 栈仍因 Dockerfile.service 中 Maven 访问 Maven Central 的 PKIX path building failed 无法拉起，真实 AI/OCR sample、真实 e2e:ai 与真实旧库 adoption 仍缺证据。当前 REPORT-07 仍为 NO_GO。

目标：
1. 下载并解析 dependency-check artifact，按依赖、CVE、scope、runtime 可利用性分类。
2. 对 critical/high 风险完成升级、替换、排除、scope 调整或正式豁免。
3. 修复真实 compose 栈的 Maven PKIX 阻塞，确保服务镜像可构建并启动。
4. 在真实后端 + 真实 OCR + 真实 LLM/Codex Bridge + 真实 seed 环境执行 sample 字段级 gate。
5. 执行真实 `RUN_AI_VISION_E2E=1 npm run e2e:ai` 或 AI release evidence workflow。
6. 在真实旧库副本或历史 schema dump 上完成 Flyway adoption / restore drill。
7. 更新 REPORT-07、PLAN-00、PLAN-24、CURRENT STATE、TEST MATRIX 与 README，给出 GO / CONDITIONAL_GO / NO_GO 复判。

执行范围 A：dependency-check critical/high 处置
1. 从最新 main run 下载 artifact：
   - run: https://github.com/sunshaoxuan/bobbuy/actions/runs/25198280108
   - artifact: dependency-check-report / 6744112430
2. 解析 `dependency-check-report.json`，输出表格：
   - dependency / package
   - CVE
   - severity
   - affected version
   - fixed version 或 recommended action
   - scope：runtime / test / provided / plugin / transitive
   - 是否进入生产镜像
   - 处置方式：upgrade / exclude / dependencyManagement pin / suppress / accept
3. 对 runtime critical/high：
   - 默认必须修复或升级。
   - 若没有可用修复版本，必须形成正式豁免，包含负责人、到期时间、不可利用理由、缓解措施、复测计划。
4. 对 test/plugin/dev-only critical/high：
   - 可以豁免，但必须证明不进入 runtime classpath 和生产镜像。
5. 若使用 suppression file：
   - suppression 条目必须精确到 CVE + dependency，不得大范围忽略。
   - suppression 文件必须有注释说明责任人与到期时间。
6. 重跑：
   - `cd backend && mvn test`
   - Maven dependency-check workflow
7. 验收：
   - critical/high 清零，或所有剩余项均有正式豁免。

执行范围 B：Compose Maven PKIX 解阻
1. 复现：
   ```bash
   docker compose up -d postgres minio redis rabbitmq nacos nacos-init core-service ai-service im-service auth-service gateway-service frontend gateway ocr-service
   ```
2. 定位 `Dockerfile.service` 中 Maven-in-Docker 访问 Maven Central 的证书链问题。
3. 优先方案：
   - 使用已验证的主机 Maven 构建产物，再让服务镜像只复制 jar，避免 service 镜像构建阶段访问外网。
   - 或在 Dockerfile 中修复 CA 证书链，但不得引入不受控证书或关闭 TLS 校验。
4. 验收：
   - `docker compose build core-service ai-service im-service auth-service gateway-service`
   - `docker compose up -d ...`
   - `curl http://localhost/api/health` 或等价 gateway 健康检查可达。
5. 不得通过跳过证书校验、禁用 Maven TLS、改用 HTTP Maven 源解决。

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
3. gate 模式必须返回 0 才算通过。
4. 如果失败，保留 JSON/Markdown 报告，并定位 OCR、LLM、字段归一化、seed、供应商规则、网络、权限或 golden 漂移根因。

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
4. 如果仍没有真实旧库输入，REPORT-07 必须保持 NO_GO，不能用空库 migration 替代。

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
   - CodeQL run 与 fixed alert 证据
   - Maven dependency-check run、artifact URL、critical/high 处置表
   - Compose PKIX 解阻证据
   - 真实 AI sample gate 报告
   - 真实 AI E2E artifact
   - 真实旧库 adoption / restore drill 记录
   - 最终 GO / CONDITIONAL_GO / NO_GO 判定
3. 判定规则：
   - GO：默认 CI、CodeQL、dependency-check critical/high、真实 AI sample、真实 e2e:ai、真实旧库 adoption 全部通过或正式闭环。
   - CONDITIONAL_GO：仅剩非 runtime 风险，且已有负责人、期限与批准记录。
   - NO_GO：runtime critical/high 未处置、真实环境证据缺失、真实旧库 adoption 缺失，任一成立即保持 NO_GO。

验收命令：
1. `docker compose config`
2. `cd backend && mvn test`
3. `cd frontend && npm ci && npm test && npm run build`
4. `docker build backend -t bobbuy-backend-test`
5. `docker build frontend -t bobbuy-frontend-test`
6. `docker compose build core-service ai-service im-service auth-service gateway-service`
7. `docker compose up -d ...` 并健康检查通过
8. Maven dependency-check artifact 可下载，critical/high 清零或正式豁免
9. 真实 sample gate 命令
10. `RUN_AI_VISION_E2E=1 npm run e2e:ai`
11. 真实旧库 Flyway adoption / restore drill

禁止事项：
1. 不得把 dependency-check run success 当成安全风险已通过。
2. 不得大范围 suppress critical/high。
3. 不得关闭 TLS 校验或改 HTTP Maven 源来绕过 PKIX。
4. 不得用 mock sample dry-run 替代真实 sample gate。
5. 不得用默认 `npm run e2e` 替代 `RUN_AI_VISION_E2E=1 npm run e2e:ai`。
6. 不得用空库 migration 替代真实旧库 adoption。
7. 不得提交明文 API key、数据库 dump、旧库原始数据或可识别个人数据。
8. 不得回滚非本任务相关的用户改动。
```

---

## 3. 审核重点

1. 是否把 dependency-check critical/high 作为发版 blocker，而不只是记录 artifact。
2. 是否禁止用关闭 TLS 校验绕过 Maven PKIX。
3. 是否继续要求真实 AI/OCR 与真实旧库证据，防止再次退回 mock/dry-run。
4. 是否明确豁免必须有负责人、期限、不可利用理由与复测计划。
5. 是否保持 `REPORT-07` 在所有真实证据补齐前为 `NO_GO`。

---

## 4. 预期交付物

1. dependency-check critical/high 处置表与新 artifact。
2. Compose 服务镜像 PKIX 解阻记录。
3. 真实 AI sample JSON/Markdown 报告。
4. 真实 AI E2E Playwright artifact。
5. 真实旧库 adoption / restore drill 记录。
6. 更新后的 `REPORT-07` 与相关文档。

---

## 5. 当前核准状态

**当前状态**: 待用户审查批准后执行。
