# PLAN-06: 订单头行重构与幂等方案实施计划

**制定日期**: 2026-01-17  
**状态**: ⏳ 待执行  
**目标**: 将扁平订单模型重构为头行结构，引入业务幂等编号，支持同事件合并购买。

---

## 1. 核心任务分解

### 1.1 后端模型与存储重构 (Backend Core)
- [ ] **模型分拆**: 
  - 创建 `OrderHeader.java` (包含 `businessId`)。
  - 创建 `OrderLine.java`。
- [ ] **存储层适配**: 
  - 修改 `BobbuyStore.java`，将 `Map<Long, Order>` 替换为 `Map<String, OrderHeader>` (以 `businessId` 为键便于幂等查找)。
  - 在 `BobbuyStore` 实现 `upsertOrder` 核心算法（对齐 `ARCH-11` 的合并策略）。

### 1.2 API 契约与集成测试 (API & Testing)
- [ ] **Controller 升级**: 修改 `OrderController` 的 `POST` 接口，支持嵌套行数据提交。
- [ ] **集成测试核销**: 编写模拟“两次提交、相同 BusinessID、不同规格”的测试用例，确保幂等生效。

### 1.3 前端全量对齐 (Frontend Alignment)
- [ ] **API 类型定义**: 更新 `frontend/src/api.ts` 中的 `Order` 与 `Metrics` 类型。
- [ ] **业务逻辑补齐**: 在 `Orders.tsx` 中增加 `businessId` 的前置生成逻辑（如 `YYYYMMDD-SEQ`）。

---

## 2. 验收标准
- [ ] **业务幂等**: 发送两条相同 `businessId` 的请求，数据库中仅保留一条 Header，行数据根据 SKU/Spec 自动合并。
- [ ] **非标品隔离**: 相同产品但 `spec` 不同（如不同重量的肉）必须存为独立行。
- [ ] **回归测试**: 后端单元测试覆盖率必须维持在 90% 以上。

---

**负责人**: 开发团队  
**关联文档**: 
- 需求: [PROD-03-订单业务幂等与合并需求详细规格说明书](../requirements/PROD-03-%E8%AE%A2%E5%8D%95%E4%B8%9A%E5%8A%A1%E5%B9%82%E7%AD%89%E4%B8%8E%E5%90%88%E5%B9%B6%E9%9C%80%E6%B1%82%E8%AF%A6%E7%BB%86%E8%A7%84%E6%A0%BC%E8%AF%B4%E6%98%8E%E4%B9%A6.md)
- 设计: [ARCH-11-订单头行模型与业务幂等详细设计说明书](../architecture/ARCH-11-%E8%AE%A2%E5%8D%95%E5%A4%B4%E8%A1%8C%E6%A8%A1%E5%9E%8B%E4%B8%8E%E4%B8%9A%E5%8A%A1%E5%B9%82%E7%AD%89%E8%AF%A6%E7%BB%86%E8%AE%BE%E8%AE%A1%E8%AF%B4%E6%98%8E%E4%B9%A6.md)
