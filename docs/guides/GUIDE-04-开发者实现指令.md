# GUIDE-04: 开发者实现指令 - 订单头行重构与业务幂等实现

请根据以下指令对现有的订单系统进行深度重构。**实现需严格遵守 ARCH-11 设计文档。**

## 1. 核心模型重构 (Backend Core)
- **拆分 Order**: 将 `com.bobbuy.model.Order` 拆分为 `OrderHeader` (头) 与 `OrderLine` (行)。
  - `OrderHeader` 必须包含 `businessId` (String), `customerId` (Long), `tripId` (Long), `status`, `totalAmount` 等。
  - `OrderLine` 包含 `skuId`, `spec`, `quantity`, `unitPrice` 等，并通过 `headerId` 关联。
- **存储适配 (`BobbuyStore.java`)**:
  - 内部存储改为 `Map<String, OrderHeader>` (Key = `businessId`)。
  - 创建 `upsertOrder` 方法：
    - 若 `businessId` 已存在，找到对应 Header，并根据 `skuId + spec` 匹配准则对行进行合并（Quantity 合计）。
    - 若 `businessId` 不存在，执行全量创建。
    - **合并细节**: 同一产品不同重量（`spec` 不同）必须视为独立行，严禁误合并。

## 2. API 契约升级
- 修改 `OrderController.java`:
  - `POST /api/orders` 的请求体需修改为 `OrderHeader` 结构（含 `List<OrderLine> lines`）。
  - 处理过程中需确保事务原子性。

## 3. 单元测试与验证
- 在 `BobbuyStoreTest.java` 增加专项测试：
  - 测试 1: 发送两个相同 `businessId` 且 `skuId+spec` 相同的行 -> 验证数量合并。
  - 测试 2: 发送两个相同 `businessId` 但 `spec` 不同（如重量不同）的行 -> 验证产生两条独立行。
  - 测试 3: 验证 GMV 计算逻辑适配新的头行结构。

## 4. 前端对齐
- 更新 `frontend/src/api.ts` 的类型定义。
- 修改 `Orders.tsx`: 提交订单前，逻辑上为当前请求赋予一个 `businessId` (建议格式 `YYYYMMDD-CUSTOMER-RANDOM`)。

---
**提示**: 完成后端改动后，请立即运行 `mvn verify` 确保 90/70 覆盖率铁律未被破坏。
