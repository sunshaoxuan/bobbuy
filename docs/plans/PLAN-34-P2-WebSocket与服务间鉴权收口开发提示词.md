# PLAN-34: P2 WebSocket 与服务间鉴权收口开发提示词

**生效日期**: 2026-04-29
**状态**: 待评审
**关联计划**: [PLAN-24: 稳定上线差距收口优先级](PLAN-24-稳定上线差距收口优先级.md)
**前置任务**:
- [PLAN-28: P1 认证与权限生产化开发提示词](PLAN-28-P1认证与权限生产化开发提示词.md)
- [PLAN-32: P2 微服务边界决策开发提示词](PLAN-32-P2-微服务边界决策开发提示词.md)
- [PLAN-33: P2 生产运维基础开发提示词](PLAN-33-P2-生产运维基础开发提示词.md)
**事实基线**: [CURRENT-STATE-2026-04-28](../reports/CURRENT-STATE-2026-04-28.md)

---

## 1. 开发目标

补齐当前仍登记为安全边界缺口的 WebSocket `/ws` 鉴权、服务间调用鉴权策略和 refresh token 取舍说明。目标是让试运行公网/半公网部署不再存在“HTTP 已鉴权、实时通道仍 permitAll”的明显裂口，并为未来服务外壳继续拆分提供最小可信边界。

当前事实：

1. HTTP API 已切到 username/password + HMAC JWT access token。
2. `BOBBUY_SECURITY_HEADER_AUTH_ENABLED` 默认关闭，header auth 只允许 dev/test 显式兼容。
3. `backend` 是源码主入口，`core-service` 是试运行部署事实源；`ai-service` / `im-service` / `auth-service` 当前仍是服务外壳。
4. `README` 和 `CURRENT STATE` 仍明确登记：WebSocket `/ws` 鉴权、refresh token 与服务间鉴权尚未完成。
5. 聊天已具备 REST 持久化 + WebSocket/STOMP 推送，前端失败时可回退到刷新/轮询。

---

## 2. 核心 Prompt 文本（请审查）

您可以直接将以下内容作为下一次开发任务的输入：

```text
### [任务: P2 WebSocket 与服务间鉴权收口]

任务背景：
BOBBuy 已完成 HTTP JWT 登录、header auth 生产禁用、服务边界 ADR、最小运维基础。当前安全缺口集中在 WebSocket `/ws` 仍 permitAll、服务间调用鉴权策略未固化、refresh token 仍未明确取舍。请补齐试运行阶段最小鉴权闭环，避免实时通道绕过 HTTP 鉴权。

目标：
1. WebSocket/STOMP 连接必须校验用户身份，未登录连接应拒绝或立即关闭。
2. 前端 WebSocket 客户端必须携带 access token，并在 token 缺失/过期时退回已存在的 REST/刷新路径。
3. 服务间调用鉴权形成明确策略：短期共享 JWT、内部 service token、gateway 签发 header、或保持内网约束但文档登记风险。
4. refresh token 明确本阶段是否实现；如暂缓，必须写清 access token TTL、重新登录策略与风险。
5. 默认测试继续不依赖真实 RabbitMQ / WebSocket 外部服务。

必须先做的排查：
1. 阅读安全配置：
   - `backend/src/main/java/com/bobbuy/security/**`
   - `backend/src/main/java/com/bobbuy/api/AuthController.java`
   - `backend/src/main/resources/application*.properties`
   - `backend/src/test/resources/application.properties`
2. 阅读 WebSocket / Chat：
   - `backend/src/main/java/com/bobbuy/config/WebSocketConfig.java`
   - `backend/src/main/java/com/bobbuy/service/ChatService.java`
   - `backend/src/main/java/com/bobbuy/api/ChatController.java`
   - 前端 `useChatWebSocket` / `ChatWidget` / 客户侧聊天相关组件
3. 阅读 gateway / 服务外壳：
   - `bobbuy-gateway/**`
   - `bobbuy-im/**`
   - `docker-compose.yml`
   - `infra/nacos/config/*.yaml`
   - `infra/nginx/prod.conf`
4. 阅读当前文档：
   - `README.md`
   - `docs/reports/CURRENT-STATE-2026-04-28.md`
   - `docs/runbooks/RUNBOOK-试运行部署.md`
   - `docs/runbooks/RUNBOOK-监控告警与故障处置.md`
   - `docs/architecture/ADR-01-试运行阶段服务边界决策.md`

修复范围 A：WebSocket 鉴权设计
1. 选择并实现一种兼容前端的 token 传递方式：
   - STOMP CONNECT header `Authorization: Bearer <token>`；或
   - query 参数短期方案 `?access_token=...`，但必须评估日志泄漏风险；或
   - SockJS / STOMP 客户端可支持的等价方式。
2. 后端在握手或 STOMP CONNECT 阶段验证 JWT：
   - token 有效：建立 Principal / Authentication。
   - token 缺失：拒绝连接。
   - token 过期或伪造：拒绝连接。
3. 服务端消息订阅 / 发送应基于当前用户做最小权限校验：
   - customer 只能访问与本人相关订单/聊天上下文。
   - agent / admin 可访问运营聊天上下文。
4. 不得在日志中输出完整 token；如使用 query token，日志必须避免记录完整 query。

修复范围 B：前端 WebSocket 客户端
1. 从当前登录态读取 access token。
2. 建立 WebSocket/STOMP 连接时携带 token。
3. token 缺失/过期/连接被拒绝时：
   - 不无限重连。
   - 显示可恢复状态或静默降级到 REST 刷新。
   - 不清空本地未发送消息。
4. 补测试覆盖：
   - 有 token 时发起带 token 的连接。
   - 无 token 时不建立 WebSocket 或进入降级。
   - 连接失败时不触发无限循环。

修复范围 C：服务间鉴权策略
1. 明确当前试运行边界：
   - `core-service` 为事实源。
   - `ai-service` / `im-service` / `auth-service` 为服务外壳。
   - gateway-service 只路由，不写业务库。
2. 选择短期服务间鉴权方案并文档化：
   - 内部 service token header，secret 由环境变量配置；或
   - gateway 签发内部 header，后端只信任来自 gateway 的 header；或
   - 暂不实现代码，仅限制内网并登记风险。
3. 如果实现 service token：
   - 新增 `BOBBUY_SECURITY_SERVICE_TOKEN` 或等价配置。
   - 默认空值时不得开放公网信任。
   - 不得把真实 token 写入 `.env.template`。
   - 补 401/403 测试。
4. 如果暂缓实现，必须在 README / Runbook / CURRENT STATE 中写清“服务间鉴权未完成，不得继续真实微服务拆分”。

修复范围 D：refresh token 取舍
1. 评估是否本阶段实现 refresh token：
   - 若实现：需要存储、撤销、过期、轮换、测试与前端刷新流程。
   - 若暂缓：明确 access token TTL、重新登录体验和试运行风险。
2. 不得只新增 refresh API 而没有撤销/过期策略。
3. 若暂缓，文档中不要把 refresh token 写成已完成。

修复范围 E：测试
1. 后端测试至少覆盖：
   - WebSocket 无 token 被拒绝。
   - WebSocket token 无效/过期被拒绝。
   - WebSocket token 有效可建立用户上下文。
   - customer 不能订阅/发送非本人上下文。
   - header auth 关闭时不能借 header 提权。
2. 前端测试至少覆盖：
   - WebSocket 连接携带 token。
   - 无 token / 过期时进入降级。
   - 连接失败不会无限重连。
3. 默认测试不得依赖真实 RabbitMQ 或外部 STOMP broker。

修复范围 F：文档同步
1. 更新：
   - `README.md`
   - `docs/reports/CURRENT-STATE-2026-04-28.md`
   - `docs/runbooks/RUNBOOK-试运行部署.md`
   - `docs/runbooks/RUNBOOK-监控告警与故障处置.md`
   - `docs/reports/TEST-MATRIX-本地与CI执行矩阵.md`
   - `docs/plans/PLAN-24-稳定上线差距收口优先级.md`
   - `.env.template`（如新增 service token 或 WebSocket 配置）
2. 明确：
   - WebSocket 鉴权状态。
   - 服务间鉴权状态。
   - refresh token 是否实现。
   - 试运行部署需要配置哪些 secret。

验收命令：
1. `cd backend && mvn test`
2. `cd frontend && npm test`
3. `cd frontend && npm run build`
4. `docker compose config`
5. 如改 gateway / Compose / Nacos：补充相关配置渲染或启动 smoke 验证。

交付要求：
1. 提供安全收口摘要：
   - WebSocket 鉴权方案
   - 服务间鉴权策略
   - refresh token 取舍
   - 前端降级行为
2. 提供修改清单：
   - 后端
   - 前端
   - 配置
   - 测试
   - 文档
3. 提供验证结果。
4. 登记仍未完成的边界：
   - OAuth / SSO
   - refresh token（若暂缓）
   - 完整服务间 mTLS / service mesh
   - 独立微服务 schema 与契约测试

禁止事项：
1. 不得把 WebSocket `/ws` 继续留成公网 permitAll 却宣称安全收口完成。
2. 不得把 JWT 或 service token 写入日志、文档或仓库。
3. 不得让 query token 被默认访问日志完整记录。
4. 不得实现只有签发没有撤销/过期策略的 refresh token。
5. 不得回滚非本任务相关的用户改动。

建议实现顺序：
1. 先确认 WebSocket 技术栈和前端 STOMP 客户端 token 支持方式。
2. 后端实现 WebSocket JWT 校验和最小授权测试。
3. 前端接入 token 传递与失败降级测试。
4. 决定服务间鉴权与 refresh token 本阶段取舍。
5. 同步 README、CURRENT STATE、Runbook、TEST MATRIX、PLAN-24。
6. 跑默认门禁并提交执行报告。
```

---

## 3. 审核重点

请重点审核以下问题：

1. WebSocket token 传递采用 STOMP CONNECT header，还是 query 参数短期方案。
2. 本阶段是否实现 service token，还是只先文档化内网边界和风险。
3. refresh token 是否继续暂缓，避免引入半套 token 生命周期。
4. customer 聊天上下文授权的最小可验收边界是什么。
5. 是否需要额外做一个本地 WebSocket smoke test，还是先由后端/前端单测覆盖。

---

## 4. 预期交付物

1. WebSocket JWT 鉴权实现与测试。
2. 前端 WebSocket token 接入与失败降级。
3. 服务间鉴权策略文档或最小 service token 实现。
4. refresh token 取舍说明。
5. README / CURRENT STATE / Runbook / TEST-MATRIX / PLAN-24 同步。
6. 一份简短执行报告，包含验证命令和仍未完成的安全边界。

---

## 5. 核准状态

**当前状态**: 待用户审查批准后执行。
