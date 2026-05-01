# PLAN-48: P0 文档拉平与真实放行证据执行提示词

**生效日期**: 2026-05-01
**状态**: 待执行
**关联计划**:
- [PLAN-47: P0 专用环境 Nacos 解阻与真实 AI 证据闭环提示词](PLAN-47-P0-专用环境Nacos解阻与真实AI证据闭环提示词.md)
- [PLAN-46: P0 依赖高危处置与真实环境证据闭环提示词](PLAN-46-P0-依赖高危处置与真实环境证据闭环提示词.md)
- [PLAN-24: 稳定上线差距收口优先级](PLAN-24-稳定上线差距收口优先级.md)

**当前事实基线**:
- PLAN-47 可执行部分已完成并推送：pgjdbc 升级到 `42.7.11`，Nacos cgroup v2 启动问题已修复，service jar 预构建门禁脚本 `scripts/build-service-images.sh` 已新增。
- Compose 在临时 secret 下已可收敛到 `nacos/core/ai/im/auth/gateway-service` healthy，`gateway/frontend` running，`ocr-service` running 且 `/health` 返回 200。
- OCR 镜像已改为运行时延迟初始化，不再构建期强制拉模型。
- 本轮文档尚未同步 README、Runbook、TEST-MATRIX、CURRENT STATE、PLAN-00、PLAN-24、REPORT-07。
- 真实 AI/OCR sample gate、真实 `RUN_AI_VISION_E2E=1 npm run e2e:ai`、真实旧库 adoption / restore drill 仍未完成。
- `REPORT-07` 当前仍应保持 **NO_GO**。

---

## 1. 任务背景

前几轮已经把默认 CI、CodeQL、dependency-check artifact、主要 Maven 高危依赖、service 镜像构建、Nacos 启动、OCR 镜像启动和 Compose 健康检查逐步收口。当前阻塞已经从“基础设施无法启动”转向“文档明显慢于代码，以及真实 AI/旧库证据尚未执行”。

下一轮必须先把文档和代码事实拉平，再执行真实证据。不能在 README/Runbook 仍描述旧 Maven-in-Docker、旧 Nacos 阻塞或旧依赖风险口径的情况下继续推进发版复判。

---

## 2. 核心 Prompt 文本（请审查后执行）

```text
### [任务: P0 文档拉平、依赖复扫归档与真实放行证据执行]

任务背景：
BOBBuy 当前 main 已完成 pgjdbc 42.7.11 升级、Nacos cgroup v2 启动修复、service jar 预构建门禁脚本、OCR 运行时延迟初始化、Compose 健康检查修复和 gateway health 入口补齐。Compose 在临时 secret 下已可收敛到核心服务 healthy / running。但 README、Runbook、TEST-MATRIX、CURRENT STATE、PLAN-00、PLAN-24、REPORT-07 尚未完全同步，真实 AI/OCR sample、真实 e2e:ai 与真实旧库 adoption 仍无可信证据。当前 REPORT-07 必须保持 NO_GO，直到真实证据闭环。

目标：
1. 将所有文档口径拉平到当前代码事实。
2. 归档 pgjdbc 升级后的 dependency-check 新复扫结果。
3. 用 `scripts/build-service-images.sh` 固定 service jar 预构建和 Compose service build 门禁。
4. 在真实 Compose 栈上执行 AI/OCR sample 字段级 gate。
5. 执行真实 `RUN_AI_VISION_E2E=1 npm run e2e:ai` 或 AI release evidence workflow。
6. 若可取得真实旧库副本或历史 schema dump，执行 Flyway adoption / restore drill；否则明确责任人与输入缺口。
7. 更新 REPORT-07 并给出 GO / CONDITIONAL_GO / NO_GO 复判。

执行范围 A：文档拉平
1. 更新以下文件：
   - `README.md`
   - `docs/runbooks/RUNBOOK-试运行部署.md`
   - `docs/reports/TEST-MATRIX-本地与CI执行矩阵.md`
   - `docs/reports/CURRENT-STATE-2026-04-28.md`
   - `docs/reports/REPORT-07-NO-GO阻断项解阻与放行复判.md`
   - `docs/plans/PLAN-00-任务看板总览.md`
   - `docs/plans/PLAN-24-稳定上线差距收口优先级.md`
2. 文档必须明确：
   - service 镜像不再容器内 Maven 构建，而是复制宿主机预构建 jar。
   - Compose service build 推荐入口为 `scripts/build-service-images.sh`。
   - Nacos cgroup v2 / ProcessorMetrics 阻塞已修复，并说明 `-XX:-UseContainerSupport` 的目的、风险和适用范围。
   - OCR 镜像不再构建期拉模型，首次 OCR 请求可能触发模型初始化。
   - gateway health 新增 `/api/actuator/health` 与 `/api/actuator/health/readiness`。
   - 当前仍未完成真实 AI sample、真实 e2e:ai、真实旧库 adoption。
3. 删除或修正任何仍宣称 Maven PKIX、Nacos cgroup v2、CodeQL high、Tomcat/Netty/FileUpload high 是当前 blocker 的旧口径。

执行范围 B：dependency-check 复扫归档
1. 等待或触发 pgjdbc 42.7.11 后的 GitHub-hosted dependency-check run。
2. 下载 artifact，记录：
   - run URL
   - artifact id
   - critical/high/moderate 摘要
   - 是否仍存在 pgjdbc `CVE-2026-42198`
3. 若 critical/high 清零：
   - 在 REPORT-07 中标记 Maven dependency-check blocker 已解阻。
4. 若仍有 high：
   - 逐项形成修复或正式豁免，豁免需包含负责人、到期时间、不可利用理由和复测计划。

执行范围 C：Compose 服务构建和健康检查
1. 执行：
   ```bash
   ./scripts/build-service-images.sh
   docker compose config
   docker compose up -d postgres minio redis rabbitmq nacos nacos-init core-service ai-service im-service auth-service gateway-service frontend gateway ocr-service
   docker compose ps
   ```
2. 验证：
   - Nacos healthy
   - core/ai/im/auth/gateway-service healthy
   - frontend/gateway running
   - ocr-service `/health` 返回 200
   - gateway `/api/health`、`/api/actuator/health`、`/api/actuator/health/readiness` 可达
3. 保留失败日志；不得只写“已启动”。

执行范围 D：真实 AI/OCR sample 字段级 gate
1. 使用真实 Compose 后端、真实 OCR、真实 LLM/Codex Bridge、真实 seed。
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
4. 失败时保留 JSON/Markdown 报告并定位 OCR、LLM、字段归一化、seed、供应商规则、网络、权限或 golden 漂移根因。

执行范围 E：真实 AI E2E
1. 使用与 sample gate 相同的后端/OCR/LLM/seed。
2. 执行：
   ```bash
   cd frontend
   RUN_AI_VISION_E2E=1 npm run e2e:ai
   ```
3. 或手动触发 `.github/workflows/ai-release-evidence.yml`。
4. 归档 Playwright HTML report、trace、screenshot、video 和 test-results。

执行范围 F：真实旧库 Flyway adoption / restore drill
1. 若已有脱敏真实旧库副本或历史 schema dump：
   - restore 到隔离 PostgreSQL
   - baseline / baseline-on-migrate 评估
   - migrate
   - validate
   - restore drill
2. 若没有真实旧库输入：
   - REPORT-07 必须继续 NO_GO。
   - 记录责任人、数据来源、预计提供日期。

执行范围 G：最终复判
1. 更新 REPORT-07，必须包含：
   - 默认 CI run
   - CodeQL fixed 证据
   - dependency-check 新 artifact 与摘要
   - Compose 全栈健康检查证据
   - AI sample gate 报告
   - AI E2E artifact
   - 旧库 adoption / restore drill 记录或输入缺口
2. 判定规则：
   - GO：默认 CI、CodeQL、dependency-check、Compose、真实 AI sample、真实 e2e:ai、真实旧库 adoption 全部通过。
   - CONDITIONAL_GO：仅剩非 runtime、非数据安全风险，且有负责人、期限、批准记录。
   - NO_GO：真实 AI sample、真实 e2e:ai、真实旧库 adoption 任一缺失或失败，仍保持 NO_GO。

验收命令：
1. `docker compose config`
2. `./scripts/build-service-images.sh`
3. `docker compose up -d ...`
4. `cd backend && mvn test`
5. `cd frontend && npm ci && npm test && npm run build`
6. dependency-check 新 artifact 可下载且 critical/high 清零或正式豁免
7. 真实 sample gate
8. `RUN_AI_VISION_E2E=1 npm run e2e:ai`
9. 真实旧库 Flyway adoption / restore drill

禁止事项：
1. 不得用 mock sample dry-run 替代真实 sample gate。
2. 不得用默认 `npm run e2e` 替代 `RUN_AI_VISION_E2E=1 npm run e2e:ai`。
3. 不得用空库 migration 替代真实旧库 adoption。
4. 不得提交明文 API key、数据库 dump、旧库原始数据或个人数据。
5. 不得把“服务 running”写成“业务证据通过”。
6. 不得回滚非本任务相关的用户改动。
```

---

## 3. 审核重点

1. 是否先把文档口径拉平，避免文档慢于代码。
2. 是否把 service jar 预构建门禁写清楚。
3. 是否要求真实 AI sample 和真实 e2e:ai，而不是继续停留在健康检查。
4. 是否继续把真实旧库 adoption 作为 GO 前置条件。
5. 是否保持 `REPORT-07` 在真实证据缺失时为 `NO_GO`。

---

## 4. 预期交付物

1. README / Runbook / TEST-MATRIX / CURRENT STATE / PLAN-00 / PLAN-24 / REPORT-07 同步更新。
2. pgjdbc 升级后的 dependency-check artifact 与摘要。
3. Compose 全栈健康检查记录。
4. 真实 AI sample JSON/Markdown 报告。
5. 真实 AI E2E Playwright artifact。
6. 真实旧库 adoption / restore drill 记录，或明确输入缺口登记。

---

## 5. 当前核准状态

**当前状态**: 待用户审查批准后执行。
