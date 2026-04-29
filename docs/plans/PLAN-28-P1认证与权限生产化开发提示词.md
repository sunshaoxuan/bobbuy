# PLAN-28: P1 认证与权限生产化开发提示词

**生效日期**: 2026-04-29
**状态**: 待评审
**关联计划**: [PLAN-24: 稳定上线差距收口优先级](PLAN-24-稳定上线差距收口优先级.md)
**前置任务**:
- [PLAN-25: P0 后端测试基线恢复开发提示词](PLAN-25-P0后端测试基线恢复开发提示词.md)
- [PLAN-26: P0 前端测试基线恢复开发提示词](PLAN-26-P0前端测试基线恢复开发提示词.md)
- [PLAN-27: P0 上线验收矩阵与 CI 固化开发提示词](PLAN-27-P0上线验收矩阵与CI固化开发提示词.md)
**事实基线**: [CURRENT-STATE-2026-04-28](../reports/CURRENT-STATE-2026-04-28.md)

---

## 1. 开发目标

把当前试运行级的 header 角色注入，升级为可逐步上线的真实认证与权限基础。

当前事实：

1. 后端 `RoleInjectionFilter` 直接信任 `X-BOBBUY-ROLE` / `X-BOBBUY-USER`。
2. 前端 `UserRoleContext` 支持 query/localStorage 切换角色。
3. 这种方式适合本地开发和受控内网演示，不适合公网、服务器部署或真实客户使用。

本任务目标不是一次性接入完整 OAuth/SSO，而是建立 BOBBuy 自有的最小生产级登录、token、角色绑定和兼容迁移路径。

---

## 2. 核心 Prompt 文本（请审查）

您可以直接将以下内容作为下一次开发任务的输入：

```text
### [任务: P1 认证与权限生产化]

任务背景：
BOBBuy 的 P0 测试与 CI 门禁已经恢复，下一阶段最高优先级是消除试运行级 header 角色注入风险。当前后端通过 `RoleInjectionFilter` 信任 `X-BOBBUY-ROLE` / `X-BOBBUY-USER`，前端通过 query/localStorage 控制角色。这不适合公网或非可信网络。请在保持现有测试稳定的前提下，落地最小生产级认证方案，并保留可控的本地开发兼容模式。

目标：
1. 增加真实登录 API，返回服务端签发的访问令牌。
2. 后端默认通过 token 识别用户身份与角色，不再在生产模式信任任意客户端 header。
3. 保留本地/测试 profile 的 header 注入兼容模式，但必须通过显式配置开启。
4. 前端从“手动切角色”迁移到“登录态驱动角色”，本地演示入口可保留但不得伪装成生产认证。
5. 增加权限与越权访问自动化测试，尤其是客户只能访问本人订单/账单。

必须先做的排查：
1. 阅读后端现有认证与授权代码：
   - `backend/src/main/java/com/bobbuy/SecurityConfig.java`
   - `backend/src/main/java/com/bobbuy/security/RoleInjectionFilter.java`
   - `backend/src/main/java/com/bobbuy/security/CustomerIdentityResolver.java`
   - `backend/src/test/java/com/bobbuy/api/SecurityAuthorizationIntegrationTest.java`
2. 阅读用户与角色模型：
   - `backend/src/main/java/com/bobbuy/model/User.java`
   - `backend/src/main/java/com/bobbuy/model/Role.java`
   - `backend/src/main/java/com/bobbuy/repository/UserRepository.java`
3. 阅读前端角色上下文与 API 封装：
   - `frontend/src/context/UserRoleContext.tsx`
   - `frontend/src/components/ProtectedRoute.tsx`
   - `frontend/src/api.ts`
4. 确认当前门禁命令仍可通过：
   - `cd backend && mvn test`
   - `cd frontend && npm test`
   - `cd frontend && npm run build`

修复范围 A：后端认证模型
1. 选择最小实现路线：
   - 推荐：自有 username/password 登录 + HMAC JWT access token。
   - 暂不做：第三方 OAuth、社交登录、复杂刷新令牌、多租户 SSO。
2. 为用户增加登录所需字段，或建立独立凭据模型：
   - username / loginId / email
   - passwordHash
   - role
   - enabled / disabled
3. 密码必须哈希存储：
   - 使用 Spring Security `PasswordEncoder`。
   - 禁止明文密码落库。
4. seed/test 数据可初始化固定测试账号：
   - agent 测试账号。
   - customer 测试账号。
   - merchant 如当前业务需要。
5. 如当前数据库迁移尚未引入 Flyway/Liquibase，可以先通过 JPA 字段兼容试运行，但必须在文档登记后续 migration 要求。

修复范围 B：登录 API 与 token 校验
1. 增加登录接口，例如：
   - `POST /api/auth/login`
   - request: `{ "username": "...", "password": "..." }`
   - response: `{ "accessToken": "...", "user": { "id": ..., "name": "...", "role": "..." } }`
2. 增加当前用户接口，例如：
   - `GET /api/auth/me`
3. 增加 token 校验 filter：
   - 读取 `Authorization: Bearer <token>`。
   - 校验签名、过期时间、用户状态。
   - 写入 `SecurityContext`，principal 应能被 `CustomerIdentityResolver` 正确解析。
4. token secret 必须来自配置：
   - `bobbuy.security.jwt.secret`
   - 测试 profile 可用固定 secret。
   - 生产缺失 secret 时应失败启动或明确拒绝 token 模式。
5. token 过期时间通过配置控制。

修复范围 C：header 注入兼容模式
1. 将 `RoleInjectionFilter` 改为显式配置启用：
   - 例如 `bobbuy.security.header-auth.enabled=true`。
2. 默认生产配置必须关闭 header auth。
3. test/dev profile 可以开启 header auth，便于既有测试和本地演示过渡。
4. 当 token auth 和 header auth 同时存在时，优先级必须明确：
   - 推荐：有效 Bearer token 优先。
   - header auth 仅在没有 Bearer token 且显式启用时生效。
5. 文档必须写明：公网部署不得开启 header auth。

修复范围 D：授权与越权测试
1. 后端新增/更新集成测试：
   - 未登录访问受保护 API 返回 401。
   - agent 登录后可访问采购、用户、审计、商品管理相关 API。
   - customer 登录后不能访问 agent-only API，返回 403。
   - customer 只能访问本人订单。
   - customer 只能访问本人账单/ledger。
   - 使用伪造 `X-BOBBUY-ROLE: AGENT` 在生产模式不应提权。
2. 保持现有 `SecurityAuthorizationIntegrationTest` 语义，不要用放宽权限换测试通过。
3. 明确 WebSocket `/ws` 当前是否仍 permitAll：
   - 如继续 permitAll，必须登记为后续聊天鉴权任务。
   - 如本次接入鉴权，必须补兼容测试。

修复范围 E：前端登录态
1. 增加最小登录页或登录组件：
   - 用户名 / 密码。
   - 登录失败提示。
   - 登录成功后保存 access token，并加载 `/api/auth/me`。
2. API 封装统一附加 `Authorization: Bearer <token>`。
3. `UserRoleContext` 不应在生产路径继续从 query/localStorage 任意切角色：
   - 可以保留 `bobbuy_test_role` 作为测试专用。
   - 可以保留 dev-only 快速切换，但必须显式标注并避免进入生产默认入口。
4. `ProtectedRoute` 基于真实登录用户角色判断。
5. 增加前端基础测试：
   - 未登录访问受保护页面跳转登录或显示未授权状态。
   - 登录成功后进入对应角色首页。
   - customer 不能进入 agent-only 页面。
   - API 请求附带 Bearer token。

修复范围 F：配置与文档
1. 更新 `.env.template` / application properties / Docker/Nacos 相关配置：
   - JWT secret。
   - token TTL。
   - header auth enable 开关。
2. 更新 README / CURRENT STATE / TEST-MATRIX 或新增 auth 文档：
   - 当前认证方案。
   - 本地开发如何开启 header auth。
   - 服务器部署如何配置 JWT secret。
   - 禁止公网开启 header auth。
3. 如暂不引入 DB migration，必须登记为 PLAN-24 后续数据库迁移治理的前置风险。

验收命令：
1. `cd backend && mvn test`
2. `cd frontend && npm test`
3. `cd frontend && npm run build`
4. 如修改 Docker 配置：
   - `cd backend && mvn -DskipTests package`
   - `docker build backend -t bobbuy-backend-test`
   - `docker build frontend -t bobbuy-frontend-test`

交付要求：
1. 提供认证方案摘要：
   - token 类型。
   - secret 配置。
   - header auth 兼容策略。
   - 前端登录态策略。
2. 提供修改清单：
   - 后端安全配置、filter、controller、model/repository。
   - 前端登录、API、路由/上下文。
   - 测试与文档。
3. 提供验证结果。
4. 明确未完成但已登记的安全边界：
   - refresh token 是否暂缓。
   - WebSocket 是否已鉴权。
   - OAuth/SSO 是否暂缓。
   - DB migration 是否后续处理。

禁止事项：
1. 不得继续让生产默认信任 `X-BOBBUY-ROLE` / `X-BOBBUY-USER`。
2. 不得明文存储密码。
3. 不得把 JWT secret 写死到生产代码。
4. 不得为了快速通过测试而放宽 agent/customer 权限矩阵。
5. 不得破坏 P0 已恢复的 backend/frontend 测试门禁。
6. 不得回滚非本任务相关的用户改动。

建议实现顺序：
1. 先增加后端 token 登录与 me 接口。
2. 增加 Bearer token filter，并把 header auth 改为显式兼容开关。
3. 更新后端授权测试，覆盖 401/403/本人数据隔离。
4. 再改前端 API token 注入、登录页、角色上下文。
5. 补前端登录/权限测试。
6. 更新配置模板和文档。
7. 跑完整默认门禁。
```

---

## 3. 审核重点

请重点审核以下问题：

1. 是否接受“自有 username/password + HMAC JWT”作为第一阶段认证方案。
2. 是否要求本任务同时实现 refresh token，还是先只做短期 access token。
3. 是否要求 WebSocket 本轮同步鉴权，还是登记为后续任务。
4. 是否允许在 dev/test profile 保留 header auth 兼容开关。
5. 是否接受暂不引入 Flyway/Liquibase，把凭据字段迁移治理放到下一项 P1 数据库迁移任务。

---

## 4. 预期交付物

1. 后端登录 API、当前用户 API、token filter、权限测试。
2. header auth 显式开关与生产默认关闭。
3. 前端登录态、API token 注入、路由权限测试。
4. 配置模板与文档更新。
5. 一份简短执行报告，包含认证边界、兼容策略、测试结果和后续风险登记。

---

## 5. 核准状态

**当前状态**: 待用户审查批准后执行。
