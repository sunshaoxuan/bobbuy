# PLAN-17-合伙人钱包与财务结算闭环提示词 (PROMPT H V7.0)

**生效日期**: 2026-04-19
**状态**: ⏳ 待评审

---

## 1. 开发目标

实现从“数据看板”向“资金账户”的跨越。通过构建合伙人虚拟钱包体系，将行程中的预估利润正式转化为可分发的“已结利润”，完成财务链路的最后一块拼图。

## 2. 核心 Prompt 文本 (请审查)

您可以直接将以下块中的内容作为下一次开发任务的输入：

```text
### [任务: 合伙人虚拟钱包与行程终结结算系统]

任务背景：在 V5.0 的分润模型基础上，建立真实的资金流转闭环。

1. 合伙人钱包模型 (Partner Wallet Engine)
   - 数据库：新建 PartnerWallet (id, partner_id, balance, currency) 与 WalletTransaction (id, amount, type, trip_id, created_at) 表。
   - 逻辑：实现“预估 -> 实结”的资金冻结与释放机制。

2. 行程终结与分账逻辑 (Finalize & Payout)
   - 后端：在 ProcurementHudService 中增加 finalizeTripSettlement(tripId) 接口。
   - 校验：确保行程所有包裹均已 DELIVERED 或手动强制终结，计算最终的净利润。
   - 分账：根据定稿的分润比例，将分润金额插入 WalletTransaction 并更新 PartnerWallet.balance。

3. 商家 HUD 结算中心升级 (Merchant Wallet Dashboard)
   - UI 开发：在 HUD 中增加“财务结算”板块。
   - 功能：展示合伙人的“当前余额”、“本月已结”及“行程对账历史”。
   - 交互：增加 [终结行程并结算] 按钮，点击后触发全量分账并归档行程财务。

4. 数字化对账单增强 (Audit & Ledger)
   - 凭证：结算完成后，自动生成一份“合伙人分账明细表”PDF，记录每一笔费用的扣除与收益的分配证据。
   - 审计：金融级审计日志记录每一笔钱包余额变动。
```

---

## 3. 核心设计预期 (Financial Integrity)

> [!IMPORTANT]
> - **原子性**：终结结算必须是一个原子操作 (Transactional)，严禁出现“分账一半失败”导致的数据不一致。
> - **不可篡改性**：所有已生成的结算单据在终结后禁止二次修改。

---
**核准状态**: 待架构师初步起草，等待用户决策。
