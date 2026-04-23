# BOBBuy

BOBBuy 当前是一套以 **Spring Boot + React + PostgreSQL/MinIO + REST 持久化 + 轮询** 为基础的代购协作系统。

## 当前真实能力
- **订单 / 行程管理**：行程、订单、批量状态流转、客户侧订单查询。
- **采购 HUD**：利润看板、额外支出、物流追踪、手工对账、客户账单汇总、财务审计流水、伙伴钱包。
- **参与者档案登记**：支持维护电话、邮箱、备注、默认地址与社交账号登记信息。
- **客户账单闭环**：客户可按行程查看 `businessId` 级账单、订单行详情、实采数量、差额说明，并完成“确认收货 / 确认账单”。
- **线下收款与差额结转**：采购端可登记现金 / 转账 / 其他线下收款；客户侧可查看已收、待收与余额结转说明。
- **结算冻结治理**：行程进入 `COMPLETED` / `SETTLED` 后，后端与前端统一拦截订单明细修改、删除、采购数量调整、手工对账转移。
- **采购小票核销工作台 V1**：支持上传多张采购小票，保存原图/缩略图/上传时间/处理状态；优先调用真实 AI 识别小票内容，并在 AI 不可用时降级为规则回退结果；展示 AI / RULE_FALLBACK、置信度、复核状态，并保留人工核销审计。
- **聊天协作**：聊天仍为 REST 持久化消息 + 15s 轮询；客户侧聊天已切换为“订单上下文优先，Trip 次级筛选”。

## 当前未实现 / 不宣称
- WebSocket 实时推送
- 消息队列驱动的聊天闭环
- 真实第三方支付网关
- 社交 OAuth 登录
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
