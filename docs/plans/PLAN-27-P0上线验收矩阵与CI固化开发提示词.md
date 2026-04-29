# PLAN-27: P0 上线验收矩阵与 CI 固化开发提示词

**生效日期**: 2026-04-29
**状态**: 待评审
**关联计划**: [PLAN-24: 稳定上线差距收口优先级](PLAN-24-稳定上线差距收口优先级.md)
**前置任务**:
- [PLAN-25: P0 后端测试基线恢复开发提示词](PLAN-25-P0后端测试基线恢复开发提示词.md)
- [PLAN-26: P0 前端测试基线恢复开发提示词](PLAN-26-P0前端测试基线恢复开发提示词.md)
**事实基线**: [CURRENT-STATE-2026-04-28](../reports/CURRENT-STATE-2026-04-28.md)

---

## 1. 开发目标

把已经恢复的后端与前端测试基线固化为稳定上线门禁，让本地验收、GitHub Actions、测试矩阵文档、Release 记录口径一致。

本任务只处理 P0 阶段第三个任务：**固化上线验收矩阵**。目标不是引入新的业务功能，而是把“什么必须通过、什么专用环境执行、什么只做风险登记”明确写进 CI 与文档。

---

## 2. 核心 Prompt 文本（请审查）

您可以直接将以下内容作为下一次开发任务的输入：

```text
### [任务: P0 上线验收矩阵与 CI 固化]

任务背景：
BOBBuy 当前 P0 前两个任务已经恢复：
1. 后端默认测试：`cd backend && mvn test` 已恢复稳定通过。
2. 前端默认测试：`cd frontend && npm test` 已恢复稳定通过，`npm run build` 继续通过。

但当前 `.github/workflows/ci.yml` 与 `docs/reports/TEST-MATRIX-本地与CI执行矩阵.md` 仍没有完全形成统一上线门禁。请完成 CI 与文档收口，让试运行上线前的验证路径明确、可执行、可追踪。

目标：
1. 更新 GitHub Actions，使默认 CI 明确执行后端测试、前端测试、前端构建、Docker 构建等基础门禁。
2. 更新 `TEST-MATRIX`，让文档与 CI job、命令、门控条件完全一致。
3. 明确 Playwright E2E、AI 真实视觉链路、CodeQL/安全扫描的分层策略。
4. 更新 README 或 Release/当前状态文档中已经过期的测试口径，避免继续宣称错误状态。
5. 保留默认 CI 不依赖真实 Ollama、Codex CLI、OpenAI、MinIO、外网 AI 服务或 Windows 桌面登录态的原则。

必须先做的排查：
1. 阅读当前 CI：
   - `.github/workflows/ci.yml`
2. 阅读当前测试矩阵与状态文档：
   - `docs/reports/TEST-MATRIX-本地与CI执行矩阵.md`
   - `docs/reports/CURRENT-STATE-2026-04-28.md`
   - `docs/plans/PLAN-24-稳定上线差距收口优先级.md`
   - `README.md`
3. 本地执行或确认以下命令当前可用：
   - `cd backend && mvn test`
   - `cd frontend && npm test`
   - `cd frontend && npm run build`
   - 可选：`cd backend && mvn -DskipTests compile`

修复范围 A：CI 默认门禁
1. 调整 `.github/workflows/ci.yml`，默认 push / pull_request 至 `main` 时至少包含：
   - backend test：`mvn -B test --file backend/pom.xml` 或等价命令。
   - frontend test：在 `frontend` 安装依赖后执行 `npm test`。
   - frontend build：`npm run build`。
   - Docker build backend/frontend：保持现有镜像构建验证。
2. 优先使用缓存：
   - Maven cache 使用 `actions/setup-java` 的 `cache: maven`。
   - Node cache 使用 `actions/setup-node` 的 npm cache，并指向 `frontend/package-lock.json`。
3. 避免重复安装依赖导致 CI 变慢；前端 job 内测试和构建应共用一次 install。
4. 后端测试默认使用 test resources，不应读取真实外部 AI/OCR/Codex/MinIO 配置。

修复范围 B：分层门禁定义
请在 CI 和文档中把验证分成至少三层：

1. 默认门禁（每个 PR / main push 必跑）：
   - backend `mvn test`
   - frontend `npm test`
   - frontend `npm run build`
   - backend/frontend Docker build
2. 专用环境门禁（可手动触发或有条件执行）：
   - Playwright `npm run e2e`
   - AI 真实视觉链路 `RUN_AI_VISION_E2E=1 npm run e2e:ai`
3. 风险登记或独立安全门禁：
   - CodeQL / security scan
   - 依赖审计
   - 如果超时或平台限制，必须在 PR/Release 说明中登记，不得默认为已通过。

修复范围 C：文档同步
1. 更新 `docs/reports/TEST-MATRIX-本地与CI执行矩阵.md`：
   - 写清每个命令的本地路径、CI job 名称、是否默认门禁、是否需要外部服务。
   - 将后端测试、前端测试状态从历史问题改为当前门禁。
   - 标注 E2E / AI E2E 的执行条件。
2. 更新 `docs/reports/CURRENT-STATE-2026-04-28.md` 或新增日期化状态文件：
   - 反映后端和前端测试基线已恢复。
   - 如果不更新原文件日期，必须明确这是 2026-04-29 的增量状态。
3. 检查 README 是否仍有过期测试状态或错误命令；如有，更新。
4. 如 Release 文档中有明显冲突口径，可以增加引用当前状态/测试矩阵的说明，不需要重写历史 Release。

修复范围 D：测试输出与可维护性
1. 不要求本任务修复所有非阻塞 warning，但需要登记明显噪声：
   - AntD `useForm` 未连接 warning。
   - 预期失败测试中的 console error。
2. 如果能低风险消除噪声，可以一并处理；否则写入后续清理项。
3. 不要因为 CI 需要而弱化已有测试断言。

验收命令：
1. 本地：
   - `cd backend && mvn test`
   - `cd frontend && npm test`
   - `cd frontend && npm run build`
2. CI 文件静态检查：
   - 确认 `.github/workflows/ci.yml` 语法有效。
   - 确认 job 名称与 `TEST-MATRIX` 一致。
3. 如环境允许：
   - `cd frontend && npm run e2e`
4. 如环境不允许执行 E2E：
   - 文档中明确 E2E 未作为默认门禁的原因、触发方式和责任边界。

交付要求：
1. 提供修改清单：
   - CI 修改点。
   - 测试矩阵/README/状态文档修改点。
   - 是否修改了代码或测试。
2. 提供验证结果：
   - 后端测试结果。
   - 前端测试结果。
   - 前端构建结果。
   - E2E 是否执行，以及未执行原因。
3. 提供上线门禁摘要：
   - 默认必过项。
   - 手动/专用环境项。
   - 风险登记项。

禁止事项：
1. 不得用 `-DskipTests` 或 `--passWithNoTests` 作为默认门禁。
2. 不得把真实 AI/OCR/Codex/MinIO 依赖放进默认 CI。
3. 不得让 CI 需要 Windows 桌面 Codex 登录态。
4. 不得把 E2E、AI E2E、CodeQL 的未执行状态写成已通过。
5. 不得回滚非本任务相关的用户改动。

建议实现顺序：
1. 读取 CI 与 TEST-MATRIX，列出当前不一致点。
2. 修改 CI：后端测试、前端测试、前端构建、Docker build 分层清晰。
3. 更新 TEST-MATRIX，使 job 名称、命令、门控条件一致。
4. 更新 CURRENT STATE / README 中过期测试状态。
5. 本地跑 backend test、frontend test、frontend build。
6. 检查 git diff，确保没有混入临时文件。
7. 提交并在 PR/提交说明中写清默认门禁与专用门禁。
```

---

## 3. 审核重点

请重点审核以下问题：

1. 是否要求 Playwright `npm run e2e` 进入默认 CI，还是保留为专用/手动门禁。
2. 是否要求 CodeQL 在本任务中接入 GitHub Actions，还是先作为风险登记项。
3. 是否需要新增 2026-04-29 状态报告，还是直接更新 2026-04-28 当前状态文件。
4. 是否把前端测试输出 warning 治理纳入本任务硬性范围。

---

## 4. 预期交付物

1. 更新后的 `.github/workflows/ci.yml`。
2. 更新后的 `docs/reports/TEST-MATRIX-本地与CI执行矩阵.md`。
3. 必要的 README / 当前状态文档同步。
4. 一份简短执行报告，包含默认门禁、专用门禁、风险登记项和本地验证结果。

---

## 5. 核准状态

**当前状态**: 待用户审查批准后执行。
