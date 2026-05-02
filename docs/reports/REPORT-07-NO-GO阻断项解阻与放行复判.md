# REPORT-07: NO-GO 阻断项解阻与放行复判

**日期**: 2026-05-02
**分支**: `main`

---

## 1. 最终结论

- **放行判定**: **GO_INTERNAL_TRIAL_PENDING_SERVER_WINDOW**
- **结论原因**: 默认 CI、CodeQL high、服务镜像 Maven-in-Docker PKIX、Nacos cgroup v2、pgjdbc high、Tomcat/Netty/FileUpload high、真实 AI/OCR sample gate、真实 `RUN_AI_VISION_E2E=1 npm run e2e:ai`、mock 与本地 Compose 真实栈双角色移动端黑盒均已通过。用户已确认当前无历史业务数据，真实旧库 adoption / restore drill 从 blocker 调整为不适用，替代为空库上线与备份恢复验收。PLAN-57 已建立关闭剩余执行中任务的服务器放行复判入口，但当前执行环境仍缺少 `SSH_TARGET` 与 `APP_DIR`，服务器部署窗口尚未执行，因此正式内部试运行仍等待服务器复跑、备份恢复演练与 artifact 归档。

---

## 2. 已确认的放行证据

### 2.1 默认 CI 已恢复

- workflow: `.github/workflows/ci.yml`
- 最新 main success run: <https://github.com/sunshaoxuan/bobbuy/actions/runs/25217655067>
- 结果: `success`
- jobs:
  - `backend-test`: success
  - `frontend-quality`: success
  - `docker-build`: success

### 2.2 CodeQL high alert 已清零

- workflow: `.github/workflows/codeql.yml`
- 最新 main success run: <https://github.com/sunshaoxuan/bobbuy/actions/runs/25217655038>
- 覆盖语言: Java/Kotlin、JavaScript/TypeScript、Actions
- 结果: success
- 已修复并复扫通过的 high:
  - `java/spring-disabled-csrf-protection`
  - `js/xss-through-dom` x2

### 2.3 Maven dependency-check critical/high 已清零

- workflow: `.github/workflows/dependency-check.yml`
- 最新 main run: <https://github.com/sunshaoxuan/bobbuy/actions/runs/25217516557>
- artifact:
  - name: `dependency-check-report`
  - id: `6750657743`
  - 已验证 ZIP 内含 `dependency-check-report.html` 与 `dependency-check-report.json`
- JSON 摘要:
  - `critical`: 0
  - `high`: 0
  - `medium`: 13
  - `low`: 2
- pgjdbc 已解析为 `42.7.11`，此前 `postgresql-42.6.2.jar` / `CVE-2026-42198` high 已清零。
- 剩余 medium/low 需继续登记和后续升级，但不再按 critical/high 阻断本轮试运行证据收集。

### 2.4 Compose 与服务镜像基础链路已闭环

- `Dockerfile.service` 当前只复制宿主机预构建 jar，不在镜像内执行 Maven。
- 推荐入口为 `scripts/build-service-images.sh`；本轮已修复该脚本在 Windows/Git Bash/WSL 下的 LF 与 Maven wrapper 调用问题。
- 本轮执行 `bash scripts/build-service-images.sh` 通过：
  - `bobbuy-core`
  - `bobbuy-ai`
  - `bobbuy-im`
  - `bobbuy-auth`
  - `bobbuy-gateway`
- `infra/nacos/init-config.sh` 已修复 CRLF，`nacos-init` 可正常发布配置并退出。
- 本轮以临时本地环境变量提供 `BOBBUY_SECURITY_JWT_SECRET` / `BOBBUY_SECURITY_SERVICE_TOKEN` 后，完整 Compose 栈可收敛到：
  - `postgres` / `minio` / `redis` / `rabbitmq` / `nacos`: healthy
  - `core-service` / `ai-service` / `im-service` / `auth-service` / `gateway-service`: healthy
  - `gateway`: running
  - `frontend`: running
  - `ocr-service`: running
- 健康检查结果:
  - `GET http://127.0.0.1/api/health` -> `{"status":"ok","service":"gateway-service"}`
  - `GET http://127.0.0.1/api/actuator/health` -> `{"status":"UP","service":"gateway-service"}`
  - `GET http://127.0.0.1/api/actuator/health/readiness` -> `{"status":"UP","service":"gateway-service"}`
  - `GET http://127.0.0.1:8000/health` -> `{"status":"ok"}`

---

## 3. 仍阻断发版的事项

| blocker | 当前状态 | 证据 / 阻塞 | 责任边界 |
| :-- | :-- | :-- | :-- |
| 默认 CI | RESOLVED | main run `25217655067` 已 success | 已解阻 |
| CodeQL 默认分支证据 | RESOLVED | main run `25217655038` 已 success | 已解阻 |
| Maven dependency-check critical/high | RESOLVED | main run `25217516557` artifact `6750657743` 为 `0 critical / 0 high / 13 medium / 2 low` | 剩余 medium/low 进入风险登记 |
| Compose 基础健康 | RESOLVED | 本地临时 secret 下完整栈启动，gateway 与 OCR health 均通过 | 生产仍必须由 secret manager 注入真实 secret |
| 双角色移动端黑盒走查 | RESOLVED_LOCAL | mock 数据与本地 Compose 真实栈均已通过，见 `REPORT-08-双角色移动端黑盒走查报告.md` 与 `REPORT-10-真实栈双角色黑盒验收报告.md` | 服务器部署窗口仍需复跑并上传 artifact |
| 真实 AI sample 实扫 | RESOLVED | PLAN-50 使用可用 Codex Bridge 注入后，`scripts/verify-ai-onboarding-samples.ps1 -IncludeNeedsHumanGolden -AuthToken <agent-token>` 返回 `3 PASS / 0 FAIL / 0 SCAN_FAIL`，`gatePassed=true`；报告归档在 `docs/reports/evidence/ai-onboarding-real-sample-plan50-2026-05-02.*` | 生产仍必须外部注入 bridge secret，不得提交明文 key |
| 真实 `RUN_AI_VISION_E2E=1 npm run e2e:ai` | RESOLVED | sample gate PASS 后复跑通过，`2 passed`；覆盖 `IMG_1484.jpg` 新商品/可确认路径与 `IMG_1638.jpg` existing product 路径 | 后续继续保留 artifact 归档 |
| 真实旧库 adoption / restore drill | N/A | 用户确认当前无历史数据，改为空库上线与备份恢复验收；见 `REPORT-12-空库上线与备份恢复演练报告.md` | 若未来导入历史数据，需重新启用 adoption 门禁 |
| 服务器部署窗口 | BLOCKED_INPUT | PLAN-57 已建立执行提示词，REPORT-13 已登记；当前未提供 `SSH_TARGET` / `APP_DIR`，无法 SSH 执行 | 发布负责人提供服务器连接信息 |

---

## 4. 本轮本地验证

- `docker compose config`: 通过
- `.\mvnw.cmd -f backend\pom.xml test`: 通过，`175 tests, 0 failures, 0 errors, 2 skipped`
- `cd frontend && npm test`: 通过，`22 files / 74 tests`
- `cd frontend && npm run build`: 通过
- `bash scripts/build-service-images.sh`: 通过
- `docker compose up -d postgres minio redis rabbitmq nacos nacos-init core-service ai-service im-service auth-service gateway-service frontend gateway ocr-service`: 首次因本机 `.env` 缺少 JWT secret 失败；临时注入本地测试 secret 后通过
- `curl http://127.0.0.1/api/health`: 通过
- `curl http://127.0.0.1/api/actuator/health`: 通过
- `curl http://127.0.0.1/api/actuator/health/readiness`: 通过
- `curl http://127.0.0.1:8000/health`: 通过
- `scripts/verify-ai-onboarding-samples.ps1 ... -AuthToken <agent token>`: 通过，`3 PASS / 0 FAIL / 0 SCAN_FAIL`，`gatePassed=true`；JSON/Markdown 报告已归档到 `docs/reports/evidence/ai-onboarding-real-sample-plan50-2026-05-02.*`
- `RUN_REAL_MOBILE_BLACKBOX=1 npm run e2e --prefix frontend -- e2e/mobile_agent_blackbox.spec.ts e2e/mobile_customer_blackbox.spec.ts`: 通过，`4 passed`，覆盖 `390x844` 与 `360x800`，使用真实 Compose 后端 API，未注册 mock API route
- `RUN_AI_VISION_E2E=1 npm run e2e:ai`: 通过，`2 passed`
- 旧库 adoption 复判：用户确认当前无历史数据，本轮改为空库上线与备份恢复验收口径，详见 `REPORT-12`

---

## 5. 本轮发现并已修复的问题

- `scripts/build-service-images.sh` 使用 CRLF 时无法在 bash 中解析 `set -euo pipefail`。
- `scripts/build-service-images.sh` 在 Windows/Git Bash/WSL 下找不到 `mvn` 或误执行 `mvnw.cmd`，已改为自动回退 Maven wrapper 并转换 WSL 路径。
- `infra/nacos/init-config.sh` 使用 CRLF 时容器内 `/bin/sh` 解析失败，导致 `nacos-init` exit 2。
- `scripts/verify-ai-onboarding-samples.ps1` 的 Markdown 报告文案在 Windows PowerShell 5 下因 UTF-8 无 BOM 解析/输出不稳定，已将报告固定文案改为 ASCII，并支持 `-AuthToken`。
- `frontend` 的 `e2e:ai` npm script 在 Windows 下直接 `spawnSync('npx.cmd')` 返回 `EINVAL`，已改为 `shell: true`。
- AI E2E 仍使用旧按钮文案 `AI Quick Snap`，已为入口按钮增加 `data-testid="ai-quick-snap-button"` 并更新测试使用稳定选择器。
- `LlmGateway` 现在会在主 Ollama 返回空 `response` 时尝试切换到 Codex Bridge / Codex CLI，避免把空响应直接传导成 `AI_RECOGNITION`。
- `LlmGateway` 对 Codex Bridge `/chat/completions` 请求显式序列化 JSON，修复 bridge 拒绝非标准 JSON body 导致的 sample `SCAN_FAIL`。
- OpenAI-compatible 响应解析已支持 `message.content` 为字符串或 `{type:"text", text:"..."}` 数组两种形态。
- `AiProductOnboardingService` 已补食品类目推断、OCR 原文结构化属性恢复、Costco Mixed Seafood 单位价格归一化、抹茶样例分散品番恢复，并调整相似商品匹配，避免通用类目 token 造成误匹配。
- Compose 与 Nacos `ai-service` 配置已补齐 `BOBBUY_AI_LLM_CODEX_BRIDGE_*` 与 `BOBBUY_AI_SECRET_MASTER_PASSWORD` 传递。
- Codex CLI 只有在容器/主机内真实可执行时才会被选为可用 provider；仓库 `.env` 默认不再设置 `BOBBUY_AI_LLM_CODEX_COMMAND=codex`，避免 Linux 服务容器误依赖 Windows 桌面 CLI。
- 双角色移动端黑盒走查发现并修复客户首页 sticky 导航遮挡手机 header、手机 header 身份文字拥挤、库存手机端新增商品不进入编辑表单等可用性问题；mock 数据下客户/采购者核心路径已通过，详见 `REPORT-08`。
- 真实栈移动端黑盒发现并修复客户首页越权请求采购者钱包导致 401 清空登录态的问题；客户视图不再加载采购者钱包，复测客户/采购者 `390x844` 与 `360x800` 全部通过，详见 `REPORT-10`。

---

## 6. 最终复判

**GO_INTERNAL_TRIAL_PENDING_SERVER_WINDOW**

当前可以确认：基础 CI、安全 high、Compose/Nacos/OCR/gateway health、真实 AI/OCR sample gate、真实 `e2e:ai`、mock 移动端黑盒、本地 Compose 真实栈双角色黑盒均已通过；旧库 adoption 因无历史数据调整为不适用。内部/小范围试运行可以进入服务器部署窗口，但最终执行前必须完成：

1. 在服务器试运行环境复跑完整门禁与真实栈双角色黑盒，并归档 Playwright artifact。
2. 执行 PostgreSQL / MinIO / Nacos 备份恢复演练并记录恢复校验。
3. 通过外部 secret 注入 Codex Bridge key、JWT secret、service token 与基础设施密码；本轮证据使用本地临时环境变量，未提交 secret。
4. 当前缺少 `SSH_TARGET` 与 `APP_DIR`，服务器窗口执行记录见 `REPORT-13`；执行入口为 `PLAN-57`。

在这些项目完成前，发版候选继续维持 `NO_GO`。
