# REPORT-07: NO-GO 阻断项解阻与放行复判

**日期**: 2026-05-01  
**分支**: `main`

---

## 1. 最终结论

- **放行判定**: **NO_GO**
- **结论原因**: 默认 CI、CodeQL high、服务镜像 Maven-in-Docker PKIX、Nacos cgroup v2、pgjdbc high、Tomcat/Netty/FileUpload high 均已不再作为当前 blocker；但真实 AI/OCR sample gate 仍失败，真实 `RUN_AI_VISION_E2E=1 npm run e2e:ai` 仍失败，且真实旧库 adoption / restore drill 仍缺少旧库 dump，因此不能放行发版候选。

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
| 真实 AI sample 实扫 | BLOCKED | `scripts/verify-ai-onboarding-samples.ps1` 已修复并带 token 打到真实 `/api/ai/onboard/scan`；结果 `0 PASS / 3 SCAN_FAIL`，后端返回 `AI_RECOGNITION`，日志显示 LLM 返回空结果 | AI/OCR / provider 配置负责人 |
| 真实 `RUN_AI_VISION_E2E=1 npm run e2e:ai` | BLOCKED | Windows npm script 已修复并能进入 Playwright；2 个用例均失败，artifact 已生成；失败原因是页面未获得成功 AI 识别结果 | AI/OCR / 前端负责人 |
| 真实旧库 adoption / restore drill | BLOCKED | 仓库内仍未提供真实旧库副本或历史 schema dump，无法执行 adoption / restore | DBA / 发布负责人 |

---

## 4. 本轮本地验证

- `docker compose config`: 通过
- `.\mvnw.cmd -f backend\pom.xml test`: 通过，`173 tests, 0 failures, 0 errors, 2 skipped`
- `cd frontend && npm ci && npm test && npm run build`: 通过
- `bash scripts/build-service-images.sh`: 通过
- `docker compose up -d postgres minio redis rabbitmq nacos nacos-init core-service ai-service im-service auth-service gateway-service frontend gateway ocr-service`: 首次因本机 `.env` 缺少 JWT secret 失败；临时注入本地测试 secret 后通过
- `curl http://127.0.0.1/api/health`: 通过
- `curl http://127.0.0.1/api/actuator/health`: 通过
- `curl http://127.0.0.1/api/actuator/health/readiness`: 通过
- `curl http://127.0.0.1:8000/health`: 通过
- `scripts/verify-ai-onboarding-samples.ps1 ... -AuthToken <agent token>`: 执行到真实接口但 gate 失败，`0 PASS / 3 SCAN_FAIL`
- `RUN_AI_VISION_E2E=1 npm run e2e:ai`: 执行到 Playwright，2 failed，保留 screenshot/video/trace

---

## 5. 本轮发现并已修复的问题

- `scripts/build-service-images.sh` 使用 CRLF 时无法在 bash 中解析 `set -euo pipefail`。
- `scripts/build-service-images.sh` 在 Windows/Git Bash/WSL 下找不到 `mvn` 或误执行 `mvnw.cmd`，已改为自动回退 Maven wrapper 并转换 WSL 路径。
- `infra/nacos/init-config.sh` 使用 CRLF 时容器内 `/bin/sh` 解析失败，导致 `nacos-init` exit 2。
- `scripts/verify-ai-onboarding-samples.ps1` 的 Markdown 报告文案在 Windows PowerShell 5 下因 UTF-8 无 BOM 解析/输出不稳定，已将报告固定文案改为 ASCII，并支持 `-AuthToken`。
- `frontend` 的 `e2e:ai` npm script 在 Windows 下直接 `spawnSync('npx.cmd')` 返回 `EINVAL`，已改为 `shell: true`。
- AI E2E 仍使用旧按钮文案 `AI Quick Snap`，已为入口按钮增加 `data-testid="ai-quick-snap-button"` 并更新测试使用稳定选择器。

---

## 6. 最终复判

**NO_GO**

当前可以确认：基础 CI、安全 high、Compose/Nacos/OCR/gateway health 已经明显前进；但发版候选仍缺少三类硬证据：

1. 真实 sample gate 必须从 `SCAN_FAIL` 变为 PASS，至少要解释并修复 `AI_RECOGNITION / LLM returned empty response`。
2. 真实 `e2e:ai` 必须跑通并归档 Playwright artifact。
3. 必须提供真实旧库 dump 并完成 Flyway adoption / restore drill。

在这三项完成前，发版候选继续维持 `NO_GO`。
