# AUDIT-01: 全栈架构与财务服务评审报告

**审计日期**: 2026-04-18
**审计人**: Antigravity AI
**状态**: 完成

---

## 1. 架构审计 (Architecture Audit)

### 1.1 容器化隔离与持久化
- **现状**：成功实现 Frontend (Nginx), Backend (Spring Boot), PG, MinIO, Redis, RabbitMQ 的全编排。
- **评价**：持久化卷 (`data/`) 配置正确，有效隔离了开发环境与数据层。
- **风险点**：`backend` 镜像构建过程中依赖 Maven 拉取组件，在离线环境下可能失败。建议后续引入多阶段构建（Multi-stage Build）并固化依赖层。

### 1.2 数据库连接稳定性
- **现状**：后端已成功通过 `HikariPool` 连接至容器内的 PostgreSQL。
- **改进意见**：当前 `spring.jpa.hibernate.ddl-auto` 设置为 `update`。生产环境上线前必须切为 `validate` 或使用 Flyway 进行迁移管理。

## 2. 核心服务审计：ProcurementHudService

### 2.1 算法连贯性 (FIFO 对账)
- **分析**：`reconcileInventory` 方法采用了 `SERIALIZABLE` 事务隔离级别并按 `createdAt` 排序，在技术上完全符合 FIFO（先下单先得）逻辑。
- **亮点**：对候选订单使用了 `FOR UPDATE`（通过 `findByStatusForUpdate`），有效防止了并发采购时的竞争条件。
- **缺陷**：
    - **物理指标真空**：服务中使用了硬编码的 `unit-weight: 1.0`。
    - **汇率静态化**：汇率仅通过配置文件读取，无法响应现场突发的汇率波动。

### 2.2 控制器集成 (AiAgentController)
- **修复记录**：修复了控制器中缺失 `ProcurementHudService` 注入的编译错误。
- **现状**：AI 扫描上架后，系统会自动调用 `reconcileInventory` 尝试核销订单。

## 3. 文档体系审计 (STD-02 Compliance)

- **线性一致性检查**：
    - [x] ARCH-01 至 ARCH-13 编号连续。
    - [x] MIGRATION-01 命名合规且存放于 `docs/migrations/`。
- **引用闭环**：README 中的 Docker 指南与 `docker-compose.yml` 配置同步。

## 4. 结论与建议

项目已具备进入“第二阶段升级”的条件。

### 4.1 近期风险
- `spring.jpa.open-in-view` 开启导致的性能风险。
- `UserDetailsService` 仍使用生成的随机密码。

### 4.2 下一阶段开发重点
1. **模型增强**：将重量 (Weight) 和体积 (Volume) 沉淀到 `Product` 模型。
2. **前端集成**：将 HUD 的 4 个 API 终端接入移动端仪表盘。
3. **汇率动态化**：增加汇率调整接口。

---
**核准状态**: 审计通过
