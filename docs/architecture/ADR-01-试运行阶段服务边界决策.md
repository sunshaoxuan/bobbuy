# ADR-01: 试运行阶段服务边界决策

**生效日期**: 2026-04-29  
**状态**: 已采纳  
**关联计划**: [PLAN-32: P2 微服务边界决策开发提示词](../plans/PLAN-32-P2-微服务边界决策开发提示词.md)

---

## Context

BOBBuy 当前同时存在：

- `backend`：开发/测试主入口，包含主要 Controller、Service、Repository、Flyway migration 与自动化测试。
- `bobbuy-core`、`bobbuy-ai`、`bobbuy-im`、`bobbuy-auth`：通过 `ComponentScan excludeFilters` 从共享 `com.bobbuy` 代码中裁剪出的服务外壳。
- `bobbuy-gateway`：Spring Cloud Gateway 路由层。
- `docker-compose.yml` / Nacos：已按 `core-service`、`ai-service`、`im-service`、`auth-service`、`gateway-service` 组织试运行部署。

现状风险：

1. 同一套 `com.bobbuy` 业务代码同时被 `backend` 与多个服务外壳复用，容易被误读为“已完成独立微服务拆分”。
2. `core-service`、`ai-service`、`im-service`、`auth-service` 共用同一个 PostgreSQL schema，除 `gateway-service` 外均可访问共享业务库。
3. 只有 `core-service` 执行 Flyway；其他服务默认禁用 Flyway，但文档口径仍可能夸大独立服务职责。
4. 当前默认测试门禁主要覆盖 `backend`、`frontend` 与 Compose 渲染，尚未形成服务壳级别的独立启动测试、服务间鉴权、独立 schema、分布式 tracing、服务级 SLO。

---

## Decision

试运行阶段采用 **“主业务单体优先稳定”** 作为默认路线。

1. **主业务入口**
   - 源码入口：`backend`
   - 试运行部署事实源：`core-service`
2. **事实源边界**
   - 订单、行程、采购、账单、钱包、审计、用户资料等核心业务事实源，统一以 `backend` 共享代码 + `core-service` 运行实例为准。
   - `core-service` 是唯一 Flyway migration 执行者。
3. **服务外壳边界**
   - `ai-service`、`im-service`、`auth-service` 当前仍属于**服务外壳**：它们暴露特定入口，但继续复用 `backend` 共享代码、共享数据库与共享安全配置。
   - 这些服务外壳**不是独立业务事实源**，不得未经额外门禁演进为独立写核心数据的服务。
4. **网关边界**
   - `gateway-service` 只负责路由与流量分发，不承担业务写库职责。
5. **Compose 策略**
   - 试运行默认 Compose 先保持现状，不在本任务中把 `ai-service` / `im-service` / `auth-service` 降为 profile/optional。
   - 原因：当前网关路由、Nacos 服务发现与缺失的服务壳启动回归测试尚不足以支撑安全降级。

---

## Options considered

### 方案 A：主业务单体优先稳定（采纳）

优点：

- 与当前代码事实一致：主要业务、测试、Flyway、发布节奏都集中在 `backend`。
- 可立即统一文档口径，减少“看起来像微服务、实际仍是共享代码/共享库”的漂移。
- 避免在服务间鉴权、独立数据库、服务级 CI/CD 尚未就绪时继续拆分。

代价：

- `ai-service` / `im-service` / `auth-service` 继续作为服务外壳存在，短期仍有命名与部署复杂度。
- 需要持续约束文档、Runbook、Nacos、Compose 不夸大服务独立性。

### 方案 B：继续推进当前多服务拆分（未采纳）

优点：

- 理论上可更早逼近独立服务部署形态。

未采纳原因：

- 当前没有独立 schema / 数据所有权。
- 没有稳定的服务间鉴权方案。
- 没有服务壳独立测试、启动 smoke、Tracing、SLO 与拆分后的 CI/CD。
- 继续拆分会在试运行阶段放大部署、回滚与排障成本。

---

## Consequences

1. `backend` 必须继续作为开发、调试、自动化测试与 migration 源码主入口。
2. `core-service` 被明确为试运行核心业务事实源与 Flyway 执行者。
3. `ai-service`、`im-service`、`auth-service` 被明确为共享代码驱动的服务外壳，而不是完成领域隔离后的独立服务。
4. 文档必须统一使用以下术语：
   - **主业务入口**
   - **服务外壳**
   - **事实源**
   - **可选服务**
   - **后续拆分候选**
5. 继续保留当前 Compose 全量启动时，必须在文档中明确：
   - 默认启动不等于独立事实源
   - 共享 PostgreSQL schema 仍是当前边界
   - 服务间鉴权、独立数据库、Tracing、SLO、拆分后 CI/CD 仍待后续任务

---

## Migration plan

1. 先统一 README、CURRENT STATE、Runbook、ARCH-13、TEST-MATRIX、PLAN-24 的边界术语。
2. 在文档中固定模块职责矩阵，明确哪些模块只有共享代码或服务外壳职责。
3. 继续保持 `core-service` 独占 Flyway migration；其他服务默认禁用。
4. 在未来任何“降级为 optional/profile”或“继续拆分”的动作前，先补：
   - 服务壳启动 smoke test
   - 服务间鉴权
   - 数据所有权 / 独立 schema
   - 回滚与告警方案
5. 若未来确实要拆分，优先从 `auth-service`、`ai-service`、`im-service` 中选择单一候选逐步推进，而不是同时深拆多个共享数据库服务。

---

## Reversal criteria

只有满足以下全部条件，才允许撤销本 ADR 并切换到“继续拆分”为默认路线：

1. 至少一个候选服务已具备独立数据所有权（独立 schema 或独占表集）。
2. 服务间鉴权方案已经落地并有自动化回归覆盖。
3. 候选服务拥有独立启动测试、契约测试与 CI/CD 门禁。
4. 已具备基础分布式 tracing、服务级日志检索与 SLO。
5. 拆分后的部署/回滚 Runbook 已完成演练，并证明不劣于当前单体优先路线。

---

## 模块 / 服务职责矩阵

| 模块 / 服务 | 是否有真实业务实现 | 是否被 Compose 启动 | 是否访问数据库 | 是否执行 Flyway | 是否需要 JWT 校验 | 是否有独立测试 | 生产 / 试运行职责 |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| `backend` | 是 | 否 | 是 | 是 | 是 | 是 | 开发/测试主入口；核心业务、Flyway 与默认自动化测试事实源 |
| `bobbuy-common` | 否（共享依赖与公共配置） | 否 | 否（不独立运行） | 否 | 否（不独立运行） | 否 | 共享依赖、DTO、基础配置承载层 |
| `core-service` | 部分（复用 `backend` 共享代码） | 是 | 是 | 是 | 是 | 否 | 试运行核心业务事实源；订单/行程/采购/账单/钱包/审计等主链路入口 |
| `ai-service` | 部分（复用 `backend` AI/OCR 相关代码） | 是 | 是 | 否 | 是 | 否 | AI/OCR 服务外壳；不是独立核心数据事实源 |
| `im-service` | 部分（复用 `backend` 聊天/WebSocket 相关代码） | 是 | 是 | 否 | 是 | 否 | 聊天/WebSocket 服务外壳；共享数据库与 RabbitMQ |
| `auth-service` | 部分（复用 `backend` 登录/JWT 相关代码） | 是 | 是 | 否 | 是 | 否 | 认证服务外壳；不是独立身份事实源 |
| `gateway-service` | 否（路由层） | 是 | 否 | 否 | 否 | 否 | Spring Cloud Gateway；只负责流量分发与健康检查 |

