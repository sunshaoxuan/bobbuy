# PLAN-23-冻结门禁与履约视图统一

**生效日期**: 2026-04-24  
**状态**: ✅ 已实现

---

## 1. 目标

在账本精算、线下结算、待配送与拣货确认闭环基础上，补齐冻结后全面只读与拣货/配送单一事实来源，消除客户确认、余额汇总、拣货页面之间的规则分叉。

## 2. 本轮范围

1. **冻结门禁一致化**
   - `COMPLETED` / `SETTLED` 统一视为 settlement frozen
   - 客户确认、线下收款、小票上传/重识别/人工复核、拣货确认全部后端拒绝
   - ClientBilling / ProcurementDashboard / Picking 页面同步只读并展示冻结原因

2. **余额口径统一**
   - `customerBalanceSummary` 与 ledger carry-forward 统一过滤规则
   - 排除 `NEW`、`CANCELLED`、无效草稿单
   - 明确当前余额、历史结转余额、本次应收/已收/待收定义

3. **履约视图统一**
   - `/picking` 切换到 reviewed receipt + picking checklist API
   - `/procurement` 与 `/picking` 按 `PENDING_DELIVERY` / `READY_FOR_DELIVERY` 展示同一状态
   - 保留 `SHORT_SHIPPED` / `ON_SITE_REPLENISHED` / `SELF_USE` 标签

## 3. 约束边界

- 冻结门禁、账单与拣货继续沿用 REST 查询/提交；聊天已具备 WebSocket(STOMP)
- 不实现 OAuth、真实支付网关、地图服务
- 当前配送能力仍是准备、导出与状态流转，不含实时追踪

## 4. 验收口径

- 后端：`cd backend && ./mvnw test`
- 前端：`cd frontend && npm test`
- 前端：`cd frontend && npm run build`
- 前端：`cd frontend && npm run e2e`
