# PLAN-14-下一阶段开发提示词 (PROMPT H V3.0)

**生效日期**: 2026-04-18
**状态**: 待评审

---

## 1. 开发目标

在 V2.0 实时看板的基础上，引入 **“动态财务闭环”**：处理波动的汇率、现场产生的杂项支出，并提供采购员手动维护对账关系的“最后一道防线”。

## 2. 核心 Prompt 文本 (请审查)

您可以直接将以下块中的内容作为下一次开发任务的输入：

```text
### [任务: 全球结算、动态汇率与支出管理]

任务背景：基于 ARCH-13 和 AUDIT-02，将采购财务系统升级为动态核算体系。

1. 动态汇率体系 (Dynamic FX Integration)
   - 后端扩展：新建 FxRateService，优先从环境变量读取，若缺失则通过 Brave Search 定向搜索“JPY to CNY exchange rate”。
   - 容错处理：获取失败时回退至系统硬编码的 1.0 比例并记录 Warning 日志。

2. 采购支出追踪 (Trip Expense Tracking)
   - 实体建模：新建 TripExpense 实体，包含 tripId, cost (Double), category (String, 如：袋子费、运费、停车费), createdAt。
   - 关联利润计算：更新 ProcurementHudService 中的利润计算逻辑：
     `Profit = (Expected Revenue * Reference Rate) - (Actual Cost * Current Rate) - Sum(TripExpenses)`。

3. 仪表盘增强 (Dashboard v2.0)
   - 交互功能：在 ProcurementDashboard.tsx 中增加“手动对账”弹窗，允许用户针对特定商品点击 [修正]，切换所属订单。
   - 支出展示：在仪表盘中增加“额外支出列表”卡片。
   - 导出能力：实现简易的 CSV/PDF 导出接口，支持导出单次 Trip 的结算汇总清单。

4. 体验与质量
   - 样式提升：仪表盘看板采用 Glassmorphism（玻璃拟态）视觉风格，并对关键指标（如毛利率）增加趋势箭头。
   - 验证：执行 STD-06 全流程验证，重点验证“多笔支出汇总后利润自动刷新”的准确性。
```

---

## 3. 待进一步明确的业务逻辑 (Open Questions)

> [!IMPORTANT]
> - **对账权限**：手动修正对账关系是仅限“管理员”操作，还是现场“采购员”也可直接操作？
> - **汇率更新频次**：是每次访问看板都重新获取实时汇率，还是每天凌晨同步一次？

---
**核准状态**: 待用户审查批准后执行
