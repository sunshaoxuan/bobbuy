# PLAN-32: P2 微服务边界决策开发提示词

**生效日期**: 2026-04-29
**状态**: 待评审
**关联计划**: [PLAN-24: 稳定上线差距收口优先级](PLAN-24-稳定上线差距收口优先级.md)
**前置任务**:
- [PLAN-28: P1 认证与权限生产化开发提示词](PLAN-28-P1认证与权限生产化开发提示词.md)
- [PLAN-29: P1 数据库迁移治理开发提示词](PLAN-29-P1数据库迁移治理开发提示词.md)
- [PLAN-30: P1 部署与配置收口开发提示词](PLAN-30-P1部署与配置收口开发提示词.md)
- [PLAN-31: P1 AI 与 OCR 可靠性治理开发提示词](PLAN-31-P1-AI与OCR可靠性治理开发提示词.md)
**事实基线**: [CURRENT-STATE-2026-04-28](../reports/CURRENT-STATE-2026-04-28.md)

---

## 1. 开发目标

明确 BOBBuy 在内部/小范围试运行阶段的系统边界：短期以主业务单体稳定为第一优先，还是继续推进多模块服务拆分。目标不是为了拆而拆，而是消除 `backend`、`bobbuy-core`、`bobbuy-ai`、`bobbuy-im`、`bobbuy-auth`、`bobbuy-gateway` 与 Docker/Nacos 配置之间的责任漂移。

当前事实：

1. 业务主实现仍集中在 `backend`。
2. Compose 已包含 gateway、core、ai、im、auth 等服务外壳。
3. Flyway 已收口为 core-service 执行迁移，其他服务默认禁用。
4. JWT、AI/OCR、采购、账单、钱包等试运行关键链路已进入可验收状态。
5. 如果不先明确边界，后续监控、告警、备份、服务间鉴权和部署 Runbook 会持续分裂。

---

## 2. 核心 Prompt 文本（请审查）

您可以直接将以下内容作为下一次开发任务的输入：

```text
### [任务: P2 微服务边界决策]

任务背景：
BOBBuy 当前同时存在主业务 backend 单体与 bobbuy-core / bobbuy-ai / bobbuy-im / bobbuy-auth / bobbuy-gateway 等服务外壳。P0/P1 已收口测试、认证、迁移、部署配置、AI/OCR 可靠性。下一步需要明确试运行阶段到底采用“单体优先稳定”还是“继续服务拆分”，并把代码入口、部署入口、数据所有权、服务间调用和文档口径固定下来。

目标：
1. 形成一份架构决策记录 ADR，明确试运行阶段主路线。
2. 梳理所有模块/服务的职责、真实代码覆盖、启动方式、依赖关系和数据访问边界。
3. 消除 README、Runbook、Compose、Nacos、CI、当前状态文档中的架构口径冲突。
4. 如果选择单体优先，明确哪些服务外壳暂作为部署适配，不承载独立业务事实源。
5. 如果选择继续拆分，列出最小可执行拆分清单、服务间鉴权、数据所有权和验收门禁。

必须先做的排查：
1. 阅读模块与构建入口：
   - `pom.xml`
   - `backend/pom.xml`
   - `bobbuy-common/pom.xml`
   - `bobbuy-core/pom.xml`
   - `bobbuy-ai/pom.xml`
   - `bobbuy-im/pom.xml`
   - `bobbuy-auth/pom.xml`
   - `bobbuy-gateway/pom.xml`
   - `Dockerfile.service`
2. 阅读部署入口：
   - `docker-compose.yml`
   - `.env.template`
   - `infra/nacos/config/*.yaml`
   - `infra/nginx/prod.conf`
3. 阅读主业务实现：
   - `backend/src/main/java/com/bobbuy/**`
   - 特别关注 auth、AI/OCR、采购、订单、账单、钱包、WebSocket、gateway 相关能力
4. 阅读服务模块实现：
   - `bobbuy-core/src/main/java/**`
   - `bobbuy-ai/src/main/java/**`
   - `bobbuy-im/src/main/java/**`
   - `bobbuy-auth/src/main/java/**`
   - `bobbuy-gateway/src/main/java/**`
5. 阅读文档：
   - `README.md`
   - `docs/reports/CURRENT-STATE-2026-04-28.md`
   - `docs/runbooks/RUNBOOK-试运行部署.md`
   - `docs/architecture/ARCH-13-全栈容器化部署方案.md`
   - `docs/reports/TEST-MATRIX-本地与CI执行矩阵.md`
   - `docs/plans/PLAN-24-稳定上线差距收口优先级.md`

修复范围 A：架构事实盘点
1. 生成模块职责表，至少包含：
   - 模块名
   - 是否有真实业务实现
   - 是否被 Compose 启动
   - 是否访问数据库
   - 是否执行 Flyway
   - 是否需要 JWT 校验
   - 是否有独立测试
   - 生产/试运行职责
2. 标记重复能力：
   - auth 能力是否只在 backend 实现，还是 auth-service 也承载事实。
   - AI/OCR 能力是否只在 backend 实现，还是 ai-service 也承载事实。
   - IM/WebSocket 是否有单独服务事实源。
   - gateway-service 与 nginx gateway 的边界。
3. 标记风险：
   - 同一业务由多个服务同时写库。
   - 多服务共享表但无所有权。
   - 文档宣称与代码不一致。
   - Compose 启动服务但功能仍回到 backend。

修复范围 B：ADR 决策
1. 新增 ADR，例如：
   - `docs/architecture/ADR-01-试运行阶段服务边界决策.md`
2. ADR 必须包含：
   - Context
   - Decision
   - Options considered
   - Consequences
   - Migration plan
   - Reversal criteria
3. 推荐默认决策：
   - 试运行阶段采用“主业务单体优先稳定”。
   - `backend` / `core-service` 承载核心业务事实源。
   - 其他服务外壳暂作为后续拆分适配层或专用能力入口，不得未经门禁独立写核心业务事实。
4. 如代码事实显示已具备可靠拆分条件，可以提出继续拆分方案，但必须给出更高测试、部署、服务间鉴权成本。

修复范围 C：部署与配置口径
1. 更新 Compose / Nacos 文档说明：
   - 哪些服务是必需服务。
   - 哪些服务是可选或预留。
   - 哪个服务执行迁移。
   - 哪些服务不得在试运行中独立写核心库。
2. 如发现 Compose 与实际主入口严重冲突，优先更新文档和 Runbook；只有明确安全时才改 Compose。
3. 不要为了“看起来微服务化”增加新服务或复制业务逻辑。

修复范围 D：代码与测试边界
1. 如果服务模块只是 thin shell，需要明确：
   - 是否仍应参与默认 Docker build。
   - 是否需要基础启动测试。
   - 是否应从默认试运行 Compose 中降级为 profile/optional。
2. 如果某服务已有真实业务逻辑，需要明确：
   - 数据所有权。
   - API contract。
   - 服务间认证。
   - 回归测试。
3. 本任务原则上以文档、配置和边界声明为主，不做大规模业务搬迁。

修复范围 E：文档同步
1. 更新：
   - `README.md`
   - `docs/reports/CURRENT-STATE-2026-04-28.md`
   - `docs/runbooks/RUNBOOK-试运行部署.md`
   - `docs/architecture/ARCH-13-全栈容器化部署方案.md`
   - `docs/reports/TEST-MATRIX-本地与CI执行矩阵.md`
   - `docs/plans/PLAN-24-稳定上线差距收口优先级.md`
2. 所有文档必须用同一套术语：
   - 主业务入口
   - 服务外壳
   - 事实源
   - 可选服务
   - 后续拆分候选

验收命令：
1. `docker compose config`
2. `cd backend && mvn test`
3. `cd frontend && npm test`
4. `cd frontend && npm run build`
5. 如改动 Java 模块构建或 Dockerfile：`mvn -DskipTests package` 与相关 image build

交付要求：
1. 提供架构决策摘要：
   - 选择单体优先还是继续拆分。
   - 选择理由。
   - 对 Compose/Nacos/Runbook 的影响。
2. 提供模块职责表。
3. 提供修改清单。
4. 提供验证结果。
5. 登记仍未完成的边界：
   - 服务间鉴权
   - 独立数据库/Schema
   - 分布式 tracing
   - 服务级 SLO
   - 拆分后的 CI/CD

禁止事项：
1. 不得未经测试搬迁核心业务逻辑。
2. 不得让多个服务同时成为同一核心数据的写入事实源。
3. 不得让文档宣称已完成的微服务拆分超过代码事实。
4. 不得扩大默认 CI 门禁到不可稳定执行的外部环境。
5. 不得回滚非本任务相关的用户改动。

建议实现顺序：
1. 先盘点模块和服务事实，不改代码。
2. 写 ADR，明确试运行阶段决策。
3. 同步 README、Runbook、ARCH-13、CURRENT STATE、TEST MATRIX、PLAN-24。
4. 仅在必要时微调 Compose profile 或文档中的启动命令。
5. 跑默认门禁并提交执行报告。
```

---

## 3. 审核重点

请重点审核以下问题：

1. 试运行阶段是否接受“主业务单体优先稳定”作为默认决策。
2. 是否需要把部分服务外壳从默认 Compose 改为 profile/optional。
3. `core-service` 与 `backend` 的命名和职责是否需要立即统一。
4. 是否要求本阶段补服务模块启动测试。
5. 是否把服务间鉴权放入本任务，还是登记到后续 P2/P3。

---

## 4. 预期交付物

1. 一份 ADR：试运行阶段服务边界决策。
2. 一张模块/服务职责矩阵。
3. README / CURRENT STATE / Runbook / ARCH-13 / TEST-MATRIX / PLAN-24 同步。
4. 如必要，轻量调整 Compose profile 或启动说明。
5. 一份简短执行报告，包含决策、风险、验证命令和后续拆分条件。

---

## 5. 核准状态

**当前状态**: 待用户审查批准后执行。
