# PLAN-02-下一阶段开发提示词 (PROMPT H v2.0)

**生效日期**: 2024-04-18
**状态**: 待评审

---

## 1. 开发目标

在完成全栈容器化部署及 `ProcurementHudService` 初版的基础上，实现 **“实时采购决策中心”** 的闭环，包括物理指标建模、对账算法优化及前端仪表盘集成。

## 2. 核心 Prompt 文本 (请审查)

您可以直接将以下块中的内容作为下一次开发任务的输入：

```text
### [任务: 采购决策中心 HUD 深度集成]

任务背景：基于 ARCH-13 和 AUDIT-01，完善现有的财务服务并实现前端可视化。

1. 实体模型扩展 (Entity Update)
   - 在 Product 模型中增加 weight (Double, 单位 KG) 和 volume (Double, 单位 CBM) 字段，支持 JSONB 序列化。

2. 财务算法精度调整 (Service Hardening)
   - 更新 ProcurementHudService：
     - 将 recalculateCurrentLoad 的 unitWeight 逻辑从 1.0 默认值改为读取 Product 实体中的真实数据。
     - 优化汇率获取逻辑：如果 application.properties 中 current-rate 缺失，则默认使用 1.0 并记录日志警告。

3. 前端看板实现 (UI/UX Implementation)
   - 使用 React + Ant Design 新建 ProcurementDashboard 组件。
   - 实现以下看板指标 (HUD Metrics)：
     -【利润透视】：展示当前 Trip 下预计毛利及毛利率。
     -【容积红线】：使用进度条展示“已购重量 / 最大载重”。
     -【对账详情】：针对当前已分配的商品，显示所属订单的业务标识符 (businessId)。

4. AI 对账逻辑集成 (Onboarding Integration)
   - 确保 AiAgentController.confirmOnboard 成功调用 reconcileInventory 后的返回结果能实时更新到前端 UI，弹出通知告知“已成功分配至 订单 [ID]”。

5. 质量标准
   - 无物理 SQL：使用 JPA Repository 更新字段。
   - 文档闭环：更新 ARCH-12 以反映最新的商品物理模型。
   - 遵循 STD-06：完成开发后执行 Clean -> Start -> Test 的全量验证。
```

---

## 3. 关联待定业务逻辑 (Business Open Questions)

> [!IMPORTANT]
> - **汇率来源**：后续是否需要增加专门的操作界面供采购员在手机端手动更正“当日实际成交汇率”？
> - **分配算法**：当前的 FIFO 逻辑是全自动的，是否需要支持“保留部分库存”不进行自动分配？

---
**核准状态**: 待用户评审后执行
