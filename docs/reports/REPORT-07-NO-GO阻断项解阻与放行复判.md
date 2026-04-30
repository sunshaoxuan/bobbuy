# REPORT-07: NO-GO 阻断项解阻与放行复判

**日期**: 2026-04-30  
**分支**: `main`
**提交**: `a66d0866cfe805d0c322bc508aa044e493fa48d4`

---

## 1. 最终结论

- **放行判定**: **NO_GO**
- **结论原因**: 最新 main 默认 CI、CodeQL main push 与 Maven dependency-check 均已成功，但 CodeQL 仍有 3 个 open high alert，Maven dependency-check 成功 run 未产生可下载 artifact；真实 AI/OCR sample、真实 `e2e:ai`、真实旧库 adoption / restore drill 仍无可信通过证据。

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

### 2.2 CodeQL main push 已成功，但仍有 open high alert

- workflow: `.github/workflows/codeql.yml`
- 当前触发方式（本次修正后）: `push`（`main`、`copilot/**`）+ `pull_request`（`main`）+ `workflow_dispatch`
- 最新 main run: <https://github.com/sunshaoxuan/bobbuy/actions/runs/25192905342>
- branch / sha: `main` / `a66d0866cfe805d0c322bc508aa044e493fa48d4`
- 结果: `success`
- matrix:
  - `analyze (actions)`: success
  - `analyze (javascript-typescript)`: success
  - `analyze (java-kotlin)`: success
- 当前 open high alerts:
  - `backend/src/main/java/com/bobbuy/SecurityConfig.java`: `java/spring-disabled-csrf-protection`
  - `docs/design/assets/ui-merchant-framework.js`: `js/xss-through-dom` x2

**复判**: CodeQL 默认分支证据已闭环，但存在 3 个 open high alert；在修复或正式豁免前，安全门禁仍不能视为通过。

### 2.3 Maven dependency-check main run 已成功，但 artifact 归档失败

- workflow: `.github/workflows/dependency-check.yml`
- 最新 main run: <https://github.com/sunshaoxuan/bobbuy/actions/runs/25177731775>
- branch / sha: `main` / `22fee1a427a8602ec5fcc7b0f6d9a6b74cef920e`
- 当前状态: `success`
- job:
  - `dependency-check`: success
- artifact:
  - 期望 artifact 名: `dependency-check-report`
  - 实际报告路径: `backend/target/dependency-check-report.html`、`backend/target/dependency-check-report.json`
  - 原上传路径: `/tmp/bobbuy-dependency-check`
  - 当前结果: run 成功但无可下载 artifact；已在后续修正 workflow 上传路径，需重跑确认

**复判**: 依赖审计执行已解阻，但证据归档未闭环；在 HTML/JSON artifact 与严重级别摘要落档前，不能写成通过。

### 2.4 AI 专用环境执行链路已具备，但真实证据仍缺失

- workflow: `.github/workflows/ai-release-evidence.yml`
- 当前 run 数: 0
- 真实 sample gate 命令与 `RUN_AI_VISION_E2E=1 npm run e2e:ai` 已固化到 workflow
- 当前缺口:
  - 真实后端 URL
  - 真实 OCR / LLM / seed 环境
  - `AI_E2E_AGENT_PASSWORD`
  - sample JSON/Markdown report
  - Playwright trace / screenshot / video / HTML artifact

### 2.5 真实旧库 adoption / restore drill 仍未执行

- 当前状态: 未提供真实旧库副本或历史 schema dump
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
| CodeQL 默认分支证据 | BLOCKED | main run `25192905342` 已成功，但仍有 3 个 open high alert 需要修复或正式豁免 | 安全负责人 / 全栈团队 |
| Maven dependency-check | BLOCKED | run `25177731775` 已成功，但 artifact 上传路径错误导致无 HTML/JSON 可下载报告；需重跑修正后的 workflow | 仓库管理员 / 安全负责人 |
| 真实 AI sample 实扫 | BLOCKED | 尚未提供真实后端 URL、provider、seed、专用环境运行记录 | AI/OCR 负责人 |
| 真实 `RUN_AI_VISION_E2E=1 npm run e2e:ai` | BLOCKED | 尚未提供真实 agent 凭据、专用后端与 Playwright artifact | AI/OCR / 前端负责人 |
| 真实旧库 adoption / restore drill | BLOCKED | 仍缺真实旧库副本或历史 schema dump、隔离库与 DBA 执行记录 | DBA / 发布负责人 |

---

## 4. 本轮本地验证

- `cd /home/runner/work/bobbuy/bobbuy && docker compose config`
- `cd /home/runner/work/bobbuy/bobbuy/backend && mvn test`
- `cd /home/runner/work/bobbuy/bobbuy/frontend && npm ci && npm test`
- `cd /home/runner/work/bobbuy/bobbuy/frontend && npm run build`
- `cd /home/runner/work/bobbuy/bobbuy/backend && mvn -DskipTests package`
- `cd /home/runner/work/bobbuy/bobbuy && docker build backend -t bobbuy-backend-test`
- `cd /home/runner/work/bobbuy/bobbuy && docker build frontend -t bobbuy-frontend-test`
- workflow YAML 校验：
  - `.github/workflows/codeql.yml`
  - `.github/workflows/dependency-check.yml`
  - `.github/workflows/ai-release-evidence.yml`

结果：均通过。

---

## 5. 最终复判

**NO_GO**

当前已确认的事实：

1. 默认 CI 已恢复，全绿证据已具备。
2. CodeQL 默认分支矩阵 run 已成功，但仍有 3 个 open high alert。
3. Maven dependency-check 最新 main run 已成功，但尚无报告 artifact。
4. 真实 AI sample、真实 AI E2E、真实旧库 adoption / restore drill 仍无可信证据。

因此当前仍不能放行发版候选。
