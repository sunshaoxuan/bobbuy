# PLAN-47: P0 专用环境 Nacos 解阻与真实 AI 证据闭环提示词

**生效日期**: 2026-05-01
**状态**: 待执行
**关联计划**:
- [PLAN-46: P0 依赖高危处置与真实环境证据闭环提示词](PLAN-46-P0-依赖高危处置与真实环境证据闭环提示词.md)
- [PLAN-44: P0 默认 CI 恢复、真实环境放行证据与 REPORT-07 复判提示词](PLAN-44-P0-真实环境放行证据与REPORT07复判提示词.md)
- [PLAN-24: 稳定上线差距收口优先级](PLAN-24-稳定上线差距收口优先级.md)

**当前事实基线**:
- PLAN-46 已覆盖 `tomcat-embed-core 10.1.54`、`netty-transport 4.1.132.Final`、`commons-fileupload 1.6.0`。
- `Dockerfile.service` 已改为复制宿主机构建好的 `${MODULE}/target/${MODULE}-*.jar`，不再在 service 镜像内访问 Maven Central。
- `docker compose build core-service ai-service im-service auth-service gateway-service` 已通过，Maven-in-Docker `PKIX path building failed` 已解除。
- 最新 GitHub-hosted Maven dependency-check 复扫 run `25215061203` 已完成，artifact `6749713633` 可下载，摘要降至 `0 critical / 1 high / 10 medium`；唯一 high 为 `postgresql-42.6.2.jar` / `CVE-2026-42198`。
- 真实 compose 栈仍未进入可执行 AI sample / `e2e:ai` 状态，当前新阻塞为 Nacos 在 cgroup v2 / `ProcessorMetrics` 场景启动异常。
- 前端 `npm test` 在本地多次复跑有 ChatWidget / StockMaster 超时波动，但 GitHub `frontend-quality` 在 run `25215061192` 中已通过；仍需登记并稳定化。
- 真实旧库 dump / 历史 schema dump 仍未提供。
- `REPORT-07` 当前仍为 **NO_GO**。

---

## 1. 任务背景

上一轮已经把 service 镜像构建从容器内 Maven 改为复制宿主机 jar，绕开了 Maven Central 证书链问题。但真实环境证据仍无法执行，因为 Compose 全栈在 Nacos 启动阶段卡住；同时 dependency-check 新复扫只剩 pgjdbc 一个 high，真实 AI/OCR 和旧库 adoption 仍没有可信证据。

下一轮目标不是继续补 mock 或文档口径，而是让专用环境真正跑起来，并把 AI sample、AI E2E、旧库 adoption 证据接到 `REPORT-07`。

---

## 2. 核心 Prompt 文本（请审查后执行）

```text
### [任务: P0 专用环境 Nacos 解阻、依赖复扫余项与真实 AI 证据闭环]

任务背景：
BOBBuy 最新 main 已完成 CodeQL high 清零、dependency-check artifact 归档、Tomcat/Netty/FileUpload 高危解析版本覆盖，并把 Dockerfile.service 改为复制宿主机构建 jar，解除 Maven-in-Docker PKIX 阻塞。但真实 compose 栈仍因 Nacos cgroup v2 / ProcessorMetrics 启动异常无法进入 AI sample 和 e2e:ai；dependency-check 新复扫只剩 pgjdbc CVE-2026-42198 high；真实旧库 dump 仍缺失。当前 REPORT-07 仍为 NO_GO。

目标：
1. 固定 service jar 预构建门禁，避免干净工作区 `docker compose build` 因缺 jar 失败。
2. 解决或绕过 Nacos cgroup v2 / ProcessorMetrics 启动异常，但不得破坏 Nacos 配置事实源边界。
3. 处理 GitHub-hosted dependency-check 复扫余项：pgjdbc `CVE-2026-42198` high。
4. 拉起真实 compose 栈，完成健康检查。
5. 执行真实 AI/OCR sample 字段级 gate。
6. 执行真实 `RUN_AI_VISION_E2E=1 npm run e2e:ai` 或 AI release evidence workflow。
7. 处理前端 ChatWidget / StockMaster 本地测试超时波动，至少形成可复现诊断和稳定化方案。
8. 若能取得真实旧库副本或历史 schema dump，执行 Flyway adoption / restore drill；否则在 REPORT-07 明确责任人与输入缺口。
9. 更新 REPORT-07、PLAN-00、PLAN-24、CURRENT STATE、TEST MATRIX 与 README，给出 GO / CONDITIONAL_GO / NO_GO 复判。

执行范围 A：service jar 预构建门禁
1. 明确 service 镜像构建前置命令：
   ```bash
   mvn -f pom.xml -DskipTests package -pl bobbuy-core,bobbuy-ai,bobbuy-im,bobbuy-auth,bobbuy-gateway -am
   docker compose build core-service ai-service im-service auth-service gateway-service
   ```
2. 在 README / TEST-MATRIX / Runbook 中说明：`Dockerfile.service` 不负责 Maven 构建，只复制已构建 jar。
3. 如有必要，增加脚本或 CI job 固定该顺序，避免使用者直接 `docker compose build` 时得到难以理解的 jar missing。
4. 验证干净工作区行为：缺 jar 时失败信息应明确；有 jar 时服务镜像可构建。

执行范围 B：Nacos cgroup v2 / ProcessorMetrics 解阻
1. 复现并保留日志：
   ```bash
   docker compose up -d nacos
   docker logs bobbuy-nacos --tail=300
   ```
2. 定位 `ProcessorMetrics` 空指针来源，优先尝试：
   - 调整 Nacos 镜像版本到 cgroup v2 兼容版本；
   - 或通过 JVM / Spring Boot actuator 配置禁用 processor metrics；
   - 或为试运行 Compose 提供 `nacos-lite` / external Nacos 配置路径。
3. 禁止事项：
   - 不得删除 Nacos 配置事实源而不更新 ADR / Runbook。
   - 不得让 core-service / ai-service / im-service / auth-service 的配置口径互相漂移。
4. 验收：
   - `docker compose up -d postgres minio redis rabbitmq nacos nacos-init core-service ai-service im-service auth-service gateway-service frontend gateway ocr-service`
   - `docker compose ps` 服务健康
   - gateway `/api/health` 可达
   - Nacos init 配置成功

执行范围 C：dependency-check 新复扫余项
1. 下载并归档 run `25215061203` artifact：
   - artifact: `dependency-check-report` / `6749713633`
   - 当前摘要：`0 critical / 1 high / 10 medium`
2. 处理唯一 high：
   - dependency: `org.postgresql:postgresql:42.6.2`
   - CVE: `CVE-2026-42198`
   - 建议动作：升级 pgjdbc 到 `42.7.11+`，或形成正式豁免。
3. 若升级：
   - 在 `backend/pom.xml` 或统一 dependencyManagement 中固定 `postgresql.version`。
   - 验证 backend 测试、服务模块构建、Docker/Compose 构建。
   - 重跑 dependency-check，确认 critical/high 清零或仅剩正式豁免项。
4. 若豁免：
   - 必须证明生产数据库连接目标可信，不存在恶意 PostgreSQL server 注入面。
   - 必须记录负责人、到期时间、不可利用理由、复测计划。
5. 更新 REPORT-07 的依赖处置表。
6. 未完成前不得写 GO。

执行范围 D：真实 AI/OCR sample 字段级 gate
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
4. 失败时保留报告并定位 OCR、LLM、字段归一化、seed、供应商规则、网络、权限或 golden 漂移根因。

执行范围 E：真实 AI E2E
1. 使用与 sample gate 相同的后端/OCR/LLM/seed。
2. 执行：
   ```bash
   cd frontend
   RUN_AI_VISION_E2E=1 npm run e2e:ai
   ```
3. 或手动触发 `.github/workflows/ai-release-evidence.yml`。
4. 归档 Playwright HTML report、trace、screenshot、video 和 test-results。

执行范围 F：前端测试波动收口
1. 定位 ChatWidget / StockMaster 超时波动：
   ```bash
   cd frontend
   npm test -- --runInBand
   npm test -- ChatWidget StockMaster
   ```
   如 Vitest 不支持该参数，使用项目现有等价定向命令。
2. 检查 fake timers、debounce、WebSocket mock、localStorage、async waitFor 是否存在泄漏。
3. 若 CI 已稳定但本地波动仍存在，记录环境差异和复现条件；若可修复，补测试稳定化改动。

执行范围 G：真实旧库 Flyway adoption / restore drill
1. 获取脱敏真实旧库副本或历史 schema dump。
2. 在隔离 PostgreSQL 环境执行 pre-backup、restore、baseline/migrate/validate、restore drill。
3. 没有真实旧库输入时，REPORT-07 必须保持 NO_GO，并记录责任人与预计提供时间。

执行范围 H：REPORT-07 复判与文档同步
1. 更新：
   - `docs/reports/REPORT-07-NO-GO阻断项解阻与放行复判.md`
   - `docs/plans/PLAN-00-任务看板总览.md`
   - `docs/plans/PLAN-24-稳定上线差距收口优先级.md`
   - `docs/reports/CURRENT-STATE-2026-04-28.md`
   - `docs/reports/TEST-MATRIX-本地与CI执行矩阵.md`
   - `README.md`
2. REPORT-07 必须包含：
   - 默认 CI run
   - CodeQL fixed 证据
   - dependency-check 新 artifact 与余项处置表
   - service jar 预构建与 Compose build 证据
   - Nacos 解阻证据
   - 真实 AI sample gate 报告
   - 真实 AI E2E artifact
   - 真实旧库 adoption / restore drill 记录
   - 最终 GO / CONDITIONAL_GO / NO_GO 判定

验收命令：
1. `docker compose config`
2. `mvn -f pom.xml -DskipTests package -pl bobbuy-core,bobbuy-ai,bobbuy-im,bobbuy-auth,bobbuy-gateway -am`
3. `docker compose build core-service ai-service im-service auth-service gateway-service`
4. `docker compose up -d ...` 并健康检查通过
5. `cd backend && mvn test`
6. `cd frontend && npm ci && npm test && npm run build`
7. dependency-check 新 artifact 可下载，pgjdbc high 清零或正式豁免
8. 真实 sample gate 命令
9. `RUN_AI_VISION_E2E=1 npm run e2e:ai`
10. 真实旧库 Flyway adoption / restore drill

禁止事项：
1. 不得用 mock sample dry-run 替代真实 sample gate。
2. 不得用默认 `npm run e2e` 替代 `RUN_AI_VISION_E2E=1 npm run e2e:ai`。
3. 不得用空库 migration 替代真实旧库 adoption。
4. 不得关闭 TLS 校验或改 HTTP Maven 源。
5. 不得把 dependency-check run success 当成 critical/high 已处置。
6. 不得提交明文 API key、数据库 dump、旧库原始数据或个人数据。
7. 不得回滚非本任务相关的用户改动。
```

---

## 3. 审核重点

1. 是否把 Nacos 启动异常作为真实环境证据的当前第一阻塞。
2. 是否明确 service 镜像依赖宿主机 jar 预构建，并要求固定门禁。
3. 是否要求 dependency-check 新复扫余项处置，而不是只记录旧 artifact。
4. 是否继续禁止 mock/dry-run 替代真实 AI 和旧库证据。
5. 是否保持 `REPORT-07` 在真实证据补齐前为 `NO_GO`。

---

## 4. 预期交付物

1. Nacos cgroup v2 / ProcessorMetrics 解阻记录。
2. service jar 预构建门禁或脚本/CI 固化。
3. dependency-check 新 artifact 与余项处置表。
4. 真实 AI sample JSON/Markdown 报告。
5. 真实 AI E2E Playwright artifact。
6. 真实旧库 adoption / restore drill 记录，或明确输入缺口登记。
7. 更新后的 `REPORT-07` 与相关文档。

---

## 5. 当前核准状态

**当前状态**: 待用户审查批准后执行。
