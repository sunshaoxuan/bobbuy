# BOBBuy

BOBBuy 当前是一套以 **Spring Boot + React + PostgreSQL/MinIO + WebSocket(STOMP) 实时聊天** 为基础的代购协作系统。

## 当前真实能力
- **订单 / 行程管理**：行程、订单、批量状态流转、客户侧订单查询。
- **采购 HUD**：利润看板、额外支出、物流追踪、手工对账、客户账单汇总、财务审计流水、伙伴钱包。
- **参与者档案登记**：支持维护电话、邮箱、备注、默认地址与社交账号登记信息。
- **客户账单闭环**：客户可按行程查看 `businessId` 级账单、订单行详情、实采数量、差额说明，并完成“确认收货 / 确认账单”；`COMPLETED` / `SETTLED` 后确认动作自动只读。
- **线下收款与差额结转**：采购端可登记现金 / 转账 / 其他线下收款；当前余额、历史结转余额、本次应收/已收/待收统一仅统计进入结算语义的订单，排除 `NEW` / `CANCELLED` / 无效草稿单。
- **结算冻结治理**：行程进入 `COMPLETED` / `SETTLED` 后，后端与前端统一拦截客户确认、线下收款、小票重新识别/人工复核、拣货确认等会改变财务/履约/确认状态的动作，仅保留查询、导出、审计查看。
- **采购小票核销工作台 V1**：支持上传多张采购小票，保存原图/缩略图/上传时间/处理状态；优先调用真实 AI 识别小票内容，并在 AI 不可用时降级为规则回退结果；展示 AI / RULE_FALLBACK、置信度、复核状态，并保留人工核销审计。
- **账本精算修正**：历史余额排除取消单、未生效单、未来无效单；线下收款方式后端强校验为 `CASH / BANK_TRANSFER / OTHER`。
- **配送准备与地址清单**：采购 HUD 与客户端账单展示默认地址摘要；支持待配送客户列表与地址 / 经纬度 CSV 导出。
- **拣货确认闭环**：`/procurement` 与 `/picking` 共用 reviewed receipt + picking checklist 单一数据源，按 `businessId` 展示 `PENDING_DELIVERY` / `READY_FOR_DELIVERY`，保留 `SHORT_SHIPPED` / `ON_SITE_REPLENISHED` / `SELF_USE` 标签，并在冻结后统一只读。
- **聊天协作**：聊天已升级为 REST 持久化 + WebSocket(STOMP) 实时推送；客户侧聊天保持“订单上下文优先，Trip 次级筛选”。

## 当前未实现 / 不宣称
- 消息队列驱动的非聊天业务闭环
- 真实第三方支付网关
- 社交 OAuth 登录
- 真实地图路径规划 / 实时配送追踪
- 无人值守 AI 小票识别

## 技术栈
- **Backend**: Spring Boot 3 / Spring Security / Spring Data JPA
- **Frontend**: React 18 / Ant Design / Vite / Vitest / Playwright
- **Storage**: PostgreSQL 15 / MinIO

## 快速开始
```bash
docker-compose -p bobbuy up -d
```

- Frontend: http://localhost
- Backend API: http://localhost/api
- MinIO Console: http://localhost:9001

## 验收命令
- `cd frontend && npm run build`
- `cd frontend && npm test`
- `cd frontend && npm run e2e`
- `cd backend && ./mvnw test`
