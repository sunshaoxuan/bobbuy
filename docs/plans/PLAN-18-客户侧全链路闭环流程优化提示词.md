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
- **OrderHeader**: 引入 `desiredDeliveryWindow` (JSONB: {start, end})；重构 `tripId` 为可选，支持预订单状态。
- **ChatMessage**: [新增] 实体模型，支持文字、图片、商品链接及支付确认。

### 3.2 业务流程优化 (Workflow)
1. **智能排序流**: 商品列表实现 `Recommended -> Latest -> Regular` 的分层加载。
2. **预订单路由**: 购买环节若无匹配行程，系统自动转入“意向单”模式，保存客户的时效要求。
3. **采买实时协同**: 集成即时通讯接口。采买人员上传现场照片，AI 自动匹配现有商品或创建“临时商品”。
4. **结算保护机制**: 采买标记为 `FINISHED` 后，后端通过 AOP 或 Service 层校验拦截任何 `OrderLine` 的删除/修改请求。

---

## 4. 下阶段开发 PROMPT (V10.0)

您可以在批准本计划后，使用以下提示词驱动开发：

```text
### [任务: 客户侧全链路闭环流程优化与 V10.0 交付]

任务背景：基于“客户真实使用过程”描述，实现从需求发现到最终对账的全链路闭环。

1. [Discover] 商品流分层排序 (Sorted Product Discovery)
   - 在 Product 实体中增加权重标识。
   - 实现 ProductService 的分层检索：推荐位优先，其次按创建/更新时间倒序。

2. [Fulfill] 弹性交付与预订单系统 (Flexible Pre-Order)
   - 解耦 OrderHeader 与 Trip 的强制绑定。
   - 首页/详情页购买流：若 TripList 为空，显示“设定期望收货期”，生成状态为 PRE_ORDER 的订单单头。

3. [Collaborate] 实时采买社交闭环 (Live Purchaser-Buyer Chat)
   - 引入 Chat 组件库。实现基于照片的“快捷加单”：采买人员发图 -> 买家点选 -> 生成带图片描述的临时商品 OrderLine。
   - 临时商品逻辑：支持在非标准库中创建仅限本次行程的动态商品项。

4. [AI Sync] 采买端实时动态总表 (AI Consolidated Shopping List)
   - 优化 ProcurementHUD，增加“AI 合理化汇总”视图。将所有 Pre-orders 与 Linked Orders 的需求实时聚合，计算单品总采购量。

5. [Lock] 结算层不可篡改性 (Immutable Post-Procurement Lock)
   - 状态流转控制：一旦 Trip 状态进入 COMPLETED/SETTLED，API 应返回 403 拦截任何 OrderLine 的物理或逻辑删除。
   - 最终账单页：增强 ZenAuditView，使其作为客户收货付款时的最终财务凭证。
```

## 5. 待确认问题 (Open Questions)

> [!CAUTION]
> 1. **临时商品归属**: 采买人员现场拍图生成的“临时商品”是否应自动注入该类别的标准库，还是仅作为该订单的私有数据？
> 2. **AI 总表逻辑**: 如果有 10 个客户下单了同一款商品，但在不同的预订单（不同收货期）中，采购人员是否应看到一个总数，还是按收货意向分拆？
> 3. **现金结算确认**: “收货付款”环节是否需要买卖双方在 App 上进行数字签名或扫码确认？

---
**核准**: 待用户评审。
