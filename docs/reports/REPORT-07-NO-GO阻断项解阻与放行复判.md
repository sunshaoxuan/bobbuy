# REPORT-07: NO-GO 阻断项解阻与放行复判

**日期**: 2026-05-01  
**分支**: `main`
**提交**: `4b44431b5b13d4b34804014473c795c69774b5c7`

---

## 1. 最终结论

- **放行判定**: **NO_GO**
- **结论原因**: 最新 main 默认 CI、CodeQL 与 Maven dependency-check 均已成功，3 个 CodeQL high alert 已标记 `fixed`，dependency-check artifact 可下载且含 HTML/JSON；但 Maven dependency-check 仍报告 `8 critical / 21 high / 19 moderate`，真实 AI/OCR sample、真实 `RUN_AI_VISION_E2E=1 npm run e2e:ai` 与真实旧库 adoption / restore drill 仍无可信通过证据。

---

## 2. 已确认的放行证据

### 2.1 默认 CI 已恢复

- workflow: `.github/workflows/ci.yml`
- 最新 main run: <https://github.com/sunshaoxuan/bobbuy/actions/runs/25192905348>
- branch / sha: `main` / `a66d0866cfe805d0c322bc508aa044e493fa48d4`
- 结果: `success`
- jobs:
  - `backend-test`: success
  - `frontend-quality`: success
  - `docker-build`: success
  - `playwright-e2e` / `postgres-migration-verify` / `maven-dependency-audit`: skipped（未手动开启）

**复判**: PLAN-44 的首要 blocker“默认 CI 恢复”已完成，frontend Docker image 不再因 `npm install` / `ECONNRESET` 造成默认门禁失败。

### 2.2 CodeQL high alert 已在 main 上清零

- workflow: `.github/workflows/codeql.yml`
- 已修复源码：
  - `backend/src/main/java/com/bobbuy/SecurityConfig.java`: 移除 `csrf.disable()`，改为 Spring Security 对 cookie-backed `/api/auth/refresh`、`/api/auth/logout` 强制 CSRF 校验，并保留 JSON 403 返回
  - `docs/design/assets/ui-merchant-framework.js`: 去掉 `pageTitle` / `pageId` 的 HTML 模板插值，改为占位节点 + `textContent`
- 最新 main success run: <https://github.com/sunshaoxuan/bobbuy/actions/runs/25198280107>
- branch / sha: `main` / `4b44431b5b13d4b34804014473c795c69774b5c7`
- code scanning alerts API:
  - `java/spring-disabled-csrf-protection`: `fixed`
  - `js/xss-through-dom` x2: `fixed`

**复判**: CodeQL high alert blocker 已解阻。

### 2.3 Maven dependency-check artifact 已闭环，且 HTML/JSON 可下载

- workflow: `.github/workflows/dependency-check.yml`
- 最新 main run: <https://github.com/sunshaoxuan/bobbuy/actions/runs/25198280108>
- branch / sha: `main` / `4b44431b5b13d4b34804014473c795c69774b5c7`
- 结果: `success`
- artifact:
  - artifact 名: `dependency-check-report`
  - artifact id: `6744112430`
  - 已验证可下载，ZIP 内含：
    - `dependency-check-report.html`
    - `dependency-check-report.json`
- JSON 摘要（unique CVE 口径）：
  - `critical`: 8
  - `high`: 21
  - `moderate`(`medium`): 19

**复判**: “artifact 可下载”这一 blocker 已解阻，但报告中仍存在大量 critical/high/moderate 依赖风险；在完成升级、修复或正式豁免前，安全门禁仍不能视为通过。

### 2.4 AI 专用环境执行链路已具备，但真实证据仍缺失

- workflow: `.github/workflows/ai-release-evidence.yml`
- 当前 run 数: 0
- 真实 sample gate 命令与 `RUN_AI_VISION_E2E=1 npm run e2e:ai` 已固化到 workflow
- 本轮沙箱核验：
  - `.env` 中 `BOBBUY_AI_LLM_MAIN_PROVIDER`、`BOBBUY_AI_LLM_MAIN_URL`、`BOBBUY_OCR_URL`、`BOBBUY_SEED_ENABLED` 为已配置状态
  - 但 `BOBBUY_API_PROXY_TARGET`、`BOBBUY_WS_PROXY_TARGET`、`BOBBUY_E2E_AGENT_USERNAME`、`BOBBUY_E2E_AGENT_PASSWORD` 未配置
  - `http://localhost/api/health` / `http://127.0.0.1/api/health` 当前均不可达
  - 已尝试 `docker compose up -d ...` 拉起真实栈，但 `Dockerfile.service` 仍在容器内 Maven 构建阶段因 `repo.maven.apache.org` `PKIX path building failed` 阻塞，未能进入可执行 sample gate / `e2e:ai` 的状态
- 当前缺口:
  - 可访问的真实后端入口
  - 真实 OCR / LLM / seed 环境联通证明
  - 可用的 agent 登录凭据或可达 seed 登录入口
  - sample JSON/Markdown report
  - Playwright trace / screenshot / video / HTML artifact

### 2.5 真实旧库 adoption / restore drill 仍未执行

- 当前状态: 仓库工作区内未发现真实旧库副本 / 历史 schema dump / restore 备份文件；本轮无法在不引入外部数据的前提下执行真实 adoption drill
- 当前缺口:
  - 数据来源与 dump 时间
  - 脱敏方式
  - 隔离 PostgreSQL 环境
  - `baseline` / `migrate` / `validate` / `restore drill` 记录

---

## 3. 仍阻断发版的事项

| blocker | 当前状态 | 仍缺证据 / 阻塞 | 责任边界 |
| :-- | :-- | :-- | :-- |
| 默认 CI | RESOLVED | main run `25192905348` 已 success | 已解阻 |
| CodeQL 默认分支证据 | RESOLVED | main run `25198280107` 已 success，3 个 high alert 均为 fixed | 已解阻 |
| Maven dependency-check | BLOCKED | main run `25198280108` artifact 已可下载，但报告中 `8 critical / 21 high / 19 moderate` 仍需风险处置或正式豁免 | 安全负责人 / 仓库管理员 |
| 真实 AI sample 实扫 | BLOCKED | 本轮尝试拉起真实 compose 栈时，service 镜像 Maven-in-Docker 因 PKIX 失败未能完成，尚无 sample JSON/Markdown 报告 | AI/OCR / 平台负责人 |
| 真实 `RUN_AI_VISION_E2E=1 npm run e2e:ai` | BLOCKED | 同上，真实后端入口与 Playwright artifact 仍未形成 | AI/OCR / 前端负责人 |
| 真实旧库 adoption / restore drill | BLOCKED | 仓库内未提供真实旧库副本或历史 schema dump，无法执行 adoption / restore | DBA / 发布负责人 |

---

## 4. 本轮本地验证

- `cd /home/runner/work/bobbuy/bobbuy && docker compose config`
- `cd /home/runner/work/bobbuy/bobbuy/backend && mvn test`
- `cd /home/runner/work/bobbuy/bobbuy/backend && mvn -Dtest='AuthControllerIntegrationTest,SecurityAuthorizationIntegrationTest,SecurityAuthorizationProductionModeIntegrationTest,AuthRefreshTokenExpiryIntegrationTest' test`
- `cd /home/runner/work/bobbuy/bobbuy/frontend && npm ci && npm test`
- `cd /home/runner/work/bobbuy/bobbuy/frontend && npm run build`
- 下载并校验 dependency-check artifact `6741960133`
- `cd /home/runner/work/bobbuy/bobbuy && docker compose up -d postgres minio redis rabbitmq nacos nacos-init core-service ai-service im-service auth-service gateway-service frontend gateway ocr-service`

结果：

- 默认本地门禁命令通过。
- dependency-check artifact 可下载且含 HTML/JSON。
- 真实 compose 环境拉起失败：`Dockerfile.service` 容器内 Maven 访问 `repo.maven.apache.org` 触发 `PKIX path building failed`，因此未能进入真实 sample gate / `e2e:ai` / adoption drill。

---

## 5. 最终复判

**NO_GO**

当前已确认的事实：

1. 默认 CI 已恢复，全绿证据已具备。
2. CodeQL 3 个 high 对应源码修复已在 main 上复扫，alerts 均为 `fixed`。
3. Maven dependency-check 最新 main run 已成功，artifact 可下载，且已记录 `8 critical / 21 high / 19 moderate`（unique CVE）摘要；该风险仍未处置。
4. 真实 AI sample、真实 AI E2E、真实旧库 adoption / restore drill 仍无可信证据；本轮已确认当前主要阻塞为 compose 服务镜像 Maven PKIX 与缺少真实旧库 dump。

因此当前仍不能放行发版候选。
