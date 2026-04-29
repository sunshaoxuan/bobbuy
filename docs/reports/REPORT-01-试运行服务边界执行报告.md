# REPORT-01: 试运行服务边界执行报告

**日期**: 2026-04-29  
**对应任务**: [PLAN-32: P2 微服务边界决策开发提示词](../plans/PLAN-32-P2-微服务边界决策开发提示词.md)

---

## 1. 架构决策摘要

1. **选择路线**：试运行阶段采用“主业务单体优先稳定”。
2. **选择理由**：
   - `backend` 仍是核心业务、Flyway migration 与默认自动化测试的真实入口。
   - `core-service`、`ai-service`、`im-service`、`auth-service` 当前仍主要是共享 `backend` 代码的服务外壳。
   - 共享 PostgreSQL schema、缺失服务间鉴权与缺失服务壳独立测试，决定了当前不适合继续深拆。
3. **对 Compose / Nacos / Runbook 的影响**：
   - 保持 `core-service` 为唯一 Flyway 执行者。
   - 保持现有网关路由与默认 Compose 启动方式不变。
   - 文档统一把 `ai-service`、`im-service`、`auth-service` 定义为服务外壳，而不是独立事实源。

---

## 2. 模块 / 服务职责矩阵

| 模块 / 服务 | 是否有真实业务实现 | 是否被 Compose 启动 | 是否访问数据库 | 是否执行 Flyway | 是否需要 JWT 校验 | 是否有独立测试 | 生产 / 试运行职责 |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| `backend` | 是 | 否 | 是 | 是 | 是 | 是 | 开发/测试主入口；主要 Controller、Service、Repository、Flyway 与测试均在此 |
| `bobbuy-common` | 否（共享依赖层） | 否 | 否 | 否 | 否 | 否 | 共享依赖、配置与 DTO/响应模型 |
| `core-service` | 部分（复用共享 `backend` 代码） | 是 | 是 | 是 | 是 | 否 | 核心业务事实源；默认 `/api/**` 路由目标 |
| `ai-service` | 部分（复用共享 `backend` 代码） | 是 | 是 | 否 | 是 | 否 | AI/OCR 服务外壳；`/api/ai/**` 路由目标 |
| `im-service` | 部分（复用共享 `backend` 代码） | 是 | 是 | 否 | 是 | 否 | 聊天 / WebSocket 服务外壳；`/api/chat/**` 与 `/ws/**` 路由目标 |
| `auth-service` | 部分（复用共享 `backend` 代码） | 是 | 是 | 否 | 是 | 否 | 登录 / JWT 服务外壳；`/api/auth/**` 路由目标 |
| `gateway-service` | 否（路由层） | 是 | 否 | 否 | 否 | 否 | Spring Cloud Gateway 路由层，不写业务库 |

---

## 3. 风险登记

1. **共享数据库风险**：`core-service`、`ai-service`、`im-service`、`auth-service` 仍共用同一 PostgreSQL schema。
2. **边界漂移风险**：服务外壳仍复用 `backend` 共享代码，若文档口径失真，容易被误读为“已完成独立微服务拆分”。
3. **测试缺口**：当前没有服务壳独立启动 smoke test、契约测试与拆分后 CI/CD。
4. **安全缺口**：服务间鉴权、WebSocket 鉴权、refresh token 仍待后续任务。

---

## 4. 修改清单

- 新增 [ADR-01-试运行阶段服务边界决策](../architecture/ADR-01-试运行阶段服务边界决策.md)
- 更新 `README.md`
- 更新 [CURRENT-STATE-2026-04-28](CURRENT-STATE-2026-04-28.md)
- 更新 [RUNBOOK-试运行部署](../runbooks/RUNBOOK-试运行部署.md)
- 更新 [ARCH-13-全栈容器化部署方案](../architecture/ARCH-13-全栈容器化部署方案.md)
- 更新 [TEST-MATRIX-本地与CI执行矩阵](TEST-MATRIX-本地与CI执行矩阵.md)
- 更新 [PLAN-24-稳定上线差距收口优先级](../plans/PLAN-24-稳定上线差距收口优先级.md)

---

## 5. 验证结果

- [x] `cd /home/runner/work/bobbuy/bobbuy && docker compose config`
- [x] `cd /home/runner/work/bobbuy/bobbuy/backend && mvn test`
- [x] `cd /home/runner/work/bobbuy/bobbuy/frontend && npm ci && npm test`
- [x] `cd /home/runner/work/bobbuy/bobbuy/frontend && npm run build`

说明：

- 本次未改动 Java 服务模块构建脚本、Dockerfile 或服务实现代码，因此未额外执行 `mvn -DskipTests package` 与相关 image build。
- 前端测试仍有已知非阻塞噪声（Ant Design `useForm` warning、预期失败路径 console error），不影响本次文档与边界决策结论。

---

## 6. 仍未完成的边界

1. 服务间鉴权
2. 独立数据库 / Schema
3. 分布式 tracing
4. 服务级 SLO
5. 拆分后的 CI/CD

只有在以上条件补齐后，才建议继续推进真实微服务拆分。
