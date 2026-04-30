# REPORT-07: NO-GO 阻断项解阻与放行复判

**日期**: 2026-04-30  
**分支**: `copilot/plan-44-default-ci-restore`  
**提交**: `c3b0192675ccafe6af5fd6d73b261261fb4aa030`

---

## 1. 最终结论

- **放行判定**: **NO_GO**
- **结论原因**: 最新 main 默认 CI 已恢复全绿，但真实 AI/OCR sample、真实 `e2e:ai`、真实旧库 adoption / restore drill 仍无可信通过证据；同时 Maven dependency-check 最新 main run 仍在执行中，CodeQL 仍缺默认分支 push 分析与可审计 alert 数。

---

## 2. 已确认的放行证据

### 2.1 默认 CI 已恢复

- workflow: `.github/workflows/ci.yml`
- 最新 main run: <https://github.com/sunshaoxuan/bobbuy/actions/runs/25178072203>
- branch / sha: `main` / `62ddb16109eb471b4886b4a82407f443342115c1`
- 结果: `success`
- jobs:
  - `backend-test`: success
  - `frontend-quality`: success
  - `docker-build`: success
  - `playwright-e2e` / `postgres-migration-verify` / `maven-dependency-audit`: skipped（未手动开启）

**复判**: PLAN-44 的首要 blocker“默认 CI 恢复”已完成，frontend Docker image 不再因 `npm install` / `ECONNRESET` 造成默认门禁失败。

### 2.2 CodeQL 已有成功 run，但默认分支证据仍未闭环

- workflow: `.github/workflows/codeql.yml`
- 当前触发方式（本次修正后）: `push`（`main`、`copilot/**`）+ `pull_request`（`main`）+ `workflow_dispatch`
- 最新触发 run: <https://github.com/sunshaoxuan/bobbuy/actions/runs/25178669741>
- branch / sha: `copilot/plan-44-default-ci-restore` / `c3b0192675ccafe6af5fd6d73b261261fb4aa030`
- 结果: `action_required`
- 现象: run 中 **0 jobs**，用于验证恢复后的 `push` 触发路径，但当前仍受 GitHub Actions 审批/权限影响
- 最新成功 run: <https://github.com/sunshaoxuan/bobbuy/actions/runs/25177727147>
- branch / sha: `copilot/plan-43-execute-no-go-prompt` / `8bdb649bfdf37d2ae42146d92028451c8d54dd13`
- 结果: `success`
- matrix:
  - `analyze (actions)`: success
  - `analyze (javascript-typescript)`: success
  - `analyze (java-kotlin)`: success
- 当前缺口:
  - GitHub 页面提示此前 workflow 缺少 `on.push`，默认分支 Security tab 无法形成新的 Code Scanning 基线
  - 通过当前 GitHub integration 调用 code scanning alerts API 返回 `403 Resource not accessible by integration`，本轮无法直接读取 alert 数

**复判**: CodeQL 已从“无法运行”推进到“矩阵可成功执行”；但恢复后的 `push` 触发在 Copilot 分支上仍被 GitHub Actions 审批/权限阻塞，默认分支 push 分析与 alert 数仍需在下一次可执行的 `main` push 后重新归档。

### 2.3 Maven dependency-check 已在 main 上持续执行

- workflow: `.github/workflows/dependency-check.yml`
- 最新 main run: <https://github.com/sunshaoxuan/bobbuy/actions/runs/25177731775>
- branch / sha: `main` / `22fee1a427a8602ec5fcc7b0f6d9a6b74cef920e`
- 当前状态: `in_progress`
- job:
  - `dependency-check`: 仍停留在 `Run OWASP dependency-check`
- artifact:
  - 期望 artifact 名: `dependency-check-report`
  - 期望路径: `/tmp/bobbuy-dependency-check/dependency-check-report.html`、`/tmp/bobbuy-dependency-check/dependency-check-report.json`
  - 当前尚未生成可下载 artifact

**复判**: 依赖审计已不再是 `action_required`，但本轮仍只能记录为“执行中”；在 HTML/JSON artifact 与严重级别摘要落档前，不能写成通过。

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
| 默认 CI | RESOLVED | main run `25178072203` 已 success | 已解阻 |
| CodeQL 默认分支证据 | BLOCKED | 恢复 `push` 后的验证 run `25178669741` 仍为 `action_required`（0 jobs）；仍需产出默认分支 analysis，并补录 code scanning alerts 数 | 仓库管理员 / 安全负责人 |
| Maven dependency-check | BLOCKED | run `25177731775` 仍为 `in_progress`，无 HTML/JSON artifact 与严重级别摘要 | 仓库管理员 / 安全负责人 |
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
2. CodeQL 矩阵 run 已成功，但默认分支 push 证据与 alert 数仍未补齐。
3. Maven dependency-check 最新 main run 仍在执行中，尚无报告 artifact。
4. 真实 AI sample、真实 AI E2E、真实旧库 adoption / restore drill 仍无可信证据。

因此当前仍不能放行发版候选。
