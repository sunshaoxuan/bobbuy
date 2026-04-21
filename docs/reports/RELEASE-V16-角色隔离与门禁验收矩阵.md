# RELEASE-V16：角色隔离、全量响应式与链路门禁验收矩阵

## 1. 角色-菜单-页面-API 矩阵（当前可验收事实）

| 角色 | 菜单入口 | 页面 | 关键动作 | API | 权限 |
| :-- | :-- | :-- | :-- | :-- | :-- |
| CUSTOMER | Discover | `/` (`ClientHomeV2`) | 浏览商品、快捷下单、查看实况 | `GET /api/mobile/products` `POST /api/orders/{tripId}/quick-order` | 允许 |
| CUSTOMER | My Orders | `/client/orders` | 查看订单列表 | `GET /api/orders` | 允许 |
| CUSTOMER | Billing | `/client/billing` | 查看账单数据 | `GET /api/procurement/{tripId}/ledger` | 前端入口允许，后端受角色头控制 |
| CUSTOMER | Chat | `/client/chat` | 行程聊天 | `GET/POST /api/chat/*` | 允许 |
| AGENT | Dashboard/Trips/Orders | `/dashboard` `/trips` `/orders` | 运营与订单管理 | `GET/POST/PATCH /api/trips/*` `GET/PATCH /api/orders/*` | 允许（关键写接口受限 AGENT） |
| AGENT | Procurement/Picking/Stock | `/procurement` `/picking` `/stock-master` | 采买、复核、上架 | `GET/POST /api/procurement/*` `PATCH /api/mobile/products/{id}` | 仅 AGENT |
| AGENT | Users/Audit | `/users` `/audit/:tripId` | 参与者与审计 | `GET /api/users/*` `GET /api/financial/audit/*` `GET /api/audit-logs` | 仅 AGENT |

> 测试注入态：前端 `?role=...` 或 `localStorage.bobbuy_test_role`；后端 `X-BOBBUY-ROLE` 头注入角色。

## 2. 路由门禁矩阵（前端）

| 路由 | CUSTOMER | AGENT | 说明 |
| :-- | :--: | :--: | :-- |
| `/` | ✅ | ↪ `/dashboard` | 客户首页与采购员首页分流 |
| `/client/orders` `/client/billing` `/client/chat` | ✅ | ⛔ | 客户专属信息架构 |
| `/dashboard` `/trips` `/orders` `/order-desk` `/procurement` `/picking` `/stock-master` `/users` `/audit/:tripId` | ⛔ | ✅ | 采购后台专属 |

## 3. 全量响应式验收矩阵（390 / 768 / 1280）

> 口径说明：390/768/1280 为视口宽度验收目标；Phone 以 390x844 竖屏为基线，Tablet/PC 同时检查常见高度下首屏可操作性与滚动后可操作性。

| 页面 | Phone 390px | Tablet 768px | PC 1280px |
| :-- | :-- | :-- | :-- |
| Dashboard / Trips / Users / Orders / OrderDesk / ProcurementDashboard / PickingMaster / StockMaster / ZenAuditView | 关键操作需无横向溢出，表格退化为可读块/卡片或可滚动分区 | 筛选区与操作区不遮挡主体，表格列宽可读 | 信息密度充足，不出现过窄内容柱 |
| ClientHomeV2 / ClientOrders / ClientBilling / ClientChat | 卡片流、选择器与聊天输入可直接操作 | 主次信息并排但保持触控间距 | 留白与信息区平衡，账单/聊天区不空洞 |

## 4. AI 调用链完备性矩阵

| 链路 | 入口 | 关键接口 | 成功行为 | 失败恢复 |
| :-- | :-- | :-- | :-- | :-- |
| 文本解析 | 聊天或输入解析 | `POST /api/ai/parse` | 生成候选条目 | 返回错误提示并允许重试 |
| 翻译 | 多语言输入 | `POST /api/ai/translate` | 返回目标语文本 | 回退原文并提示失败 |
| 图片扫描 | Trip 聊天发图 | `POST /api/ai/onboard/scan` | 产出候选商品 | 扫描失败可重试扫描 |
| 候选确认/建品 | 图片候选确认 | `POST /api/ai/onboard/confirm` | 临时商品创建/更新并可发布 | 候选为空走人工新建；发布失败保留恢复动作 |
| 聊天闭环 | 客户/采购聊天 | `/api/chat/*` | 消息收发与轮询同步 | 拉取失败保留最近成功快照 |

## 5. 访问链路门禁结论（发布前）

1. 先验收角色隔离（菜单、路由、关键 API 三层同时通过）。
2. 再验收全量响应式（核心页面 390/768/1280）。
3. 再验收访问链路（角色→菜单→页面→动作→API）。
4. 再验收 AI 主链路（文本解析、图片扫描、候选确认、失败恢复）。
5. 四类门禁通过后，才执行全面自动化回归与试运行演练。
