# REPORT-13: 服务器试运行证据与 PLAN-00 关闭报告

**日期**: 2026-05-03
**分支**: `main`
**结论**: `PENDING_SERVER_INPUT`
**当前执行入口**: `PLAN-58-P0-服务器输入接入与放行窗口执行提示词.md`

---

## 1. 本轮结论

本轮已完成 PLAN-58 服务器输入预检查入口与 PLAN-00 历史收口任务的归档准备：

- PLAN-24 / PLAN-40~57 / CURRENT 已被 REPORT-07、PLAN-50~58、REPORT-10/11/12 覆盖，按历史覆盖项关闭或归档。
- PLAN-58 是当前唯一执行中入口，仍依赖服务器输入与服务器部署窗口证据。
- 已新增 `scripts/run-server-release-window.ps1` 作为服务器放行窗口执行脚本。脚本会先做非敏感输入检查，再在输入有效时执行服务器预检查、Compose health、AI sample gate、AI E2E、双角色真实栈黑盒与 PostgreSQL / MinIO / Nacos 恢复演练。

服务器窗口未执行，原因是当前执行环境没有提供：

- `SSH_TARGET`
- `APP_DIR`
- `BOBBUY_AGENT_AUTH_TOKEN`

因此本报告不能给出服务器 `GO` 证据，REPORT-07 仍保持 `GO_INTERNAL_TRIAL_PENDING_SERVER_WINDOW`。

---

## 2. 已关闭的历史覆盖项

| 计划 | 关闭口径 | 证据 |
| :-- | :-- | :-- |
| PLAN-24 | 稳定上线差距已收口到服务器窗口复跑 | REPORT-07、REPORT-10、REPORT-12 |
| PLAN-40 / 41 / 42 / 44 / 47 / 48 / 49 | 历史 NO-GO 收口计划已被后续发版门禁覆盖 | REPORT-07、REPORT-09、REPORT-10、REPORT-12 |
| PLAN-54 | 本地空库、seed、真实栈、备份恢复口径已闭环 | REPORT-12 |
| PLAN-55 | 试运行包封版复判口径已归档 | REPORT-07、REPORT-13 |
| PLAN-56 / 57 | 历史服务器封口与复判计划已被 PLAN-58 接管 | PLAN-58、REPORT-13 |
| CURRENT | 当前事实基线已拉平，动态服务器窗口状态由 PLAN-58 维护 | CURRENT-STATE、REPORT-13 |

---

## 3. 服务器输入预检查

| 输入 / 检查项 | 当前状态 | 说明 |
| :-- | :-- | :-- |
| `SSH_TARGET` | missing | 当前执行环境未提供 |
| `APP_DIR` | missing | 当前执行环境未提供 |
| `BRANCH` | default `main` | 未提供时按 `main` 执行 |
| `BOBBUY_AGENT_AUTH_TOKEN` | missing | 当前执行环境未提供；真实 AI sample gate 需要 |
| SSH 连通性 | not_run | 缺少 `SSH_TARGET`，未执行 SSH |
| 仓库目录存在性 | not_run | 缺少 `APP_DIR`，未执行远端目录检查 |
| 服务器 `.env` 存在性 | not_run | 缺少 `APP_DIR`，未执行远端文件检查 |

推荐执行入口：

```powershell
pwsh scripts/run-server-release-window.ps1
```

仅执行预检查：

```powershell
pwsh scripts/run-server-release-window.ps1 -PrecheckOnly
```

脚本在当前环境中的最近一次执行结果：

| 检查项 | 结果 |
| :-- | :-- |
| `SSH_TARGET` | missing |
| `APP_DIR` | missing |
| `BRANCH` | `main` |
| `BOBBUY_AGENT_AUTH_TOKEN` | missing |
| 退出码 | `2`，缺少 `SSH_TARGET` 或 `APP_DIR` |

底层预检查命令模板：

```bash
ssh "$SSH_TARGET" "hostname; date; test -d '$APP_DIR'; test -f '$APP_DIR/.env'"
```

该命令只允许输出主机名、时间与 pass/fail 摘要，不得输出 `.env` 内容。

---

## 4. 服务器窗口待执行项

| 验证项 | 当前状态 | 关闭条件 |
| :-- | :-- | :-- |
| SSH 部署窗口 | BLOCKED | 提供 `SSH_TARGET` 与 `APP_DIR` |
| 服务器 `.env` secret 存在性校验 | NOT_RUN | 只输出变量 present/missing，不输出值 |
| 服务器 Compose health | NOT_RUN | gateway / service / OCR health 通过 |
| 服务器真实 AI sample gate | NOT_RUN | `gatePassed=true` |
| 服务器真实 `e2e:ai` | NOT_RUN | Playwright 通过并归档 artifact |
| 服务器真实栈双角色黑盒 | NOT_RUN | `4 passed` |
| PostgreSQL / MinIO / Nacos 备份恢复 | NOT_RUN | 恢复校验摘要写入本报告 |

---

## 5. 服务器窗口证据模板

服务器窗口执行后，在本节记录摘要，不写入 secret 原文。

| 证据项 | 结果 | 摘要 / artifact |
| :-- | :-- | :-- |
| commit | NOT_RUN | 待记录服务器 `git rev-parse HEAD` |
| 执行时间 | NOT_RUN | 待记录服务器时区时间 |
| `.env` 必需变量存在性 | NOT_RUN | 只记录 present/missing |
| Compose 服务状态 | NOT_RUN | 待记录 `docker compose ps` 摘要 |
| Gateway health | NOT_RUN | `/api/health`、`/api/actuator/health`、`/api/actuator/health/readiness` |
| OCR health | NOT_RUN | `http://127.0.0.1:8000/health` |
| AI sample gate | NOT_RUN | 要求 `gatePassed=true` |
| AI E2E | NOT_RUN | 要求 Playwright 通过并归档 artifact |
| 真实栈双角色黑盒 | NOT_RUN | 要求 `4 passed` |
| PostgreSQL restore drill | NOT_RUN | 校验核心表与 `flyway_schema_history` |
| MinIO restore drill | NOT_RUN | 恢复 bucket 与探针对象 |
| Nacos archive drill | NOT_RUN | 归档文件路径与校验结果 |

---

## 6. 当前 PLAN-00 关闭判定

- 历史覆盖项：可关闭。
- PLAN-24：已关闭，服务器复跑转入 PLAN-58。
- PLAN-54：已关闭，服务器备份恢复复跑转入 PLAN-58。
- PLAN-55：已关闭，服务器最终复判转入 PLAN-58。
- PLAN-56：已关闭，执行口径已被 PLAN-58 接管。
- PLAN-57：已关闭，服务器输入预检查与窗口执行已被 PLAN-58 接管。
- PLAN-58：继续执行中，当前阻塞于服务器输入缺失。
- CURRENT：已关闭为事实基线，服务器窗口动态状态由 PLAN-58 / REPORT-13 维护。

---

## 7. 下一步输入

执行 PLAN-58 需要提供：

```text
SSH_TARGET=<user@server>
APP_DIR=<server repo path>
BRANCH=main
BOBBUY_AGENT_AUTH_TOKEN=<agent access token>
```

提供后执行：

```powershell
pwsh scripts/run-server-release-window.ps1
```

执行完成后按 `docs/plans/PLAN-58-P0-服务器输入接入与放行窗口执行提示词.md` 更新本报告。只有服务器窗口全部通过后，才允许将 REPORT-07 改为 `GO_INTERNAL_TRIAL` 并关闭 PLAN-58。
