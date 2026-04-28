# RELEASE-V16：角色隔离、全量响应式与链路门禁验收矩阵

> 2026-04-28 更新：本文为历史验收矩阵。当前事实基线以
> [CURRENT-STATE-2026-04-28](CURRENT-STATE-2026-04-28.md) 为准。

## 1. 角色-菜单-页面-API 矩阵（当前可验收事实）

| 角色 | 菜单入口 | 页面 | 关键动作 | API | 权限 |
| :-- | :-- | :-- | :-- | :-- | :-- |
| CUSTOMER | Discover | `/` (`ClientHomeV2`) | 浏览商品、快捷下单、查看实况 | `GET /api/mobile/products` `POST /api/orders/{tripId}/quick-order` | 允许 |
| CUSTOMER | My Orders | `/client/orders` | 查看订单列表 | `GET /api/orders` | 允许 |
| CUSTOMER | Billing | `/client/billing` | 查看本人账单、确认收货、确认账单 | `GET /api/procurement/{tripId}/ledger` `POST /api/procurement/{tripId}/ledger/{businessId}/confirm` | 允许（仅本人账单） |
| CUSTOMER | Chat | `/client/chat` | 订单优先聊天 / Trip 次级筛选 | `GET/POST /api/chat/*` | 允许 |
| AGENT | Dashboard/Trips/Orders | `/dashboard` `/trips` `/orders` | 运营与订单管理 | `GET/POST/PATCH /api/trips/*` `GET/PATCH /api/orders/*` | 允许（冻结规则另行约束） |
| AGENT | Procurement/Picking/Stock/Suppliers | `/procurement` `/picking` `/stock-master` `/suppliers` | 采买、复核、上架、小票核销、供应商 AI 识别规则维护 | `GET/POST/PATCH /api/procurement/*` `PATCH/DELETE /api/mobile/products/{id}` `GET/POST/PUT /api/mobile/suppliers` | 仅 AGENT |
| AGENT | Users/Audit | `/users` `/audit/:tripId` | 参与者与审计 | `GET /api/users/*` `GET /api/financial/audit/*` | 仅 AGENT |

## 2. 当前边界
- 聊天已具备 REST 持久化 + WebSocket(STOMP) 推送；非聊天业务页仍以 REST 查询/刷新为主。
- 结算仍为人工确认 / 线下结算，不宣称真实支付网关。
- 采购小票识别优先使用真实 AI 结果；当 AI 服务不可达时自动降级为规则回退结果。
- 配送仍是“准备 / 导出 / 状态流转”，不宣称实时追踪、实时地图重规划。

## 3. 冻结与履约统一验收点

| 主题 | 当前可验收事实 |
| :-- | :-- |
| 冻结门禁 | Trip 进入 `COMPLETED` / `SETTLED` 后，客户确认、线下收款、小票重识别、人工复核保存、拣货确认全部后端拒绝，前端按钮同步只读并展示冻结原因。 |
| 余额口径 | `customerBalanceSummary`、`balanceBeforeCarryForward`、`balanceAfterCarryForward` 统一只统计进入结算语义的订单，排除 `NEW` / `CANCELLED` / 无效草稿单。 |
| 拣货视图 | `/procurement` 与 `/picking` 共用 reviewed receipt + picking checklist 单一事实来源，按 `READY_FOR_DELIVERY` / `PENDING_DELIVERY` 展示一致状态。 |
| 拣货标签 | `SHORT_SHIPPED` / `ON_SITE_REPLENISHED` / `SELF_USE` 在两页保持一致展示。 |

## 4. 自动化口径
- `frontend npm run build`
- `frontend npm test`
- `frontend npm run e2e`
- `backend ./mvnw test`
