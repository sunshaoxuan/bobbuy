# PLAN-36: P2 Refresh Token 与会话生命周期治理开发提示词

**生效日期**: 2026-04-29
**状态**: 待评审
**关联计划**: [PLAN-24: 稳定上线差距收口优先级](PLAN-24-稳定上线差距收口优先级.md)
**前置任务**:
- [PLAN-28: P1 认证与权限生产化开发提示词](PLAN-28-P1认证与权限生产化开发提示词.md)
- [PLAN-34: P2 WebSocket 与服务间鉴权收口开发提示词](PLAN-34-P2-WebSocket与服务间鉴权收口开发提示词.md)
- [PLAN-35: P2 服务间鉴权与服务壳 Smoke 测试开发提示词](PLAN-35-P2-服务间鉴权与服务壳Smoke测试开发提示词.md)
**事实基线**: [CURRENT-STATE-2026-04-28](../reports/CURRENT-STATE-2026-04-28.md)

---

## 1. 开发目标

在 HTTP JWT、WebSocket 鉴权和最小服务间鉴权已经落地后，补齐用户会话生命周期治理。目标不是引入完整 OAuth/SSO，而是让当前 username/password 登录具备可控的 access token 刷新、登出撤销、过期恢复和前端重试边界，避免公网试运行时只能依赖短 TTL access token 与手动重新登录。

当前事实：

1. `/api/auth/login` 与 `/api/auth/me` 已使用 HMAC JWT access token。
2. `BOBBUY_SECURITY_JWT_TTL_SECONDS` 默认 3600 秒，过期后当前策略是重新登录。
3. WebSocket 通过 STOMP `CONNECT`/`SUBSCRIBE` 校验 access token。
4. service token 只用于 `/internal/**` 内部服务身份，不代表用户会话。
5. refresh token、会话撤销、复用检测、会话列表、OAuth/SSO 尚未实现。

---

## 2. 核心 Prompt 文本（请审查）

您可以直接将以下内容作为下一次开发任务的输入：

```text
### [任务: P2 Refresh Token 与会话生命周期治理]

任务背景：
BOBBuy 已完成生产化 HTTP JWT 登录、WebSocket STOMP Bearer 鉴权，以及试运行阶段最小服务间 service token。当前仍登记的认证边界是 refresh token 暂缓，access token 默认 3600 秒过期后需要重新登录。请补齐用户会话生命周期治理，支持 access token 安全刷新、登出撤销、刷新 token 轮换和前端 401 恢复。

目标：
1. 新增 refresh token 机制，使 access token 过期后可以通过受控刷新恢复会话。
2. refresh token 必须可撤销、可过期、可轮换，服务端不得明文保存 token。
3. 前端 API 层遇到 access token 过期时最多刷新并重试一次，不允许无限重试。
4. WebSocket 在 token 刷新后使用新的 access token 重连；刷新失败时停止重连并进入未登录态。
5. 明确本任务不引入 OAuth/SSO，不改变 service token 的语义，不把 service token 当成用户身份。

必须先做的排查：
1. 阅读后端认证实现：
   - `backend/src/main/java/com/bobbuy/api/AuthController.java`
   - `backend/src/main/java/com/bobbuy/security/JwtTokenService.java`
   - `backend/src/main/java/com/bobbuy/security/BearerTokenAuthenticationService.java`
   - `backend/src/main/java/com/bobbuy/security/TokenAuthenticationFilter.java`
   - `backend/src/main/java/com/bobbuy/security/InternalServiceTokenFilter.java`
   - `backend/src/main/java/com/bobbuy/config/SecurityConfig.java`
2. 阅读前端登录态与 API 实现：
   - `frontend/src/pages/LoginPage.tsx`
   - `frontend/src/stores/authStore.ts`
   - `frontend/src/lib/api.ts`
   - `frontend/src/hooks/useChatWebSocket.ts`
   - 相关测试文件
3. 阅读数据库迁移与配置：
   - `backend/src/main/resources/db/migration/**`
   - `backend/src/test/resources/application.properties`
   - `.env.template`
   - `docker-compose.yml`
   - `infra/nacos/config/*.yaml`
4. 阅读当前事实文档：
   - `README.md`
   - `docs/reports/CURRENT-STATE-2026-04-28.md`
   - `docs/reports/TEST-MATRIX-本地与CI执行矩阵.md`
   - `docs/runbooks/RUNBOOK-试运行部署.md`
   - `docs/plans/PLAN-24-稳定上线差距收口优先级.md`

修复范围 A：后端 refresh token 设计
1. 优先采用 opaque refresh token：
   - 登录时生成高熵随机 refresh token。
   - 服务端只保存 token hash，不保存明文。
   - 响应只返回明文 token 一次。
2. 增加持久化模型与迁移：
   - 记录用户、token hash、过期时间、撤销时间、创建时间、最近使用时间、轮换来源、客户端标识或 user agent 摘要。
   - 如使用 JPA entity，必须补 Flyway migration。
   - 测试 profile 继续保持 H2 可运行。
3. 增加配置项：
   - `BOBBUY_SECURITY_REFRESH_TOKEN_TTL_SECONDS`
   - 可选 `BOBBUY_SECURITY_REFRESH_TOKEN_ROTATION_ENABLED`
   - 默认值写入 `.env.template`，但不得包含真实 secret。
4. 新增或扩展接口：
   - `POST /api/auth/login` 返回 `accessToken`、`refreshToken`、access 过期时间、refresh 过期时间、当前用户。
   - `POST /api/auth/refresh` 校验 refresh token，轮换 refresh token，签发新的 access token。
   - `POST /api/auth/logout` 撤销当前 refresh token；没有 refresh token 时也应幂等成功或返回明确错误。
   - 可选 `GET /api/auth/sessions` / `DELETE /api/auth/sessions/{id}` 仅在范围可控时实现。
5. 安全边界：
   - refresh token 不得写日志。
   - refresh token 复用检测：已轮换/已撤销 token 再次使用时拒绝，并可撤销同链路会话。
   - refresh token 不能从 `X-BOBBUY-ROLE`、header auth 或 service token 推导用户身份。
   - service token 仍只用于 `/internal/**`，不得调用用户 refresh 流程获得用户 token。

修复范围 B：前端会话恢复
1. 登录成功后保存 access token、refresh token、当前用户与过期时间；如继续使用 localStorage，必须在文档中明确 XSS 风险和试运行取舍。
2. API 客户端遇到 401 时：
   - 如果不是 refresh/login 请求，尝试刷新一次。
   - 刷新成功后重试原请求一次。
   - 刷新失败后清理登录态并跳转登录页或进入未登录态。
   - 不允许多请求并发时同时发起多轮 refresh；应合并/排队同一轮刷新。
3. WebSocket 行为：
   - token 刷新后后续连接使用新 access token。
   - 鉴权失败或 refresh 失败后停止重连。
   - 不清空本地未发送消息，保留现有降级行为。
4. 登出行为：
   - 调用后端 logout 撤销 refresh token。
   - 无论后端是否成功，前端最终都要清理本地 token 和用户态。

修复范围 C：测试
1. 后端测试至少覆盖：
   - 登录返回 access token 与 refresh token。
   - refresh token 可换取新的 access token。
   - refresh token 轮换后旧 token 不可再次使用。
   - logout 后 refresh token 不可再使用。
   - 过期、伪造、缺失、已撤销 refresh token 均被拒绝。
   - customer/admin 角色刷新后仍保持原有权限边界。
   - service token 不能冒充用户刷新会话。
2. 前端测试至少覆盖：
   - access token 过期导致 401 时自动 refresh 并重试原请求一次。
   - refresh 失败时清理登录态。
   - 并发 401 只触发一轮 refresh。
   - logout 清理 access/refresh token。
   - WebSocket 使用刷新后的 access token，刷新失败停止重连。
3. 如新增 migration，补 Flyway 验证或在交付报告中说明本地未执行原因。

修复范围 D：文档同步
1. 更新：
   - `.env.template`
   - `README.md`
   - `docs/reports/CURRENT-STATE-2026-04-28.md`
   - `docs/reports/TEST-MATRIX-本地与CI执行矩阵.md`
   - `docs/runbooks/RUNBOOK-试运行部署.md`
   - `docs/runbooks/RUNBOOK-监控告警与故障处置.md`
   - `docs/plans/PLAN-24-稳定上线差距收口优先级.md`
2. 明确：
   - access token TTL 与 refresh token TTL。
   - refresh token 保存、轮换、撤销策略。
   - 前端 localStorage / cookie / memory 的实际选择和风险。
   - OAuth/SSO 仍未实现。
   - service token 与用户 token 的边界。

验收命令：
1. `cd backend && mvn test`
2. `cd frontend && npm test`
3. `cd frontend && npm run build`
4. `docker compose config`
5. 如新增 migration：执行 PostgreSQL Flyway migrate/validate，或说明当前环境无法执行的原因。
6. 如改 WebSocket：补定向前端/后端 WS 鉴权测试并记录命令。

交付要求：
1. 提供 refresh token 方案摘要：
   - token 类型
   - 服务端存储方式
   - TTL 配置
   - 轮换与撤销策略
   - 与 access token、service token 的关系
2. 提供修改清单和验证结果。
3. 登记仍未完成边界：
   - OAuth / SSO
   - mTLS / service mesh
   - 独立 schema / 数据所有权
   - 契约测试
   - 拆分后独立 CI/CD
   - 更强的浏览器端 token 防护策略，如 HttpOnly SameSite cookie

禁止事项：
1. 不得服务端明文保存 refresh token。
2. 不得把 refresh token 写入日志、错误响应、trace 或 metrics label。
3. 不得在 refresh 失败后无限重试。
4. 不得让 service token 换取用户 access token。
5. 不得让 header auth 或 `X-BOBBUY-ROLE` 参与 refresh 用户身份判定。
6. 不得在文档中写入真实 secret。
7. 不得回滚非本任务相关的用户改动。

建议实现顺序：
1. 先明确 refresh token 数据模型、TTL 与轮换策略。
2. 增加 migration、entity/repository/service。
3. 扩展 auth API 与后端测试。
4. 接入前端 API refresh 队列与 logout。
5. 接入 WebSocket token 更新行为。
6. 同步配置与文档。
7. 跑默认门禁、migration 验证和定向鉴权测试。
```

---

## 3. 审核重点

请重点审核以下问题：

1. refresh token 是否采用 opaque token + 服务端 hash 存储，而不是长期 JWT refresh token。
2. 是否本轮实现会话列表和远程踢出，还是只实现 login/refresh/logout 最小闭环。
3. 前端 refresh token 暂存位置：继续 localStorage 以控制改动面，还是切换 HttpOnly SameSite cookie。
4. refresh token 轮换后旧 token 复用时，是只拒绝当前请求，还是撤销同一 token family。
5. 是否把 refresh token migration 纳入本轮 PostgreSQL Flyway 验证。

---

## 4. 预期交付物

1. refresh token 后端模型、迁移、接口与测试。
2. 前端 401 refresh/retry、logout 清理、WebSocket token 更新与测试。
3. `.env.template`、README、CURRENT STATE、TEST MATRIX、Runbook、PLAN-24 同步。
4. 一份简短执行报告，包含验证结果和仍未完成认证边界。

---

## 5. 核准状态

**当前状态**: 待用户审查批准后执行。
