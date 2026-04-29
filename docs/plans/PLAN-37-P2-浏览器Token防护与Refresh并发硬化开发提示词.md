# PLAN-37: P2 浏览器 Token 防护与 Refresh 并发硬化开发提示词

**生效日期**: 2026-04-29
**状态**: 待评审
**关联计划**: [PLAN-24: 稳定上线差距收口优先级](PLAN-24-稳定上线差距收口优先级.md)
**前置任务**:
- [PLAN-28: P1 认证与权限生产化开发提示词](PLAN-28-P1认证与权限生产化开发提示词.md)
- [PLAN-34: P2 WebSocket 与服务间鉴权收口开发提示词](PLAN-34-P2-WebSocket与服务间鉴权收口开发提示词.md)
- [PLAN-35: P2 服务间鉴权与服务壳 Smoke 测试开发提示词](PLAN-35-P2-服务间鉴权与服务壳Smoke测试开发提示词.md)
- [PLAN-36: P2 Refresh Token 与会话生命周期治理开发提示词](PLAN-36-P2-RefreshToken与会话生命周期治理开发提示词.md)
**事实基线**: [CURRENT-STATE-2026-04-28](../reports/CURRENT-STATE-2026-04-28.md)

---

## 1. 开发目标

在 refresh token 已经落地后，继续收口浏览器端 token 暴露面和 refresh 轮换并发边界。目标不是引入 OAuth/SSO，而是把当前 localStorage 保存 refresh token 的试运行取舍升级为更适合公网试运行的 HttpOnly SameSite cookie 方案，并让同一个 refresh token 在并发刷新时只能成功轮换一次。

当前事实：

1. access token 是 HMAC JWT，默认 TTL 3600 秒。
2. refresh token 是 opaque token，服务端只存 SHA-256 hash，默认 TTL 604800 秒。
3. refresh 时轮换 token，旧 token 复用会撤销同 family 活跃会话。
4. 前端已实现 401 单轮 refresh + retry、并发 refresh 合并、logout 撤销、WebSocket refresh 后重连。
5. 当前 refresh token 仍存放在 localStorage；这对 XSS 暴露面不够理想。
6. 当前后端 refresh 轮换流程未明确使用行级锁、乐观锁或条件更新；极端并发刷新同一个旧 token 时需要进一步证明或修复单次轮换语义。

---

## 2. 核心 Prompt 文本（请审查）

您可以直接将以下内容作为下一次开发任务的输入：

```text
### [任务: P2 浏览器 Token 防护与 Refresh 并发硬化]

任务背景：
BOBBuy 已完成 JWT access token、opaque refresh token、refresh 轮换、logout 撤销、WebSocket token 更新和服务间 service token。当前剩余的认证风险集中在两点：浏览器端 refresh token 仍存 localStorage，XSS 下容易被读取；后端 refresh 轮换流程需要对同一旧 refresh token 的并发请求建立“只成功一次”的强约束。请在不引入 OAuth/SSO 的前提下完成这轮安全硬化。

目标：
1. 将 refresh token 从 localStorage 迁移到 HttpOnly、Secure、SameSite cookie，或给出不能迁移的明确技术原因并完成等价风险降低。
2. access token 可以继续由前端内存/localStorage 管理，但 refresh token 不应被 JavaScript 直接读取。
3. refresh/logout 接口改为从 cookie 读取 refresh token，并补 CSRF 防护策略。
4. 同一个 refresh token 在并发 refresh 场景下只能成功轮换一次；其他并发请求必须失败并触发复用检测或明确的 401。
5. 保持现有登录、刷新、登出、WebSocket 重连和前端 401 retry 行为可用。

必须先做的排查：
1. 阅读后端认证与 refresh 实现：
   - `backend/src/main/java/com/bobbuy/api/AuthController.java`
   - `backend/src/main/java/com/bobbuy/service/AuthService.java`
   - `backend/src/main/java/com/bobbuy/service/RefreshTokenService.java`
   - `backend/src/main/java/com/bobbuy/model/RefreshTokenSession.java`
   - `backend/src/main/java/com/bobbuy/repository/RefreshTokenSessionRepository.java`
   - `backend/src/main/java/com/bobbuy/security/**`
   - `backend/src/main/resources/db/migration/V2__refresh_token_sessions.sql`
2. 阅读前端 auth 与 WebSocket：
   - `frontend/src/authStorage.ts`
   - `frontend/src/api.ts`
   - `frontend/src/context/UserRoleContext.tsx`
   - `frontend/src/hooks/useChatWebSocket.ts`
   - 相关测试文件
3. 阅读网关与部署配置：
   - `infra/nginx/prod.conf`
   - `docker-compose.yml`
   - `.env.template`
   - `infra/nacos/config/*.yaml`
4. 阅读当前文档：
   - `README.md`
   - `docs/reports/CURRENT-STATE-2026-04-28.md`
   - `docs/reports/TEST-MATRIX-本地与CI执行矩阵.md`
   - `docs/runbooks/RUNBOOK-试运行部署.md`
   - `docs/runbooks/RUNBOOK-监控告警与故障处置.md`
   - `docs/plans/PLAN-24-稳定上线差距收口优先级.md`

修复范围 A：Refresh Cookie 与 CSRF
1. 设计 cookie 策略：
   - cookie 名称建议：`bobbuy_refresh_token`
   - HttpOnly: true
   - Secure: 生产 true；本地开发可配置 false
   - SameSite: Lax 或 Strict，需说明选择理由
   - Path 限定为 `/api/auth`
   - Max-Age 与 `BOBBUY_SECURITY_REFRESH_TOKEN_TTL_SECONDS` 对齐
2. 登录：
   - `/api/auth/login` 设置 refresh cookie。
   - 响应体不再返回 refresh token 明文，或仅在明确 dev/test 兼容开关下返回。
3. 刷新：
   - `/api/auth/refresh` 默认从 HttpOnly cookie 读取 refresh token。
   - 刷新成功后设置新的 refresh cookie。
   - 刷新失败后清理 refresh cookie。
4. 登出：
   - `/api/auth/logout` 从 cookie 读取并撤销 refresh token。
   - 无论撤销是否成功，都清理 refresh cookie。
5. CSRF：
   - 因 refresh/logout 依赖 cookie，必须补 CSRF 防护。
   - 可采用 double-submit CSRF token：后端设置非 HttpOnly `bobbuy_csrf_token`，前端在 mutating request 中带 `X-BOBBUY-CSRF-TOKEN`。
   - 至少保护 `/api/auth/refresh`、`/api/auth/logout`，并评估是否保护全部非 GET API。
   - CORS、SameSite、Nginx 代理头配置必须与方案一致。

修复范围 B：前端存储与 API 调整
1. `authStorage.ts` 不再保存 refresh token 明文。
2. `api.ts` 的 refresh 请求不再从 localStorage 读取 refresh token，而是 `credentials: 'same-origin'` 或等价方式携带 cookie。
3. 登录、refresh、logout、me 流程保持现有调用方接口尽量稳定。
4. 前端 401 refresh 合并队列保留：
   - 并发 401 仍只触发一轮 refresh。
   - refresh 成功后重试原请求一次。
   - refresh 失败后清理 access token 和用户态。
5. WebSocket：
   - WebSocket 仍使用 access token。
   - refresh 成功后新连接使用新的 access token。
   - refresh 失败时停止重连并进入未登录态。

修复范围 C：后端 Refresh 并发硬化
1. 对 refresh token 读取和轮换建立并发保护，任选其一并说明理由：
   - JPA pessimistic write lock：`findByTokenHashForUpdate`
   - optimistic lock：`@Version`
   - 条件更新：`update ... where revoked_at is null`
2. 目标语义：
   - 同一旧 refresh token 同时发起多次 refresh，只能有一次成功。
   - 失败请求不得再签发新的 access token 或 refresh token。
   - 旧 token 复用检测应撤销同 family 活跃会话，或明确记录更保守策略。
3. 补并发测试：
   - 同一个 refresh token 并发刷新，多线程/多请求下成功数必须为 1。
   - 其他请求返回 401 或明确错误。
   - 成功后的新 refresh token 可以继续刷新。

修复范围 D：测试
1. 后端测试至少覆盖：
   - login 设置 HttpOnly refresh cookie。
   - refresh 从 cookie 读取 token 并轮换 cookie。
   - logout 清理 cookie 并撤销服务端 session。
   - 缺失/伪造 CSRF token 被拒绝。
   - 同一 refresh token 并发刷新只成功一次。
   - service token 不能参与用户 refresh。
2. 前端测试至少覆盖：
   - 不再向 localStorage 写入 refresh token。
   - refresh 请求携带 cookie/credentials，并携带 CSRF header。
   - 401 refresh+retry 仍然只重试一次。
   - 并发 401 仍合并为一轮 refresh。
   - logout 不需要读取 refresh token 明文。
   - WebSocket 使用刷新后的 access token。
3. 如引入 cookie/CSRF 配置，补 Nginx/Compose 配置渲染验证。

修复范围 E：文档同步
1. 更新：
   - `.env.template`
   - `README.md`
   - `docs/reports/CURRENT-STATE-2026-04-28.md`
   - `docs/reports/TEST-MATRIX-本地与CI执行矩阵.md`
   - `docs/runbooks/RUNBOOK-试运行部署.md`
   - `docs/runbooks/RUNBOOK-监控告警与故障处置.md`
   - `docs/plans/PLAN-24-稳定上线差距收口优先级.md`
2. 明确：
   - refresh cookie 名称、Path、HttpOnly、Secure、SameSite。
   - CSRF 策略和前端 header。
   - localStorage 中还保留什么，不再保留什么。
   - 本轮仍未实现 OAuth/SSO。
   - 本轮仍未实现 mTLS/service mesh。

验收命令：
1. `cd backend && mvn test`
2. `cd frontend && npm test`
3. `cd frontend && npm run build`
4. `docker compose config`
5. 如涉及 migration：执行 PostgreSQL Flyway migrate/validate，或说明当前环境无法执行的原因。
6. 如涉及 Nginx cookie/CORS/CSRF：补配置渲染或定向测试命令。

交付要求：
1. 提供 cookie/CSRF 方案摘要：
   - cookie 属性
   - CSRF token 生成、存储、校验方式
   - dev/test/prod 差异
2. 提供 refresh 并发硬化摘要：
   - 使用锁、乐观锁还是条件更新
   - 并发测试结果
3. 提供修改清单和验证结果。
4. 登记仍未完成边界：
   - OAuth / SSO
   - mTLS / service mesh
   - 独立 schema / 数据所有权
   - 契约测试
   - 拆分后独立 CI/CD

禁止事项：
1. 不得继续在生产路径把 refresh token 明文存入 localStorage。
2. 不得在日志、错误响应、trace、metrics label 中输出 refresh token 或 CSRF token。
3. 不得引入 cookie refresh 却没有 CSRF 防护。
4. 不得让同一个旧 refresh token 在并发请求下签发多个有效新 token。
5. 不得让 service token 换取用户 access token。
6. 不得写入真实 secret。
7. 不得回滚非本任务相关的用户改动。

建议实现顺序：
1. 先写后端并发刷新失败测试，复现或约束单次轮换语义。
2. 实现 refresh token 行级锁/乐观锁/条件更新。
3. 增加 refresh cookie 设置、读取和清理。
4. 增加 CSRF token 生成与校验。
5. 调整前端 authStorage/api/logout/WebSocket 流程。
6. 同步配置与文档。
7. 跑默认门禁和定向认证测试。
```

---

## 3. 审核重点

请重点审核以下问题：

1. refresh token 是否必须本轮迁移到 HttpOnly cookie，还是继续 localStorage 但缩短 TTL 并强化 CSP。
2. SameSite 选择 Lax 还是 Strict；是否影响未来跨域前后端部署。
3. CSRF 防护是仅覆盖 auth refresh/logout，还是扩展到所有非 GET API。
4. refresh 并发硬化采用 pessimistic lock、optimistic lock 还是条件更新。
5. 是否需要引入 session 列表和远程撤销 UI，或继续保持后端能力先行。

---

## 4. 预期交付物

1. HttpOnly refresh cookie 与 CSRF 防护实现。
2. refresh token 并发轮换强约束与测试。
3. 前端不再保存 refresh token 明文，401 refresh/retry 和 WebSocket 行为保持稳定。
4. `.env.template`、README、CURRENT STATE、TEST MATRIX、Runbook、PLAN-24 同步。
5. 一份简短执行报告，包含验证结果和仍未完成认证边界。

---

## 5. 核准状态

**当前状态**: 待用户审查批准后执行。
