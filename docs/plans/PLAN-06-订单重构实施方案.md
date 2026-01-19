# PLAN-06: 订单头行重构与幂等方案实施计划

**制定日期**: 2026-01-17  
**状态**: ✅ 已完成  
**目标**: 将扁平订单模型重构为头行结构，引入业务幂等编号，支持同事件合并购买。

---

## 1. 核心任务分解

### 1.1 后端模型与存储重构 (Backend Core)
- [x] **模型分拆**: 
  - 创建 `OrderHeader.java` (包含 `businessId`)。
  - 创建 `OrderLine.java`。
- [x] **存储层适配**: 
  - 将 `Map<Long, Order>` 替换为 `Map<String, OrderHeader>` (以 `businessId` 为键便于幂等查找)。
  - 在 `BobbuyStore` 实现 `upsertOrder` 核心算法（对齐 `ARCH-11` 的合并策略）。

### 1.2 API 契约与集成测试 (API & Testing)
- [x] **Controller 升级**: 修改 `OrderController` 的 `POST` 接口，支持嵌套行数据提交。
- [x] **集成测试核销**: 编写模拟“两次提交、相同 BusinessID、不同规格”的测试用例，确保幂等生效。

### 1.3 前端 API 与类型对齐 (API & Type Alignment)
- [x] **API 类型定义**: 更新 `frontend/src/api.ts` 中的 `Order` 与 `Metrics` 类型。
- [x] **业务逻辑补齐**: 在 `Orders.tsx` 中增加 `businessId` 的前置生成逻辑。

### 1.4 层级化 UI 展现 (Hierarchical UI)
- [x] **行程概览组件**: 在页面顶部实现 `Trip` 信息的固定展示。
- [x] **订单头折叠列表**: 将订单列表重构为以 `businessId` 为键的折叠面板（Collapse）。
- [x] **订单行明细嵌入**: 在折叠面板内嵌套展示该订单下包含的所有 `lines`。

### 1.5 业务流程与状态联动 (Fulfillment & Linkage) - ✅ 已完成
- [x] **容量自动扣减**: 在 `upsertOrder` 或状态转为 `CONFIRMED` 时，自动调用 `reserveTripCapacity`。
- [x] **批量状态更新**: 扩展 `OrderController` (通过 `TripController`)，支持对特定 `tripId` 下的所有订单进行一键状态流转。
- [x] **前端批量操作 UI**: 在 Trip Dashboard 区域增加“批量确认”、“批量完成采购”等动作按钮。
- [x] **订单锁定逻辑**: 当订单状态为 `SETTLED` 时，UI 已按各组件逻辑点禁用编辑。

### 1.6 异常流转与结算预备 (Exception & Settlement) - ✅ 已完成
- [x] **状态联动**: 实现 `CANCELLED` 状态，自动释放行程预占容量。
- [x] **数据模型**: 订单头增加 `PaymentMethod`, `PaymentStatus` 字段。
- [x] **功能聚合**: 实现行程维度的采购清单导出逻辑 (`getProcurementList`)。
- [x] **UI 交互**: 订单详情展示支付状态，支持取消操作。

---

## 2. 验收标准
- [x] **业务幂等**: 发送两条相同 `businessId` 的请求，数据库中仅保留一条 Header，行数据根据 SKU/Spec 自动合并。
- [x] **非标品隔离**: 相同产品但 `spec` 不同（如不同重量的肉）必须存为独立行。
- [x] **回归测试**: 后端单元测试覆盖率达标 (Line 100%, Branch >90%)。

---

**负责人**: 开发团队  
**关联文档**: 
- 需求: [PROD-03-订单业务幂等与合并需求详细规格说明书](../requirements/PROD-03-订单业务幂等与合并需求详细规格说明书.md)
- 设计: [ARCH-11-订单头行模型与业务幂等详细设计说明书](../architecture/ARCH-11-订单头行模型与业务幂等详细设计说明书.md)
