# RELEASE-V16：角色隔离、全量响应式与链路门禁验收矩阵

## 1. 角色-菜单-页面-API 矩阵（当前可验收事实）

| 角色 | 菜单入口 | 页面 | 关键动作 | API | 权限 |
| :-- | :-- | :-- | :-- | :-- | :-- |
| CUSTOMER | Discover | `/` (`ClientHomeV2`) | 浏览商品、快捷下单、查看实况 | `GET /api/mobile/products` `POST /api/orders/{tripId}/quick-order` | 允许 |
| CUSTOMER | My Orders | `/client/orders` | 查看订单列表 | `GET /api/orders` | 允许 |
| CUSTOMER | Billing | `/client/billing` | 查看本人账单、确认收货、确认账单 | `GET /api/procurement/{tripId}/ledger` `POST /api/procurement/{tripId}/ledger/{businessId}/confirm` | 允许（仅本人账单） |
| CUSTOMER | Chat | `/client/chat` | 订单优先聊天 / Trip 次级筛选 | `GET/POST /api/chat/*` | 允许 |
| AGENT | Dashboard/Trips/Orders | `/dashboard` `/trips` `/orders` | 运营与订单管理 | `GET/POST/PATCH /api/trips/*` `GET/PATCH /api/orders/*` | 允许（冻结规则另行约束） |
| AGENT | Procurement/Picking/Stock | `/procurement` `/picking` `/stock-master` | 采买、复核、上架、小票核销 | `GET/POST/PATCH /api/procurement/*` `PATCH /api/mobile/products/{id}` | 仅 AGENT |
| AGENT | Users/Audit | `/users` `/audit/:tripId` | 参与者与审计 | `GET /api/users/*` `GET /api/financial/audit/*` | 仅 AGENT |

## 2. 当前边界
- 聊天仍为 REST + 轮询，不宣称 WebSocket。
- 结算仍为人工确认 / 线下结算，不宣称真实支付网关。
- 采购小票识别结果当前为受控 mock，不宣称真实 OCR。

## 3. 自动化口径
- `frontend npm run build`
- `frontend npm test`
- `frontend npm run e2e`
- `backend ./mvnw test`
