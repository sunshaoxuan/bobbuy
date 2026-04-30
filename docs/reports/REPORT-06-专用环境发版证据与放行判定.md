# REPORT-06: 专用环境发版证据与放行判定

**日期**: 2026-04-30  
**分支**: `copilot/plan-42-p0-special-environment-release`  
**提交**: `8f890fe144d5fa22f03c636d3ccb16d1c9e28ff7`

---

## 1. 最终结论

- **放行判定**: **NO_GO**
- **原因**: PLAN-42 要求的五类专用环境 blocker 中，CodeQL 实跑、Maven 可信依赖审计、真实 AI sample 实扫、真实 `npm run e2e:ai`、真实旧库 adoption / restore drill 仍未全部形成可审计证据。
- **当前已补强证据**:
  - GitHub Actions 默认 CI 最近一次 `main` 分支 run 成功：<https://github.com/sunshaoxuan/bobbuy/actions/runs/25141502571>
  - 本地/沙箱已复验 `docker compose config`、`backend mvn test`、`frontend npm ci && npm test`、`frontend npm run build`、`frontend npm run e2e`
  - sample dry-run gate 通过，JSON 报告已生成：`/tmp/plan42-sample-pass.json`
  - 前端依赖审计维持 `0 critical / 0 high / 6 moderate`

---

## 2. 默认门禁与现有证据

### 2.1 GitHub Actions 默认 CI

- workflow: `BOBBuy CI`
- run URL: <https://github.com/sunshaoxuan/bobbuy/actions/runs/25141502571>
- branch / sha: `main` / `8f890fe144d5fa22f03c636d3ccb16d1c9e28ff7`
- 时间: `2026-04-30T00:46:10Z` ~ `2026-04-30T00:48:25Z`

| Job | 结论 |
| :-- | :-- |
| `frontend-quality` | success |
| `backend-test` | success |
| `docker-build` | success |
| `playwright-e2e` | skipped（`workflow_dispatch` 条件未开启） |
| `postgres-migration-verify` | skipped（`workflow_dispatch` 条件未开启） |

### 2.2 本地/沙箱复验

| 验证项 | 结果 |
| :-- | :-- |
| `cd /home/runner/work/bobbuy/bobbuy && docker compose config` | 通过 |
| `cd /home/runner/work/bobbuy/bobbuy/backend && mvn test` | 通过 |
| `cd /home/runner/work/bobbuy/bobbuy/frontend && npm ci && npm test` | 通过 |
| `cd /home/runner/work/bobbuy/bobbuy/frontend && npm run build` | 通过 |
| `cd /home/runner/work/bobbuy/bobbuy/frontend && npm run e2e` | 通过（`46 passed / 2 skipped`） |
| sample dry-run gate | 通过（`/tmp/plan42-sample-pass.json`，`summary.gatePassed=true`） |
| `cd /home/runner/work/bobbuy/bobbuy/frontend && npm audit --json` | `0 critical / 0 high / 6 moderate` |

---

## 3. CodeQL 实跑状态

- workflow 文件：`/home/runner/work/bobbuy/bobbuy/.github/workflows/codeql.yml`
- GitHub Actions 中 `CodeQL` workflow 当前 **run 数量为 0**
- 因未形成任何 workflow run，当前不存在可归档的:
  - run URL
  - matrix 结果
  - code scanning alerts 处置结论

**结论**: CodeQL 专用环境证据 **未完成，属于 blocker**。

---

## 4. Maven 依赖审计状态

执行命令：

```bash
cd /home/runner/work/bobbuy/bobbuy/backend
mvn -B org.owasp:dependency-check-maven:12.1.8:check \
  -Dformat=HTML,JSON \
  -DoutputDirectory=/tmp/plan42-dependency-check \
  -DskipProvidedScope=true \
  -DskipTestScope=true
```

结果：

- **状态**: BLOCKED
- **错误**: `UnknownHostException: www.cisa.gov: No address associated with hostname`
- **附带错误**: `NoDataException: No documents exist`
- **产物**: 未生成 `/tmp/plan42-dependency-check` HTML/JSON 报告

**结论**: 后端可信依赖审计 **未完成，属于 blocker**；需在可访问 NVD / CISA KEV 或内部缓存镜像的环境重跑。

---

## 5. 真实 AI/OCR 专用环境状态

### 5.1 sample 字段级实扫

- 当前仅完成 mock dry-run：
  - 命令返回 `0`
  - `/tmp/plan42-sample-pass.json` 显示 `summary.gatePassed=true`
- 当前**未**在真实 `/api/ai/onboard/scan`、真实 OCR provider、真实 LLM provider、真实 seed 数据环境执行

**结论**: 真实 sample 实扫 **未完成，属于 blocker**。

### 5.2 Playwright AI 真实视觉链路

- 已完成默认 smoke：`cd /home/runner/work/bobbuy/bobbuy/frontend && npm run e2e` → `46 passed / 2 skipped`
- 当前**未**执行：`cd /home/runner/work/bobbuy/bobbuy/frontend && npm run e2e:ai`
- 当前**未**归档真实 AI/OCR/MinIO/seed 环境下的 HTML report / trace / screenshot / video

**结论**: 真实 `e2e:ai` 证据 **未完成，属于 blocker**。

---

## 6. 真实旧库 Flyway adoption 状态

- 当前没有真实旧库副本或历史 schema dump
- 当前没有隔离数据库中的 baseline / migrate / validate / restore drill 记录
- 既有空库 migration 与恢复演练不能替代真实旧库 adoption 证据

**结论**: 真实旧库 adoption / restore drill **未完成，属于 blocker**。

---

## 7. 最终放行判定

### 7.1 判定结果

**NO_GO**

### 7.2 仍阻断发版的事项

1. CodeQL workflow 尚未实跑，run 数量仍为 `0`
2. Maven dependency-check 未形成可信 HTML/JSON 报告
3. 真实 `/api/ai/onboard/scan` sample 实扫未执行
4. 真实 `RUN_AI_VISION_E2E=1 npm run e2e:ai` 未执行
5. 真实旧库 adoption / baseline / validate / restore drill 未执行

### 7.3 发版前必须补齐的证据

1. GitHub Actions `CodeQL` run URL 与 matrix 结果
2. Maven dependency-check HTML/JSON 报告
3. 真实 AI sample JSON/Markdown report
4. 真实 AI E2E Playwright artifact
5. 真实旧库 adoption / restore drill 记录

