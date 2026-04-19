# PLAN-18: 客户侧全链路闭环流程优化提示词 (PROMPT H V7.0)

**生效日期**: 2026-04-19
**状态**: ⏳ 待评审 (Aligning with Customer Journey Truth)

---

## 1. 目标描述 (Goal Description)

全面重塑 BOBBuy 的客户交互逻辑，从“以行程为中心”转向“以客户需求为中心”。本阶段将实现预订单、即时通讯、临时商品加单及采买后账单锁定等核心闭环功能，确保系统行为与真实业务场景 100% 对齐。

## 2. 差异化分析 (Gap Analysis)

> [!NOTE]
> 详细差异对比请参考 [GAP-02-客户侧全链路流程差异分析.md](file:///c:/workspace/bobbuy/docs/history/GAP-02-客户侧全链路流程差异分析.md)。

---

## 3. 拟议变更内容 (Proposed Implementation)

### 3.1 核心数据结构升级 (Data Model)
- **Product**: 引入 `isRecommended` (排序权重) 与 `isTemporary` (临时商品标识)。
## 4. 下阶段开发 PROMPT (V10.0)

您可以在批准本计划后，使用以下提示词驱动开发：

```text
### [任务: 客户侧全链路闭环流程优化与 V10.0 交付]

1. [Discover] 商品流分层排序与可见性控制 (Visibility & Sorting)
   - 在 Product 实体中增加：visibilityStatus (DRAFTER_ONLY, PUBLIC) 和 draftPublished (boolean)。
   - 临时商品逻辑：默认仅对创建者对采买人可见（DRAFTER_ONLY）。增加“一键发布到商场”功能，由采买人完善档案后公开。
   - 实现 AI 预审：在发布前根据产品号/名称自动提示合并重复的临时项。

2. [Fulfill] 弹性交付与采买批次规划 (Flexible Batching)
   - 解耦 OrderHeader 与 Trip。实现“采买批次规划”：采买人员从预订单池中手动勾选进入本次行程。
   - 穿透查询逻辑：在总清单视图中，点击总件数可下钻查看每个客户的购买数量。

3. [Collaborate] 实时采买社交闭环 (Photo to Order)
   - 引入 Chat 模块。实现 Purchaser 拍图 -> Buyer 点选 -> 自动生成私有临时商品并关联订单。

4. [Cash] 简化现金结算逻辑 (Simple Cash Settlement)
   - 在结算环节，采买人员仅需确认实付总额即可触发账单生成，暂不实现复杂的支付网关。

5. [Lock] 结算层不可篡改性 (Immutable Post-Procurement Lock)
   - 一旦 Trip 进入 COMPLETED 状态，API 强制拦截任何 OrderLine 的修改或删除请求。
```

## 5. 待确认问题 (已闭环)

> [!NOTE]
> - **临时商品**: 已确定入库流程（先私有，发布后公开）。
> - **穿透逻辑**: 已确定总表统计需支持看明细。
> - **结算方式**: 已确定简化为人为金额确认。

---
**核准**: 待用户最终评审。

---
**核准**: 待用户评审。
