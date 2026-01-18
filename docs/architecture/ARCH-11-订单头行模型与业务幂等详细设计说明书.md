# ARCH-11: 订单头行模型与业务幂等详细设计说明书

**生效日期**: 2026-01-17  
**状态**: 待评审  
**引用**: [PROD-03-订单业务幂等与合并需求详细规格说明书](../requirements/PROD-03-订单业务幂等与合并需求详细规格说明书.md)

---

## 1. 总体设计 (High-Level Design)
本设计旨在通过物理层面的“头表 (Header)”与“行表 (Line)”分离，支撑 `PROD-03` 中定义的业务合并需求。系统采用“乐观锁 + 业务主键索引”实现并发环境下的幂等一致性。

### 1.1 层级化交互 (Hierarchical Interaction)
用户界面应遵循“行程-单头-单行”的深度展开模式：
1. **行程层 (Trip Level)**: 顶部固定展示当前行程的基本信息（目的地、出发日期、容量）。
2. **订单头层 (Order Header Level)**: 以客户为维度的卡片/磁贴列表。点击单头可展开该客户的明细。
3. **订单行层 (Order Line Level)**: 订单头折叠区内的商品明细列表。

## 2. 静态模型设计 (Data Structures)

### 2.1 对象模型 (Java Classes)
```java
// com.bobbuy.model.OrderHeader
public class OrderHeader {
    private Long id;                // 物理主键
    private String businessId;      // 业务幂等标识 (Event ID)
    private Long customerId;        // 客户 ID
    private Long tripId;            // 行程 ID
    private OrderStatus status;     // 订单状态
    private double totalAmount;     // 冗余汇总金额
    private List<OrderLine> lines;  // 嵌套行条目
}

// com.bobbuy.model.OrderLine
public class OrderLine {
    private Long id;                // 物理主键
    private Long headerId;          // 头关联 ID
    private String skuId;           // 商品标识
    private String spec;            // 规格区分 (用于非标品合并隔离)
    private int quantity;           // 数量
    private double unitPrice;       // 单价
}
```

### 2.2 逻辑存储结构 (Logical Schema)
| 表/数据集 | 核心索引 | 持久化策略 |
| :--- | :--- | :--- |
| `OrderHeader` | `Unique(businessId)` | 强一致性写入 |
| `OrderLine` | `Index(headerId, skuId, spec)` | 联合索引确保匹配效率 |

---

## 3. 核心算法逻辑 (Algorithms)

### 3.1 订单合并与幂等算法 (The Upsert Logic)
位于 `BobbuyStore.java` 的核心处理链路：

1. **头定位**: 使用 `Map.get(businessId)` 获取现有 Header。
2. **分支处理**:
   - **Case Header存在**:
     - 遍历入参的 `lines`。
     - 在现有 `header.getLines()` 中执行查找：`match = current.skuId == new.skuId && current.spec == new.spec`。
     - 若 `match` 命中：原有行 `quantity += new_quantity`。
     - 若 `match` 缺失：现有 Header 追加对应的新行。
   - **Case Header缺失**: 序列化新 Header 并挂载全量行。
3. **后处理**: 
   - 累加所有行 `quantity * unitPrice` 更新 `header.totalAmount`。
   - 触发 `AuditLogService` 记录状态变更。

### 3.2 并发控制方案 (Concurrency Control)
- **实现手段**: 使用 `synchronized` 关键字包装 `upsertOrder` 方法。
- **验证**: 单测需模拟高频并发请求（相同 `businessId`），断言最终 Header 唯一且 Line 数量准确。

---

## 4. 接口契约 (API Contracts)

### 4.1 提交接口升级
- **Endpoint**: `POST /api/orders`
- **Payload 变更**:
```json
{
  "businessId": "20260117001",
  "customerId": 1001,
  "tripId": 2000,
  "lines": [
    {"skuId": "P001", "spec": "500g", "quantity": 1, "unitPrice": 10.0}
  ]
}
```

---
**核准**: Architect  
**存档**: docs/architecture/ARCH-11-订单头行模型与业务幂等详细设计说明书.md
