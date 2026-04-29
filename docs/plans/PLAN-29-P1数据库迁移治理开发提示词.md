# PLAN-29: P1 数据库迁移治理开发提示词

**生效日期**: 2026-04-29
**状态**: 待评审
**关联计划**: [PLAN-24: 稳定上线差距收口优先级](PLAN-24-稳定上线差距收口优先级.md)
**前置任务**:
- [PLAN-27: P0 上线验收矩阵与 CI 固化开发提示词](PLAN-27-P0上线验收矩阵与CI固化开发提示词.md)
- [PLAN-28: P1 认证与权限生产化开发提示词](PLAN-28-P1认证与权限生产化开发提示词.md)
**事实基线**: [CURRENT-STATE-2026-04-28](../reports/CURRENT-STATE-2026-04-28.md)

---

## 1. 开发目标

把当前依赖 Hibernate `ddl-auto` 的数据库结构管理，升级为可审计、可回滚评估、可在空库初始化的迁移治理。

当前事实：

1. `backend` 尚未引入 Flyway / Liquibase。
2. `application-dev.properties` 仍使用 `spring.jpa.hibernate.ddl-auto=update`。
3. `application-prod.properties` 已是 `ddl-auto=none`，但没有迁移脚本支撑生产空库初始化。
4. PLAN-28 新增认证字段后，数据库结构治理已经成为上线前必须收口的 P1 风险。

本任务目标是建立最小可靠的迁移基线，而不是一次性做复杂数据迁移平台。

---

## 2. 核心 Prompt 文本（请审查）

您可以直接将以下内容作为下一次开发任务的输入：

```text
### [任务: P1 数据库迁移治理]

任务背景：
BOBBuy 已完成 P0 测试/CI 门禁和 P1 认证生产化第一阶段。当前数据库 schema 仍主要依赖 Hibernate `ddl-auto`：dev 使用 `update`，test 使用 H2 `create-drop`，prod 使用 `none` 但缺少迁移脚本。认证字段、JSONB 字段、财务/订单/采购表结构已经足够复杂，继续依赖 Hibernate 自动建表不适合试运行上线。请引入数据库迁移治理，让空库可初始化，生产 profile 不依赖 Hibernate 自动改表。

目标：
1. 选择并引入 Flyway 或 Liquibase；推荐 Flyway，除非代码库已有强理由使用 Liquibase。
2. 为当前 JPA 实体建立 PostgreSQL 基线 migration。
3. 让生产/试运行 PostgreSQL 空库可以通过 migration 初始化。
4. 关闭 dev/prod 对 Hibernate `ddl-auto=update` 的依赖；保留 test 使用 H2 `create-drop` 或单独测试迁移策略。
5. 为认证字段、JSONB、枚举、索引、唯一约束建立显式迁移。
6. 更新 CI / 文档，明确 migration 验证方式。

必须先做的排查：
1. 阅读依赖与配置：
   - `backend/pom.xml`
   - `backend/src/main/resources/application.properties`
   - `backend/src/main/resources/application-dev.properties`
   - `backend/src/main/resources/application-prod.properties`
   - `backend/src/test/resources/application.properties`
   - `docker-compose.yml`
2. 阅读所有 JPA 实体：
   - `backend/src/main/java/com/bobbuy/model/*.java`
3. 特别关注以下模型：
   - `User`：认证字段、username、passwordHash、enabled。
   - `OrderHeader` / `OrderLine`：订单头行与业务 ID。
   - `Product` / `Category` / `Supplier` / `MerchantSku`：JSONB、多语言、供应商规则、商品编号。
   - `ProcurementReceipt` / `CustomerPaymentLedger` / `FinancialAuditLog` / `PartnerWallet` / `WalletTransaction`：财务与审计链。
   - `ChatMessage`：聊天持久化与检索字段。
4. 先运行当前门禁确认基线：
   - `cd backend && mvn test`
   - `cd frontend && npm test`
   - `cd frontend && npm run build`

修复范围 A：迁移工具选择与配置
1. 引入 Flyway 或 Liquibase 依赖。
2. 推荐 Flyway 配置：
   - migration 路径：`backend/src/main/resources/db/migration`
   - 初始脚本：`V1__baseline_schema.sql`
3. 配置 PostgreSQL profile 自动执行 migration。
4. 生产 profile 必须保持：
   - `spring.jpa.hibernate.ddl-auto=none`
   - migration 作为 schema 初始化来源。
5. dev profile 应从 `ddl-auto=update` 收口：
   - 推荐改为 `validate` 或 `none`，由 migration 初始化本地库。
   - 如为兼容短期保留 `update`，必须明确登记为临时例外，并提供关闭计划。

修复范围 B：基线 schema
1. 根据当前实体生成 PostgreSQL DDL，手工审查后提交 migration。
2. 必须覆盖：
   - 主键、外键、非空约束。
   - 唯一约束，例如 username、businessId、商品/商家 SKU 等实际唯一语义。
   - 索引，例如 tripId、customerId、businessId、order status、chat conversation、ledger 查询字段。
   - JSONB 字段，例如多语言名称/描述、media gallery、supplier onboardingRules、receipt recognition result 等。
   - 枚举字段的 varchar/check 策略。若不做 check constraint，需文档说明。
3. 对金额字段保持当前代码口径，不在本任务中强行重构 BigDecimal；如发现明显财务精度风险，登记为后续专项。
4. 不要删除或重命名实体字段来适配 migration，除非发现真实 schema bug 并补测试。

修复范围 C：空库与升级验证
1. 增加一个迁移验证测试或 CI 命令：
   - 使用 PostgreSQL 容器优先。
   - 如 CI 复杂度过高，至少提供本地 `docker compose` 验证步骤，并在文档登记。
2. 验证空库启动路径：
   - 启动 PostgreSQL 空库。
   - 执行 migration。
   - 后端启动不依赖 Hibernate 自动建表。
3. 验证已有测试不退化：
   - 默认 `mvn test` 继续通过。
   - H2 测试如不运行 Flyway，需说明 H2 与 PostgreSQL migration 验证边界。
4. 如果引入 Testcontainers，需评估 CI 耗时和 Docker 可用性，不要让默认门禁变得不稳定。

修复范围 D：配置与部署
1. 更新 `.env.template`，补充 migration 相关说明（如有）。
2. 更新 `docker-compose.yml` 或服务环境变量：
   - 保证 backend/core-service 使用 migration 初始化数据库。
   - 确保生产不再依赖 `ddl-auto=update`。
3. 如多模块服务共用数据库，明确哪个服务负责执行 migration：
   - 推荐短期由主 `backend` / `core-service` 负责。
   - 避免多个服务并发重复迁移造成启动竞态。
4. 如果 Nacos 配置中有 JPA/DB 配置，也需要同步。

修复范围 E：文档同步
1. 更新：
   - `docs/reports/CURRENT-STATE-2026-04-28.md`
   - `docs/reports/TEST-MATRIX-本地与CI执行矩阵.md`
   - README
   - PLAN-24 如状态有变化
2. 文档必须写清：
   - migration 工具。
   - migration 文件位置。
   - 空库初始化命令。
   - 生产禁止 Hibernate 自动改表。
   - 现有数据库升级/备份要求。
3. 登记尚未解决的事项：
   - 复杂回滚策略。
   - 数据修复脚本。
   - 财务金额 BigDecimal 专项（如适用）。
   - 多服务并发 migration 风险（如适用）。

验收命令：
1. 默认门禁：
   - `cd backend && mvn test`
   - `cd frontend && npm test`
   - `cd frontend && npm run build`
2. 后端构建：
   - `cd backend && mvn -DskipTests package`
3. migration 验证：
   - 对 PostgreSQL 空库执行 migration，并记录命令与结果。
   - 如果使用 Docker Compose，记录具体 compose 命令。
4. 如修改 Docker：
   - `docker build backend -t bobbuy-backend-test`

交付要求：
1. 提供迁移方案摘要：
   - 选择 Flyway/Liquibase 的理由。
   - migration 文件列表。
   - 哪个 profile 执行 migration。
   - dev/prod/test 的 `ddl-auto` 策略。
2. 提供 schema 覆盖摘要：
   - 表数量。
   - 关键索引/唯一约束。
   - JSONB/枚举处理方式。
3. 提供验证结果：
   - 默认门禁。
   - 空库 migration。
   - 后端启动或构建。
4. 明确未完成但已登记的边界：
   - 旧库数据迁移。
   - 回滚策略。
   - 多模块服务迁移执行权。

禁止事项：
1. 不得继续让生产/试运行依赖 Hibernate `ddl-auto=update`。
2. 不得提交只适用于 H2、不适用于 PostgreSQL 的 migration。
3. 不得为了 migration 方便删除业务字段或放松认证/财务约束。
4. 不得把空库 migration 未验证写成已通过。
5. 不得把多个服务都默认配置成无协调地执行迁移。
6. 不得回滚非本任务相关的用户改动。

建议实现顺序：
1. 引入 Flyway 依赖与基础配置。
2. 从当前实体生成 PostgreSQL DDL 草稿，并手工审查约束/索引/JSONB。
3. 提交 `V1__baseline_schema.sql`。
4. 调整 dev/prod `ddl-auto` 策略。
5. 验证 PostgreSQL 空库 migration。
6. 跑默认门禁。
7. 更新文档与测试矩阵。
```

---

## 3. 审核重点

请重点审核以下问题：

1. 是否接受 Flyway 作为迁移工具。
2. dev profile 是否立即从 `ddl-auto=update` 改为 `validate`，还是保留短期兼容例外。
3. 默认 CI 是否要立刻加入 PostgreSQL migration 验证，还是先作为手动/专用门禁。
4. 当前是否需要为旧库升级写数据迁移，还是先完成空库基线。
5. 多模块服务中由哪个服务负责执行 migration。

---

## 4. 预期交付物

1. Flyway/Liquibase 依赖与配置。
2. PostgreSQL baseline migration。
3. dev/prod/test `ddl-auto` 策略收口。
4. 空库 migration 验证记录。
5. README / CURRENT STATE / TEST-MATRIX 文档同步。
6. 一份简短执行报告，包含 schema 覆盖、验证结果和后续风险登记。

---

## 5. 核准状态

**当前状态**: 待用户审查批准后执行。
