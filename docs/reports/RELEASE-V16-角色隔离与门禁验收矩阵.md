# RELEASE-V16：角色隔离、全量响应式与链路门禁验收矩阵

## 1. 角色-菜单-页面-API 矩阵（当前可验收事实）

| 角色 | 菜单入口 | 页面 | 关键动作 | API | 权限 |
| :-- | :-- | :-- | :-- | :-- | :-- |
| CUSTOMER | Discover | `/` (`ClientHomeV2`) | 浏览商品、快捷下单、查看实况 | `GET /api/mobile/products` `POST /api/orders/{tripId}/quick-order` | 允许 |
| CUSTOMER | My Orders | `/client/orders` | 查看订单列表 | `GET /api/orders` | 允许 |
| CUSTOMER | Billing | `/client/billing` | 查看本人账单数据 | `GET /api/procurement/{tripId}/ledger` | 允许（仅白名单接口；账单按 customer/business 维度收敛） |
| CUSTOMER | Chat | `/client/chat` | 行程聊天 | `GET/POST /api/chat/*` | 允许 |
| AGENT | Dashboard/Trips/Orders | `/dashboard` `/trips` `/orders` | 运营与订单管理 | `GET/POST/PATCH /api/trips/*` `GET/PATCH /api/orders/*` | 允许（关键写接口受限 AGENT） |
| AGENT | Procurement/Picking/Stock | `/procurement` `/picking` `/stock-master` | 采买、复核、上架 | `GET/POST /api/procurement/*` `PATCH /api/mobile/products/{id}` | 仅 AGENT |
| AGENT | Users/Audit | `/users` `/audit/:tripId` | 参与者与审计 | `GET /api/users/*` `GET /api/financial/audit/*` `GET /api/audit-logs` | 仅 AGENT |

> 测试注入态：前端 `?role=...` 或 `localStorage.bobbuy_test_role`；后端 `X-BOBBUY-ROLE` 头注入角色。

## 2. 路由门禁矩阵（前端）

| 路由 | CUSTOMER | AGENT | 说明 |
| :-- | :--: | :--: | :-- |
| `/` | ✅ | ↪ `/dashboard` | 客户首页与采购员首页分流 |
| `/client/orders` `/client/billing` `/client/chat` | ✅ | ↪ `/dashboard` | 已由 `frontend/e2e/client_role_gate.spec.ts` 自动化验证 |
| `/dashboard` `/trips` `/orders` `/order-desk` `/procurement` `/picking` `/stock-master` `/users` `/audit/:tripId` | ⛔ | ✅ | 采购后台专属 |

## 3. 全量响应式验收矩阵（390 / 768 / 1280）

> 口径说明：390/768/1280 为视口宽度验收目标；Phone 以 390x844 竖屏为基线，Tablet/PC 同时检查常见高度下首屏可操作性与滚动后可操作性。

| 页面 | Phone 390px | Tablet 768px | PC 1280px |
| :-- | :-- | :-- | :-- |
| Dashboard / Trips / Users / Orders / OrderDesk / ProcurementDashboard / PickingMaster / StockMaster / ZenAuditView | 未纳入本轮自动化矩阵，需人工复核 | 未纳入本轮自动化矩阵，需人工复核 | 未纳入本轮自动化矩阵，需人工复核 |
| ClientHomeV2 / ClientOrders / ClientBilling / ClientChat | 门禁与首屏关键路径已自动化覆盖，响应式细节仍需人工复核 | 门禁与首屏关键路径已自动化覆盖，响应式细节仍需人工复核 | 门禁与首屏关键路径已自动化覆盖，响应式细节仍需人工复核 |

## 4. AI 调用链完备性矩阵

| 链路 | 入口 | 关键接口 | 成功行为 | 失败恢复 |
| :-- | :-- | :-- | :-- | :-- |
| 文本解析 | 聊天或输入解析 | `POST /api/ai/parse` | 生成候选条目 | 返回错误提示并允许重试 |
| 翻译 | 多语言输入 | `POST /api/ai/translate` | 返回目标语文本 | 回退原文并提示失败 |
| 图片扫描 | Trip 聊天发图 | `POST /api/ai/onboard/scan` | 产出候选商品 | 扫描失败可重试扫描 |
| 候选确认/建品 | 图片候选确认 | `POST /api/ai/onboard/confirm` | 临时商品创建/更新并可发布 | 候选为空走人工新建；发布失败保留恢复动作 |
| 聊天闭环 | 客户/采购聊天 | `/api/chat/*` | 消息收发与轮询同步 | 拉取失败保留最近成功快照 |

## 5. 访问链路门禁结论（发布前）

1. 本轮自动化实测：
   - `frontend npm run build` ✅
   - `frontend npm test` ✅
   - `frontend npm run e2e` ✅（4 passed / 2 skipped）
   - `backend ./mvnw test` ✅
2. 客户账单链路已打通：`/client/billing` + `GET /api/procurement/{tripId}/ledger`，并在 CUSTOMER 下做数据收敛。
3. 后端显式角色门禁已自动化覆盖：CUSTOMER 对 `/api/procurement/**`（非白名单）、`/api/financial/audit/**`、`/api/users/**`、`/api/orders/**` 的允许/拒绝矩阵可回归。
4. `ai_onboarding.spec.ts` 仍为条件化跳过（依赖专用 AI/文件环境），当前不计入常规本地门禁。
5. 发布前剩余风险：390/768/1280 全量响应式尚无自动化矩阵，需要补充半自动检查清单并固化结果。
