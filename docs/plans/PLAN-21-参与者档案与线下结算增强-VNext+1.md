# PLAN-21-参与者档案与线下结算增强 VNext+1

**生效日期**: 2026-04-23  
**状态**: ✅ 已实现

---

## 1. 目标

在 VNext 基线之上补齐真实线下运营所需的参与者档案、地址、社交账号登记、线下收款、差额结转与小票人工复核追溯能力。

## 2. 本轮范围

1. **参与者档案**
   - `User` 新增 `phone`、`email`、`note`、`defaultAddress`、`socialAccounts`
   - 地址支持联系人、电话、国家/地区、城市、详细地址、邮编、可选经纬度
   - 社交账号仅登记展示，不承诺 OAuth / SSO

2. **线下收款与差额账户**
   - 新增客户线下收款账本
   - 支持采购端录入现金 / 转账 / 其他线下收款
   - 客户账单展示本次应收、已收、待收、结转前余额、结转后余额
   - 提供客户余额与客户账本历史查询

3. **小票复核追溯**
   - 识别结果补充 `confidence`、`reviewStatus`、`reviewedBy`、`reviewedAt`
   - 支持重新识别
   - 保存人工核销时保留 `rawRecognitionResult` 与 `manualReconciliationResult`
   - 审计日志记录原始结果、人工结果与差异

## 3. 约束边界

- 不实现 OAuth / SSO，仅做社交账号登记
- 不实现真实在线支付网关，仅做线下收款记录与余额结转
- 不引入 WebSocket / MQ，继续使用 REST + 轮询

## 4. 验收口径

- 后端：`cd backend && ./mvnw test`
- 前端：`cd frontend && npm test`
- 前端：`cd frontend && npm run build`
