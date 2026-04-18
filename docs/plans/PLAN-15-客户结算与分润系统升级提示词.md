# PLAN-15-下一阶段开发提示词 (PROMPT H V5.0)

**生效日期**: 2026-04-18
**状态**: 待评审

---

## 1. 开发目标

在 V4.0 审计与多语种结算的基础上，进入 **“合伙人分润与实物追踪”** 阶段：解决利润分配的公平性，实现采购小票的照片化存档，并整合包裹物流状态。

## 2. 核心 Prompt 文本 (请审查)

您可以直接将以下块中的内容作为下一次开发任务的输入：

```text
### [任务: 合伙人分润、凭证存档与物流追踪]

任务背景：基于 V4.0 的审计基座，完善从利润分配到实物交付的全流程管理。

1. 合伙人分润引擎 (Profit Sharing)
   - 配置化：支持为行程设置合伙人角色（如：买手、BD）及百分比比例。
   - 算力集成：在 ProcurementHudService 中增加分润计算逻辑，公式为 `Remaining_Profit = Net_Profit * Share_Ratio`。
   - 看板展示：HUD 增加“合伙人应得利益”卡片，实时显示动态分润结果。

2. 支出凭证 MinIO 存档 (Receipt Storage)
   - 附件上传：扩展 /api/procurement/{tripId}/expenses 接口，支持上传图像文件。
   - 存储逻辑：文件存储于 MinIO `bobbuy` 桶的 `receipts/{tripId}/` 路径下，并与 TripExpense 记录关联。
   - UI 体验：在费用列表中增加 [查看凭证] 缩略图，点击后调用 MinIO 预签名 URL 预览。

3. 物流轨迹追踪 (Logistics Integration)
   - 模型扩展：Trip 实体增加 `trackingNumbers` (List<String>) 和 `logisticsStatus` (Enum) 字段。
   - 追踪逻辑：集成 Mock 物流查询接口（后续接入 17track），支持点击 [更新物流] 刷新包裹状态。
   - 联动结算：当物流状态为“DELIVERED”时，在客户结算中心显著提示“可发起全额核销”。

4. 验证与审计
   - 日志记录：所有分润比例的修改必须记录在 FinancialAuditLog 中。
   - 导出增强：结算 PDF 增加包含物流追踪信息的页脚。
```

---

## 3. 待进一步明确的业务逻辑 (Open Questions)

> [!IMPORTANT]
> - **分润扣除顺序**：合伙人分润是在扣除杂项支出 (TripExpenses) 之后计算，还是按总利润比例提取再扣除支出？
> - **物流接口**：初期是否仅需手动维护单号状态，还是必须立刻完成真实物流 API 的对接？

---
**核准状态**: 待用户审查批准后执行
