# PLAN-24: 稳定上线差距收口优先级

**生效日期**: 2026-04-28
**状态**: 执行中
**目标版本**: 内部/小范围真实试运行上线
**事实基线**: [CURRENT-STATE-2026-04-28](../reports/CURRENT-STATE-2026-04-28.md)

---

## 1. 排序原则

本计划只处理“离稳定上线最近、风险最高、依赖最多”的事项。排序依据：

1. 是否阻断上线判断。
2. 是否影响数据安全、财务正确性或客户可见体验。
3. 是否是后续任务的前置条件。
4. 是否能用自动化验收固定下来。

---

## 2. 优先级总览

| 排名 | 优先级 | 任务 | 当前问题 | 上线影响 | 目标状态 |
| :-- | :-- | :-- | :-- | :-- | :-- |
| 1 | P0 | 恢复后端测试基线 | `mvn test` 当前失败：AI 测试口径漂移、测试数据隔离污染 | 无法判断核心业务是否稳定 | `backend mvn test` 稳定全绿 |
| 2 | P0 | 恢复前端测试基线 | `npm test` 本地执行超时，无法确认组件回归 | 前端改动缺少可靠门禁 | `frontend npm test` 可稳定完成 |
| 3 | P0 | 固化上线验收矩阵 | 文档已有矩阵，但 CI/本地状态与实际不完全一致 | 上线决策无统一门槛 | build/test/e2e/AI 专项分层明确 |
| 4 | P1 | 认证与权限生产化 | 当前依赖可伪造 header 角色注入 | 不适合公网或非可信网络 | JWT/session + 角色绑定 + 服务间鉴权方案落地 |
| 5 | P1 | 数据库迁移治理 | 仍依赖 Hibernate `ddl-auto` 路径 | 数据结构变更不可审计，升级风险高 | Flyway/Liquibase 管理 schema |
| 6 | P1 | 部署与配置收口 | `.env`、Nacos、Compose、backend profile 有重复配置；Codex CLI 只适合本地 | 部署漂移与环境误判 | 试运行配置模板和生产禁用项明确 |
| 7 | P1 | AI/OCR 可靠性治理 | AI 上架/小票识别依赖多服务，fallback 多但观测不足 | 业务可用性和人工复核压力不稳定 | 可观测、可重试、可人工接管 |
| 8 | P2 | 微服务边界决策 | 单体 backend 与多模块服务壳并存 | 长期维护成本和部署边界不清 | 明确“先单体稳定”或“继续服务拆分” |
| 9 | P2 | 生产运维基础 | 缺少完整日志、指标、告警、备份恢复文档和演练 | 故障不可控 | 最小运维手册与备份恢复演练 |
| 10 | P3 | 后续商业能力 | 支付、OAuth、地图路径规划仍未实现 | 不影响内部试运行，但影响商业化 | 独立里程碑推进 |

---

## 3. P0: 上线判断门禁

### 3.1 恢复后端测试基线

**问题**

- `AiProductOnboardingServiceTest` 仍按旧视觉直连路径断言，当前代码已改为 OCR-first。
- `BobbuyStoreTest`、`ProcurementHudServiceTest`、`SecurityAuthorizationIntegrationTest` 存在测试数据污染，seed 后列表数量、容量、冻结状态互相影响。

**执行任务**

1. 为每个集成测试建立独立数据上下文：清库、唯一 ID、或 `@DirtiesContext` 分层使用。
2. 重写 AI 上架测试 mock：OCR -> LLM -> supplier rules -> source filter -> confirm 的当前链路。
3. 把“真实 AI 视觉验收”继续保留为 `RUN_AI_VISION_E2E=1` 门控，不进入默认 `mvn test`。
4. 固定失败样例，避免测试依赖执行顺序。

**验收标准**

- `cd backend && mvn test` 连续 2 次通过。
- Surefire 报告无失败、无错误。
- 不通过连接真实 Ollama/Codex/MinIO 来完成默认单测。

### 3.2 恢复前端测试基线

**问题**

- `npm test` 本地执行超过 3 分钟未完成，无法作为门禁。

**执行任务**

1. 用 `vitest --run --reporter=verbose` 定位卡住的测试文件。
2. 拆分慢测和 E2E 依赖，禁止单测等待真实计时器/网络。
3. 给新供应商页面、商品删除、AI 识别结果编辑补基础组件测试。
4. 明确常规单测与 Playwright E2E 的边界。

**验收标准**

- `cd frontend && npm test` 在可接受时间内稳定完成。
- `cd frontend && npm run build` 继续通过。
- 新增页面不会因缺 i18n key 显示裸 key。

### 3.3 固化上线验收矩阵

**执行任务**

1. 更新 CI，使它与 `TEST-MATRIX` 一致。
2. 把 AI 真实链路、E2E、CodeQL 作为分层门禁：默认、专用、风险登记。
3. 每次 Release 必须记录本地/CI 验证结果。

**验收标准**

- `docs/reports/TEST-MATRIX-本地与CI执行矩阵.md` 与 GitHub Actions 一致。
- README 不再宣称未通过的测试为已通过。

---

## 4. P1: 试运行安全与数据可靠性

### 4.1 认证与权限生产化

**问题**

当前 `X-BOBBUY-ROLE` / `X-BOBBUY-USER` 适合本地和受控内网，不适合公网。

**执行任务**

1. 增加登录 API 与用户凭据模型，或接入独立 auth-service。
2. 使用 JWT/session 表达用户身份与角色。
3. 后端禁止公网请求直接信任角色 header；服务间 header 需由 gateway 签发或内网校验。
4. 补客户只能访问本人订单/账单的集成测试。

**验收标准**

- 未登录访问受保护 API 返回 401。
- 非本人账单/订单访问返回 403/404。
- AGENT/CUSTOMER 权限矩阵有自动化覆盖。

### 4.2 数据库迁移治理

**问题**

当前 schema 变更依赖 Hibernate `ddl-auto`，上线升级不可审计。

**执行任务**

1. 选择 Flyway 或 Liquibase。
2. 从当前实体生成基线 migration。
3. 关闭生产 `ddl-auto=update`。
4. 为 JSONB、枚举、索引、唯一约束建立显式迁移。

**验收标准**

- 空库可通过 migration 初始化。
- 老库可通过 migration 升级。
- 生产 profile 不依赖 Hibernate 自动改表。

### 4.3 部署与配置收口

**问题**

AI、MinIO、Nacos、Compose、profile 配置分散；Codex CLI fallback 对服务器部署有明确限制。

**执行任务**

1. 整理 `.env.template`、Nacos config、`application-*.properties` 的配置优先级。
2. 明确服务器部署禁用 `codex-cli`，使用 Ollama/OpenAI API/私有 Codex gateway 三选一。
3. 写出试运行部署手册：启动、健康检查、回滚、日志路径。
4. 收敛默认密码与公网暴露端口。

**验收标准**

- 新机器按文档可完成一次干净部署。
- `docker-compose config` 无关键变量缺失。
- AI provider 选择在日志中可见。

### 4.4 AI/OCR 可靠性治理

**问题**

AI 链路有 fallback，但缺少统一任务状态、重试、监控和人工队列。

**执行任务**

1. 给 AI 上架和小票识别统一记录 provider、模型、耗时、错误码、fallback 原因。
2. 区分“识别失败”“来源治理失败”“人工待复核”“可发布”。
3. 增加私有 Codex gateway 模式的配置文档；生产默认不依赖 Windows 桌面登录态。
4. 保证 AI 失败不破坏订单/账单主链路。

**验收标准**

- AI 服务不可用时，用户看到可操作状态而不是静默失败。
- 每次 AI 结果可追溯到 provider 与原始输入。
- 人工复核可以接管失败结果。

---

## 5. P2: 架构与运维长期稳定

### 5.1 微服务边界决策

当前 `backend` 是主业务单体，多模块服务更多是部署外壳。稳定上线前建议先明确路线：

- **建议短期路线**：先保证单体业务与数据库稳定，多模块只作为部署适配层。
- **暂不建议**：在测试基线和认证未恢复前继续深拆微服务。

**验收标准**

- 文档明确主开发入口和部署入口。
- 不再出现同一能力在单体和服务模块中双重漂移。

### 5.2 生产运维基础

**执行任务**

1. 日志：请求 trace、AI trace、审计 trace 统一检索。
2. 指标：健康检查、错误率、AI 成功率、WebSocket 连接数、DB 连接池。
3. 告警：服务不可用、AI fallback 激增、数据库失败、MinIO 上传失败。
4. 备份：PostgreSQL、MinIO、Nacos 配置备份与恢复演练。

**验收标准**

- 有最小运维 Runbook。
- 至少完成一次备份恢复演练记录。

---

## 6. 暂不作为上线阻塞

以下能力重要，但不阻断内部/小范围真实试运行：

1. 第三方在线支付。
2. 社交 OAuth / SSO。
3. 地图路径规划与实时配送追踪。
4. 无人值守 AI 自动改账。
5. 完整 SaaS 多租户计费与运营后台。

---

## 7. 推荐执行顺序

1. 修后端测试。
2. 修前端测试。
3. 对齐 CI 与 TEST-MATRIX。
4. 做认证替代方案。
5. 引入数据库迁移。
6. 收敛部署配置与 AI provider。
7. 加 AI/OCR 可观测与人工接管。
8. 写运维 Runbook。
9. 决定微服务边界长期路线。
10. 再排支付/OAuth/地图等商业化能力。

