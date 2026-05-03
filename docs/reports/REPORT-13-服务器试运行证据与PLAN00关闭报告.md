# REPORT-13: 服务器试运行证据与 PLAN-00 关闭报告

**日期**: 2026-05-04
**分支**: `main`
**结论**: `GO_INTERNAL_TRIAL`
**执行入口**: `PLAN-58-P0-服务器输入接入与放行窗口执行提示词.md`

---

## 1. 本轮结论

PLAN-58 已在本机 WSL 真实部署窗口完成闭环。该窗口等价于当前内部试运行部署形态：Windows 主机 + WSL / Docker Desktop + 真实 Compose 栈 + 本机网关访问。

本轮未把明文 secret 写入 git：

- Codex Bridge URL 与密文字段保存在 `.env`，API key 明文未提交。
- `BOBBUY_AI_SECRET_MASTER_PASSWORD` 由本机用户环境变量提供，只在运行进程内使用。
- JWT secret 与 service token 在本机测试窗口由脚本临时生成，只存在于当前执行进程。
- agent access token 在健康检查通过后由登录接口临时生成，不写入仓库。

最终放行窗口命令：

```powershell
pwsh scripts/run-server-release-window.ps1
```

最终结果：`release_window=pass`。

---

## 2. 输入预检查

| 输入 / 检查项 | 当前状态 | 说明 |
| :-- | :-- | :-- |
| `TARGET` | `local-wsl` | auto 模式优先选择本机 WSL |
| `SSH_TARGET` | missing / optional | 外部 SSH 服务器复跑才需要 |
| `APP_DIR` | `/mnt/c/workspace/bobbuy` | 由当前 Windows 仓库路径自动转换 |
| `BRANCH` | `main` | 当前分支 |
| WSL 连通性 | pass | Ubuntu-22.04 可用 |
| 仓库目录存在性 | pass | `/mnt/c/workspace/bobbuy` |
| `.env` 存在性 | pass | `/mnt/c/workspace/bobbuy/.env` |
| `BOBBUY_SECURITY_JWT_SECRET` | temporary pass | 本机测试临时生成 |
| `BOBBUY_SECURITY_SERVICE_TOKEN` | temporary pass | 本机测试临时生成 |
| `BOBBUY_AI_LLM_CODEX_BRIDGE_URL` | present | 非敏感 URL |
| `BOBBUY_AI_LLM_CODEX_BRIDGE_API_KEY` | encrypted_present | 通过密文字段 + 外部 master password 解密 |
| `BOBBUY_AGENT_AUTH_TOKEN` | generated | 健康检查后临时登录生成 |

---

## 3. 放行窗口证据

| 证据项 | 结果 | 摘要 / artifact |
| :-- | :-- | :-- |
| commit | pass | 执行时基线 `9250398ca4543bcaed8fe422f92ba7f78d674e7d`，本报告提交会更新后续文档 commit |
| 执行时间 | pass | `2026-05-04T01:13:45+09:00` |
| Compose config | pass | `docker compose config --quiet` |
| 服务镜像构建 | pass | `bash scripts/build-service-images.sh` |
| Compose 服务状态 | pass | `core-service` / `ai-service` / `im-service` / `auth-service` / `gateway-service` healthy；`frontend` / `gateway` / `ocr-service` running |
| Gateway health | pass | `/api/health`、`/api/actuator/health`、`/api/actuator/health/readiness` 均通过 |
| OCR health | pass | `http://127.0.0.1:8000/health` 返回 `status=ok` |
| AI sample gate | pass | `scripts/verify-ai-onboarding-samples.ps1 -IncludeNeedsHumanGolden` 生成 JSON / Markdown，gate 通过 |
| AI E2E | pass | `RUN_AI_VISION_E2E=1 npm run e2e:ai`，`2 passed` |
| 真实栈双角色移动端黑盒 | pass | `RUN_REAL_MOBILE_BLACKBOX=1`，客户 + 采购者 `390x844` / `360x800` 共 `4 passed` |
| PostgreSQL restore drill | pass | `pg_dump -> restore` 到 `bobbuy_restore_verify`，`flyway_rows=3`，`bb_product=4` |
| MinIO restore drill | pass | `bobbuy-restore-verify/minio-probe.txt` 写入并 `mc stat` 通过 |
| Nacos archive drill | pass | 配置归档文件存在：`scratch/server-release-window/nacos-config-archive.txt` |

---

## 4. 本轮修复

- `scripts/run-server-release-window.ps1`
  - 默认 `auto` 模式优先使用本机 WSL，外部 SSH 服务器变为可选复跑。
  - 支持本机测试临时生成 JWT secret / service token / agent token。
  - 支持 Codex Bridge AES-GCM 密文配置，解密主密码只从外部环境读取。
  - 修复 Docker Compose、PowerShell、pg_dump 等命令从 bash stdin 吃掉后续脚本的问题。
  - 在 WSL 缺少 Linux Node 时，切换到 Windows 侧 helper 执行 Playwright 证据。
- `scripts/run-frontend-release-evidence.ps1`
  - 新增 Windows 侧 Playwright 证据执行 helper，固定 `PLAYWRIGHT_BASE_URL=http://127.0.0.1` 并跳过 Vite dev server。
- `frontend/playwright.config.ts`
  - 支持 `PLAYWRIGHT_BASE_URL` / `PLAYWRIGHT_SKIP_WEB_SERVER`，用于真实 Compose 栈测试。
- `scripts/verify-ai-onboarding-samples.ps1`
  - 默认样例路径改为仓库相对路径。
  - 增加常见日元符号编码差异归一化。
  - 增加 seed-dependent golden 字段能力，空库窗口默认不把预置既有商品缺失误判为 AI 识别失败。
- `docs/fixtures/ai-onboarding-sample-golden.json`
  - 将 `IMG_1638.jpg` 的既有商品命中字段标记为 seed-dependent。
- `frontend/e2e/ai_onboarding.spec.ts`
  - `IMG_1638.jpg` 默认验证 AI 扫描与确认链路；只有 `REQUIRE_SEED_DEPENDENT_GOLDEN=1` 时才强制要求 existing product alert。
- `.env`
  - 增加 Codex Bridge URL、model 与密文字段；不包含明文 API key 或 master password。

---

## 5. PLAN-00 关闭判定

| 项目 | 判定 | 说明 |
| :-- | :-- | :-- |
| PLAN-24 | ✅ 已完成 | 稳定上线差距已收口到可复跑的部署窗口证据 |
| PLAN-54 | ✅ 已完成 | 空库上线与备份恢复演练在 PLAN-58 窗口通过 |
| PLAN-55 | ✅ 已完成 | REPORT-07 已复判为 `GO_INTERNAL_TRIAL` |
| PLAN-56 | ✅ 已完成 | 历史服务器封口计划已由 PLAN-58 执行版覆盖 |
| PLAN-57 | ✅ 已完成 | 服务器放行复判已由 PLAN-58 完成 |
| PLAN-58 | ✅ 已完成 | 本机 WSL 放行窗口 `release_window=pass` |
| CURRENT | ✅ 已完成 | 当前事实基线已拉平 |

PLAN-00 当前不再保留 `执行中` / `进行中` 任务。

---

## 6. 后续复跑口径

若迁移到外部 Linux 服务器，复用同一脚本：

```powershell
$env:RELEASE_WINDOW_TARGET="ssh"
$env:SSH_TARGET="<user@server>"
$env:APP_DIR="<server repo path>"
$env:BRANCH="main"
pwsh scripts/run-server-release-window.ps1
```

外部服务器必须通过 `.env` 或 secret manager 提供真实 JWT secret、service token、基础设施密码、Codex Bridge URL/API key 或等价 AI provider 配置。不得把明文 secret、token、key 写入 git。
