# TC-API-001 订单与行程接口基础验证

## 用例目标
验证订单、行程接口的基本 CRUD 能力与错误码返回。

## 前置条件
- 后端已启动（`mvn spring-boot:run`）。
- 使用 `curl` 或 Postman 发送请求。

## 测试步骤
1. 请求 `GET /api/orders` 与 `GET /api/trips`。
2. 创建订单：`POST /api/orders`，填写必填字段。
3. 更新订单状态：`PATCH /api/orders/{id}/status`。
4. 创建行程：`POST /api/trips`，填写必填字段。
5. 预留行程容量：`POST /api/trips/{id}/reserve`。
6. 使用非法 ID 查询 `GET /api/orders/{id}` 与 `GET /api/trips/{id}`。
7. 发送 `Accept-Language: en-US` 验证错误提示语言。

## 预期结果
- 列表接口返回 `status=success` 且 `meta.total` 正确。
- 创建成功返回创建后的实体数据。
- 状态流转非法时返回 `INVALID_STATUS`。
- 容量不足时返回 `CAPACITY_NOT_ENOUGH`。
- 资源不存在时返回 `RESOURCE_NOT_FOUND`，错误信息随语言切换。

