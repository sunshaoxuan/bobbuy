# 本地 / CI 测试执行矩阵

> 2026-04-29 更新：默认上线门禁以 `.github/workflows/ci.yml` 为准。后端 `mvn test`、前端 `npm test`、前端 `npm run build` 已恢复为默认门禁；Playwright、AI 真实视觉链路与安全扫描按分层策略执行。

## 1. 默认门禁（每个 PR / `main` push 必跑）

| 验证项 | 本地命令 | CI job / step | 默认门禁 | 外部依赖 |
| :-- | :-- | :-- | :-- | :-- |
| 后端测试 | `cd /home/runner/work/bobbuy/bobbuy/backend && mvn test` | `backend-test` / `Run backend tests` | 是 | 否；默认使用测试资源与 fake/mock 配置，不依赖真实 Ollama、Codex CLI、MinIO、外网 AI 服务 |
| 前端单测 | `cd /home/runner/work/bobbuy/bobbuy/frontend && npm test` | `frontend-quality` / `Run frontend tests` | 是 | 否 |
| 前端构建 | `cd /home/runner/work/bobbuy/bobbuy/frontend && npm run build` | `frontend-quality` / `Build frontend` | 是 | 否 |
| 后端 Docker 构建 | `cd /home/runner/work/bobbuy/bobbuy/backend && mvn -DskipTests package && cd /home/runner/work/bobbuy/bobbuy && docker build backend -t bobbuy-backend-test` | `docker-build` / `Build backend package for Docker image` + `Build backend image` | 是 | 否 |
| 前端 Docker 构建 | `cd /home/runner/work/bobbuy/bobbuy && docker build frontend -t bobbuy-frontend-test` | `docker-build` / `Build frontend image` | 是 | 否 |

## 2. 专用环境门禁（手动触发 / 条件执行）

| 验证项 | 本地命令 | CI 触发方式 | 默认门禁 | 环境要求 |
| :-- | :-- | :-- | :-- | :-- |
| Playwright 页面回归 | `cd /home/runner/work/bobbuy/bobbuy/frontend && npm run e2e` | `workflow_dispatch` + `playwright-e2e` job（输入 `run_playwright_e2e=true`） | 否 | GitHub Hosted Runner 可执行；常规用例走前端共享 mock，不依赖真实 AI / MinIO |
| AI 真实视觉链路 | `cd /home/runner/work/bobbuy/bobbuy/frontend && npm run e2e:ai` | 不纳入默认 Hosted CI；仅在专用环境手动执行并单独记录结果 | 否 | 必须提供 `RUN_AI_VISION_E2E=1`、`SPRING_PROFILES_ACTIVE=dev,ai-hermes`、可访问的 AI 模型、MinIO、seed 数据与样本图片 |

## 3. 风险登记 / 独立安全门禁

| 验证项 | 默认状态 | 执行要求 | 备注 |
| :-- | :-- | :-- | :-- |
| CodeQL / 安全扫描 | 不纳入默认 `ci.yml` | 独立执行；若本次 PR / Release 未执行，必须登记为风险项，不得写成“已通过” | 如出现超时或平台限制，需记录原因与缓解动作 |
| 依赖审计 | 不纳入默认 `ci.yml` | 按需独立执行；未执行时同样登记风险 | 重点关注 npm / Maven 高危依赖 |

## 4. 执行约束与已知噪声

- `frontend-quality` job 通过一次 `npm ci --prefix frontend` 复用同一份依赖，避免测试与构建重复安装。
- `playwright-e2e` 继续复用 `npm run e2e:prepare` 的浏览器探测逻辑，仅在缺失 Chromium 时安装。
- 后端默认测试必须继续使用 H2 / fake/mock 资源，禁止默认门禁外连真实 Ollama、Codex CLI、MinIO 或公网服务。
- 后端 `mvn test` 现同时覆盖 JWT 登录、`/api/auth/me`、401/403、customer 本人数据隔离，以及 `bobbuy.security.header-auth.enabled=false` 时伪造 header 不得提权。
- 当前已知前端测试噪声：
  - Ant Design `useForm` 未连接 warning。
  - 预期失败路径中的 `Delete failed: Error: Server error` console 输出。
  以上噪声目前不阻断默认门禁，但应继续在后续清理项中跟踪。

## 5. 2026-04-29 本地验证结果

- [x] `cd /home/runner/work/bobbuy/bobbuy/backend && mvn test`
- [x] `cd /home/runner/work/bobbuy/bobbuy/frontend && npm test`
- [x] `cd /home/runner/work/bobbuy/bobbuy/frontend && npm run build`
- [ ] `cd /home/runner/work/bobbuy/bobbuy/frontend && npm run e2e`
  - 本次实际执行：`44 passed / 2 failed / 2 skipped`
  - 失败用例：`chat_publish_flow.spec.ts`、`client_role_gate.spec.ts`
- [ ] `cd /home/runner/work/bobbuy/bobbuy/frontend && npm run e2e:ai`
- [ ] CodeQL / 依赖审计（未纳入默认门禁，需单独执行或在 PR / Release 中登记）
