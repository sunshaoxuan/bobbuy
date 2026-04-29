# PLAN-35: P2 服务间鉴权与服务壳 Smoke 测试开发提示词

**生效日期**: 2026-04-29
**状态**: 待评审
**关联计划**: [PLAN-24: 稳定上线差距收口优先级](PLAN-24-稳定上线差距收口优先级.md)
**前置任务**:
- [PLAN-32: P2 微服务边界决策开发提示词](PLAN-32-P2-微服务边界决策开发提示词.md)
- [PLAN-33: P2 生产运维基础开发提示词](PLAN-33-P2-生产运维基础开发提示词.md)
- [PLAN-34: P2 WebSocket 与服务间鉴权收口开发提示词](PLAN-34-P2-WebSocket与服务间鉴权收口开发提示词.md)
**事实基线**: [CURRENT-STATE-2026-04-28](../reports/CURRENT-STATE-2026-04-28.md)

---

## 1. 开发目标

在已经完成 HTTP JWT、WebSocket STOMP Bearer 鉴权和服务边界 ADR 的基础上，补齐试运行阶段的最小服务间鉴权与服务壳启动 smoke test。目标不是立即完成真实微服务拆分，而是让 `core-service`、`ai-service`、`im-service`、`auth-service`、`gateway-service` 在当前“服务外壳”定位下具备可验证的内部调用边界与启动健康门禁。

当前事实：

1. `backend` 是源码和默认测试主入口，`core-service` 是试运行核心业务事实源。
2. `ai-service`、`im-service`、`auth-service` 仍复用共享代码和共享 PostgreSQL schema，不是独立业务事实源。
3. WebSocket `/ws` 已通过 STOMP `CONNECT`/`SUBSCRIBE` 校验 access token。
4. 独立服务间 service token / mTLS 尚未实现，当前仍依赖 Docker 内网边界与共享 JWT 配置。
5. 服务壳缺少独立启动 smoke test；这会阻碍后续把服务外壳降级为 optional/profile 或继续真实拆分。

---

## 2. 核心 Prompt 文本（请审查）

您可以直接将以下内容作为下一次开发任务的输入：

```text
### [任务: P2 服务间鉴权与服务壳 Smoke 测试]

任务背景：
BOBBuy 已完成 HTTP JWT、WebSocket 鉴权、部署配置、服务边界 ADR 和最小运维基础。当前仍登记的安全/架构缺口是：服务间调用尚未有独立 service token / mTLS，服务外壳缺少启动 smoke test。请补齐试运行阶段最小服务间鉴权边界，并增加服务壳启动健康验证，避免继续依赖“内网可信”作为唯一保护。

目标：
1. 引入最小 service token 机制或明确选择更合适的短期服务间鉴权方案。
2. gateway-service 转发到内部服务时可以附带可信内部身份；后端服务只在明确配置开启时信任该内部身份。
3. 服务间鉴权默认安全：未配置 secret 时不得开放公网信任，不得回退到可伪造 header。
4. 为 `core-service`、`ai-service`、`im-service`、`auth-service`、`gateway-service` 增加最小启动 smoke test / 配置渲染验证。
5. 文档继续明确：本任务不等于完成独立 schema、mTLS 或真实微服务拆分。

必须先做的排查：
1. 阅读安全实现：
   - `backend/src/main/java/com/bobbuy/security/**`
   - `backend/src/main/java/com/bobbuy/config/SecurityConfig.java`
   - `backend/src/main/java/com/bobbuy/security/TokenAuthenticationFilter.java`
   - `backend/src/main/java/com/bobbuy/security/BearerTokenAuthenticationService.java`
2. 阅读 gateway 与服务壳：
   - `bobbuy-gateway/**`
   - `bobbuy-core/**`
   - `bobbuy-ai/**`
   - `bobbuy-im/**`
   - `bobbuy-auth/**`
   - `Dockerfile.service`
   - `docker-compose.yml`
   - `infra/nacos/config/*.yaml`
3. 阅读当前边界文档：
   - `docs/architecture/ADR-01-试运行阶段服务边界决策.md`
   - `docs/reports/CURRENT-STATE-2026-04-28.md`
   - `docs/runbooks/RUNBOOK-试运行部署.md`
   - `docs/runbooks/RUNBOOK-监控告警与故障处置.md`
   - `docs/reports/TEST-MATRIX-本地与CI执行矩阵.md`

修复范围 A：服务间鉴权方案
1. 设计短期最小机制，建议优先考虑：
   - `BOBBUY_SECURITY_SERVICE_TOKEN`：服务间共享 token。
   - `X-BOBBUY-SERVICE-TOKEN`：内部服务 token header。
   - `X-BOBBUY-INTERNAL-SERVICE`：可选服务名，仅在 service token 校验通过后使用。
2. 默认值必须安全：
   - `.env.template` 不提供生产可用 token。
   - token 为空时不启用内部信任。
   - 不得把普通外部请求中的 `X-BOBBUY-INTERNAL-*` header 直接信任为身份。
3. 鉴权失败返回 401/403，并且日志不得记录完整 service token。
4. 明确 service token 与用户 JWT 的关系：
   - service token 只证明调用方是内部服务，不代表最终用户身份。
   - 如需要用户上下文，仍应携带并校验用户 JWT 或明确记录为 system 操作。
5. 如最终判断本阶段不适合实现代码，必须给出技术原因，并至少补 smoke test 与风险文档；不要把未实现写成已完成。

修复范围 B：gateway / 服务壳接入
1. 如果实现 service token：
   - gateway-service 对内部下游请求附加 service token。
   - 下游服务识别并校验 service token。
   - 不允许浏览器直接设置内部 token header 形成提权。
2. 确认 Nginx gateway 不会把外部伪造的内部 header 直接透传为可信身份；如需要，在 `infra/nginx/prod.conf` 清理/覆盖敏感内部 header。
3. 文档写清当前 service token 只适合试运行内网，不替代 mTLS 或 service mesh。

修复范围 C：服务壳 Smoke 测试
1. 增加最小 smoke test 方案，覆盖：
   - `core-service`
   - `ai-service`
   - `im-service`
   - `auth-service`
   - `gateway-service`
2. 可选实现方式：
   - Maven profile / Spring Boot smoke context test。
   - Docker image 启动后 readiness 检查。
   - CI workflow_dispatch 手动 job。
   - 文档化脚本命令。
3. 不要求默认 PR CI 启动完整 Compose，但必须给出可执行 smoke 命令和边界。
4. 如果只做配置渲染验证，必须明确它不能替代真实启动 smoke。

修复范围 D：测试
1. 后端测试至少覆盖：
   - 未配置 service token 时，内部 header 不生效。
   - service token 缺失/错误时拒绝。
   - service token 正确时只获得内部服务身份，不直接冒充用户。
   - 外部伪造 `X-BOBBUY-INTERNAL-*` 不得提权。
   - header auth 关闭时仍不能借旧 header 提权。
2. gateway / 配置测试至少覆盖：
   - Nginx / gateway 清理或覆盖内部 header。
   - Compose / Nacos service token 配置项渲染正确。
3. 如新增手动 smoke job，更新测试矩阵。

修复范围 E：文档同步
1. 更新：
   - `.env.template`
   - `README.md`
   - `docs/reports/CURRENT-STATE-2026-04-28.md`
   - `docs/runbooks/RUNBOOK-试运行部署.md`
   - `docs/runbooks/RUNBOOK-监控告警与故障处置.md`
   - `docs/reports/TEST-MATRIX-本地与CI执行矩阵.md`
   - `docs/plans/PLAN-24-稳定上线差距收口优先级.md`
2. 明确：
   - service token 是否已实现。
   - 它保护什么，不保护什么。
   - mTLS / service mesh 是否仍未完成。
   - 服务壳 smoke test 如何执行。

验收命令：
1. `cd backend && mvn test`
2. `cd frontend && npm test`
3. `cd frontend && npm run build`
4. `docker compose config`
5. 如新增服务壳 smoke test：执行对应 smoke 命令并记录结果。
6. 如改 Docker / gateway / Nacos：补充 image build 或配置渲染验证。

交付要求：
1. 提供服务间鉴权摘要：
   - 采用的机制
   - 配置项
   - 默认安全行为
   - 与用户 JWT 的关系
2. 提供服务壳 smoke test 摘要：
   - 覆盖服务
   - 执行方式
   - 是否进入默认 CI
3. 提供修改清单和验证结果。
4. 登记仍未完成边界：
   - mTLS / service mesh
   - 独立 schema / 数据所有权
   - 契约测试
   - 拆分后独立 CI/CD
   - refresh token / OAuth

禁止事项：
1. 不得提供生产可用的硬编码 service token。
2. 不得在 token 未配置时信任内部 header。
3. 不得让外部请求伪造内部服务身份。
4. 不得把 service token 直接等同于用户身份。
5. 不得宣称已完成 mTLS、service mesh 或真实微服务拆分。
6. 不得回滚非本任务相关的用户改动。

建议实现顺序：
1. 先确认当前 gateway / nginx 是否会透传内部 header。
2. 设计最小 service token 配置与过滤器。
3. 补后端安全测试。
4. 补 gateway/nginx header 清理或配置说明。
5. 补服务壳 smoke test 或手动 smoke job。
6. 同步 README、CURRENT STATE、Runbook、TEST MATRIX、PLAN-24。
7. 跑默认门禁和 smoke 验证。
```

---

## 3. 审核重点

请重点审核以下问题：

1. 是否现在实现 `BOBBUY_SECURITY_SERVICE_TOKEN`，还是只先补服务壳 smoke test。
2. gateway-service 和 nginx gateway 哪一层负责清理/覆盖内部 header。
3. service token 是否允许携带用户上下文，还是只表达系统调用身份。
4. 服务壳 smoke test 是否进入默认 CI，还是先作为手动 workflow_dispatch。
5. 本任务是否继续暂缓 mTLS / service mesh，避免范围过大。

---

## 4. 预期交付物

1. 最小服务间鉴权实现或明确暂缓决策。
2. 服务壳 smoke test / 手动验证 job / 可执行命令。
3. 安全测试与配置验证。
4. README / CURRENT STATE / Runbook / TEST-MATRIX / PLAN-24 同步。
5. 一份简短执行报告，包含验证结果和仍未完成的微服务拆分边界。

---

## 5. 核准状态

**当前状态**: 待用户审查批准后执行。
