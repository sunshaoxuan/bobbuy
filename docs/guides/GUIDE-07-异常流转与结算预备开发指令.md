# GUIDE-07: 异常流转、容量释放与结算预备开发指令

## 1. 核心任务说明

在打通正向履行路径后，本阶段重点解决**异常流转（取消）**、**资源回收（释放容量）**及**结算自动化预备**。

### 1.1 订单取消与容量释放 (Resource Recovery)
- **业务规则**: 当订单从任意状态转为 `CANCELLED` (需新增此状态或利用现有逻辑) 时，系统必须**释放**该订单占用的行程容量。
- **后端实现**: 
  - `BobbuyStore` 需要实现 `releaseTripCapacity(tripId, quantity)`。
  - 在 `updateOrderStatus` 或 `bulkUpdateOrderStatus` 中，若目标状态为取消，则调用释放逻辑。
- **幂等性**: 释放操作必须幂等，防止重复释放导致容量溢出。

### 1.2 结算字段扩展 (Settlement Prep)
- **模型变更**: 
  - `OrderHeader` 增加 `paymentMethod` (枚举: `ALIPAY`, `WECHAT`, `CASH`)。
  - `OrderHeader` 增加 `paymentStatus` (枚举: `UNPAID`, `PAID`, `REFUNDED`)。
- **UI 适配**: 
  - 订单创建/编辑表单增加支付方式选择。
  - 订单列表展示支付状态。

### 1.3 采购清单导出 (Procurement Export)
- **功能描述**: 为了方便程序员/代购实地作业，需要一个生成当前行程“代采清单”的功能。
- **实现建议**:
  - 后端：`GET /api/trips/{tripId}/procurement-list`。
  - 返回按商品聚合后的列表（SKU, 名称, 总件数, 包含的订单 BusinessID）。

### 1.4 回归与加固
- **测试**: 补齐“取消订单后行程剩余容量恢复”的单元测试。
- **合规**: 所有状态流转必须继续记录审计日志。

## 2. 技术约束

- **状态机严谨性**: 需明确定义哪些状态可以转为取消（通常为 `NEW`, `CONFIRMED`）。
- **字段默认值**: 新增结算字段需有合理的默认值（如 `UNPAID`）。

## 3. 关联参考

- 实施方案：[PLAN-06](../plans/PLAN-06-%E8%AE%A2%E5%8D%95%E9%87%8D%E6%9E%84%E5%AE%9E%E6%96%BD%E6%96%B9%E6%A1%88.md)
- 历史记录：[walkthrough.md](../walkthrough.md)
