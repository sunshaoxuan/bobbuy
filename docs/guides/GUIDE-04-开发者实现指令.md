# GUIDE-04: 开发者实现指令 - 订单头行重构与业务幂等实现

请根据以下指令对现有的订单系统进行深度重构。**实现需严格遵守 ARCH-11 设计文档。**

## 1. 核心模型重构 (Backend Core)
- **实现依据**: 
  - 业务规则：[PROD-03](file:///c:/workspace/bobbuy/docs/design/PROD-03-%E8%AE%A2%E5%8D%95%E4%B8%9A%E5%8A%A1%E5%B9%82%E7%AD%89%E4%B8%8E%E5%90%88%E5%B9%B6%E9%9C%80%E6%B1%82%E8%AF%A6%E7%BB%86%E8%A7%84%E6%A0%BC%E8%AF%B4%E6%98%8E%E4%B9%A6.md)
  - 技术详设：[ARCH-11](file:///c:/workspace/bobbuy/docs/design/ARCH-11-%E8%AE%A2%E5%8D%95%E5%A4%B4%E8%A1%8C%E6%A8%A1%E5%9E%8B%E4%B8%8E%E4%B8%9A%E5%8A%A1%E5%B9%82%E7%AD%89%E8%AF%A6%E7%BB%86%E8%AE%BE%E8%AE%A1%E8%AF%B4%E6%98%8E%E4%B9%A6.md)
- **任务清单**:
  - 创建 `OrderHeader` 与 `OrderLine` 模型（依据 ARCH-11 第 2.1 节）。
  - 在 `BobbuyStore` 实现 `upsertOrder`（依据 ARCH-11 第 3.1 节伪代码）。
  - 更新 `OrderController` 以支持嵌套 JSON 提交。

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
