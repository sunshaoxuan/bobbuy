# PLAN-38: P2 Playwright 端到端试运行验收开发提示词

**生效日期**: 2026-04-29
**状态**: 待评审
**关联计划**: [PLAN-24: 稳定上线差距收口优先级](PLAN-24-稳定上线差距收口优先级.md)
**前置任务**:
- [PLAN-27: P0 上线验收矩阵与 CI 固化开发提示词](PLAN-27-P0上线验收矩阵与CI固化开发提示词.md)
- [PLAN-28: P1 认证与权限生产化开发提示词](PLAN-28-P1认证与权限生产化开发提示词.md)
- [PLAN-34: P2 WebSocket 与服务间鉴权收口开发提示词](PLAN-34-P2-WebSocket与服务间鉴权收口开发提示词.md)
- [PLAN-36: P2 Refresh Token 与会话生命周期治理开发提示词](PLAN-36-P2-RefreshToken与会话生命周期治理开发提示词.md)
- [PLAN-37: P2 浏览器 Token 防护与 Refresh 并发硬化开发提示词](PLAN-37-P2-浏览器Token防护与Refresh并发硬化开发提示词.md)
**事实基线**: [CURRENT-STATE-2026-04-28](../reports/CURRENT-STATE-2026-04-28.md)

---

## 1. 开发目标

在后端、前端单元测试和 Docker 配置已经稳定后，补齐真实浏览器端到端试运行验收。目标不是扩大到完整 AI 真实视觉链路，而是让核心登录、角色门禁、订单/行程、账单、聊天/WebSocket、库存上架人工接管等浏览器路径形成可重复执行的 Playwright smoke 套件，并把它作为试运行上线前的手动门禁。

当前事实：

1. `backend mvn test`、`frontend npm test`、`frontend npm run build`、`docker compose config` 已是默认门禁。
2. Playwright E2E 曾登记为手动/专用环境项，历史上存在 `chat_publish_flow.spec.ts`、`client_role_gate.spec.ts` 失败记录。
3. 当前认证已从 access token + localStorage refresh token 迁移到 HttpOnly refresh cookie + CSRF double-submit。
4. WebSocket 已使用 access token 鉴权，refresh 后应使用新 access token 重连。
5. AI 真实视觉链路、OCR 真实服务和外部 LLM 不应进入默认 smoke；它们仍属于专用环境门禁。

---

## 2. 核心 Prompt 文本（请审查）

您可以直接将以下内容作为下一次开发任务的输入：

```text
### [任务: P2 Playwright 端到端试运行验收]

任务背景：
BOBBuy 已完成认证生产化、refresh token、HttpOnly cookie + CSRF、WebSocket 鉴权、服务间 service token、服务壳 smoke test 和默认单元测试门禁。当前离稳定上线的关键缺口是：真实浏览器路径尚未形成稳定、可重复、文档化的 Playwright 试运行验收。请修复/重构现有 E2E，使核心业务 smoke 在本地和 CI 手动 job 中可靠通过。

目标：
1. 建立可重复运行的 Playwright smoke 套件，覆盖公网试运行最关键路径。
2. 修复现有失败或脆弱用例，尤其是登录态、角色门禁、聊天/WebSocket、cookie/CSRF 后的请求行为。
3. 明确默认 E2E 与 AI 真实视觉/E2E 专用环境的边界。
4. 将 E2E 执行方式写入 TEST-MATRIX、README、Runbook 和 PLAN-24。
5. 不把真实 OCR、真实 LLM、真实支付、外部地图等不稳定依赖纳入默认 smoke。

必须先做的排查：
1. 阅读前端 E2E 与测试配置：
   - `frontend/playwright.config.*`
   - `frontend/e2e/**`
   - `frontend/package.json`
   - `.github/workflows/ci.yml`
2. 阅读认证与会话实现：
   - `frontend/src/api.ts`
   - `frontend/src/authStorage.ts`
   - `frontend/src/context/UserRoleContext.tsx`
   - `frontend/src/hooks/useChatWebSocket.ts`
   - `backend/src/main/java/com/bobbuy/api/AuthController.java`
   - `backend/src/main/java/com/bobbuy/security/AuthCookieService.java`
3. 阅读当前业务 smoke 相关页面：
   - `frontend/src/pages/LoginPage.tsx`
   - `frontend/src/pages/ClientHomeV2.tsx`
   - `frontend/src/pages/Orders.tsx`
   - `frontend/src/pages/Trips.tsx`
   - `frontend/src/pages/StockMaster.tsx`
   - `frontend/src/components/ChatWidget.tsx`
4. 阅读文档事实源：
   - `docs/reports/TEST-MATRIX-本地与CI执行矩阵.md`
   - `docs/reports/CURRENT-STATE-2026-04-28.md`
   - `docs/runbooks/RUNBOOK-试运行部署.md`
   - `docs/plans/PLAN-24-稳定上线差距收口优先级.md`

修复范围 A：E2E 环境与数据
1. 明确 E2E 启动方式：
   - 后端使用 test/dev profile、H2 或可控本地 PostgreSQL。
   - 前端使用生产 build preview 或 dev server，二选一并文档化。
   - seed 数据必须可重复，不依赖手工浏览器状态。
2. 每个 E2E 文件应独立登录，不依赖其他测试留下的 localStorage/cookie。
3. Cookie/CSRF：
   - 登录后验证 refresh cookie 为 HttpOnly，不在 localStorage 出现 refresh token。
   - refresh/logout 请求能携带 CSRF header。
   - 测试不得通过伪造旧 `X-BOBBUY-ROLE` 绕过真实登录，除非明确是本地兼容测试。
4. 增加统一 E2E helper：
   - 登录 agent/customer/merchant。
   - 清理 storage/cookie。
   - 等待 API 空闲或关键数据加载。
   - 捕获 console error 和 failed request，但允许已知失败路径白名单。

修复范围 B：核心 Smoke 场景
1. 认证与角色：
   - 登录成功进入对应首页。
   - `/api/auth/me` 驱动角色。
   - customer 不能访问 agent-only 页面。
   - header auth 关闭时伪造 `X-BOBBUY-ROLE` 不提权。
2. 订单/行程：
   - agent 可查看行程与订单。
   - customer 只能看到本人订单/账单。
   - 关键按钮和状态文案不依赖脆弱选择器。
3. 账单与履约：
   - 客户账单页可加载。
   - 冻结/只读状态至少覆盖一个 smoke 断言。
4. 聊天/WebSocket：
   - 有 token 时建立连接或优雅降级。
   - 无 token 不建连。
   - refresh 后后续连接使用新 access token。
   - 鉴权失败不无限重连。
5. 库存与 AI 上架：
   - 默认 smoke 覆盖“人工录入/草稿保存/失败后人工接管”路径。
   - 不依赖真实 OCR/LLM。
   - AI 真实视觉链路继续留在 `npm run e2e:ai` 或专用环境。

修复范围 C：稳定性与可维护性
1. 尽量使用可访问名称、role、test id 或稳定业务文本，不使用易碎 CSS 层级选择器。
2. 对网络等待使用 API 响应或页面状态，不使用固定长 sleep。
3. 对所有新增/修复 E2E，加上失败时截图、trace、video 或 Playwright 默认 trace 配置。
4. 如果某条真实链路暂不可测，必须登记原因、替代 smoke 和后续任务。
5. 现有 flaky 测试不要简单 skip；只有外部依赖不在默认环境时才能隔离到专用脚本。

修复范围 D：CI 与脚本
1. 明确脚本：
   - `npm run e2e`：默认 smoke，不依赖真实 AI/OCR/外部服务。
   - `npm run e2e:ai`：专用真实 AI/OCR 链路。
2. GitHub Actions：
   - 默认 PR CI 可继续不跑 Playwright，除非耗时和服务依赖已可控。
   - `workflow_dispatch` 手动 job 必须能跑 `npm run e2e`。
   - 文档写清需要的环境变量、服务启动方式和 artifact。
3. 如果 E2E 仍不能在当前沙箱跑通，必须给出具体阻塞和本地可复现命令；不要只写“环境原因”。

修复范围 E：文档同步
1. 更新：
   - `README.md`
   - `docs/reports/TEST-MATRIX-本地与CI执行矩阵.md`
   - `docs/reports/CURRENT-STATE-2026-04-28.md`
   - `docs/runbooks/RUNBOOK-试运行部署.md`
   - `docs/runbooks/RUNBOOK-监控告警与故障处置.md`
   - `docs/plans/PLAN-24-稳定上线差距收口优先级.md`
2. 明确：
   - 默认 E2E 覆盖范围。
   - AI/OCR 真实 E2E 的专用环境边界。
   - 失败 artifact 如何查看。
   - 哪些 E2E 是试运行上线前必须手动通过的门禁。

验收命令：
1. `cd backend && mvn test`
2. `cd frontend && npm test`
3. `cd frontend && npm run build`
4. `cd frontend && npm run e2e`
5. `cd /home/runner/work/bobbuy/bobbuy && docker compose config`
6. 如改 GitHub Actions：用 YAML 解析命令校验 workflow。
7. 如 `npm run e2e:ai` 不执行，必须说明专用环境缺口。

交付要求：
1. 提供 E2E 覆盖摘要：
   - 覆盖哪些角色
   - 覆盖哪些业务路径
   - 明确不覆盖哪些外部依赖
2. 提供修改清单和验证结果。
3. 登记仍未完成边界：
   - OAuth / SSO
   - mTLS / service mesh
   - 独立 schema / 数据所有权
   - 契约测试
   - 拆分后独立 CI/CD
   - 跨站前后端分离部署下的通用 cookie/CORS/CSRF 方案

禁止事项：
1. 不得通过固定 sleep 掩盖异步问题。
2. 不得用伪造角色 header 代替真实登录做默认 smoke。
3. 不得把真实 AI/OCR/外部 LLM 纳入默认 E2E。
4. 不得简单 skip 失败测试而不登记原因和替代覆盖。
5. 不得依赖上一个测试残留的 localStorage/cookie。
6. 不得回滚非本任务相关的用户改动。

建议实现顺序：
1. 先跑现有 `npm run e2e`，记录失败用例和失败原因。
2. 建立统一登录/storage/cookie helper。
3. 修复认证与角色门禁用例。
4. 修复聊天/WebSocket 用例。
5. 修复订单/行程/账单/库存 smoke。
6. 明确隔离 AI 真实链路。
7. 同步 CI 手动 job 和文档。
8. 跑完整验收命令。
```

---

## 3. 审核重点

请重点审核以下问题：

1. `npm run e2e` 是否应进入默认 PR CI，还是继续作为 `workflow_dispatch` 手动门禁。
2. E2E 使用生产 build preview 还是 Vite dev server。
3. 默认 smoke 是否覆盖 WebSocket，还是只验证鉴权失败不破坏 REST 主链路。
4. AI/OCR 真实链路是否继续保留在 `e2e:ai` 专用环境。
5. 是否需要为 E2E 准备独立 seed profile，避免污染本地 dev 数据。

---

## 4. 预期交付物

1. 稳定的 Playwright smoke 套件。
2. 统一 E2E 登录、清理、等待和 artifact helper。
3. 修复或隔离历史失败 E2E。
4. README、TEST-MATRIX、CURRENT STATE、Runbook、PLAN-24 同步。
5. 一份简短执行报告，包含 E2E 覆盖范围、验证结果和仍未完成边界。

---

## 5. 核准状态

**当前状态**: 待用户审查批准后执行。
