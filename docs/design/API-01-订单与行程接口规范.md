# API-01 订单与行程接口规范

## 1. 目标与范围
- 覆盖订单与行程的核心 API。
- 给出请求/响应示例、错误码与状态流转规则。

## 2. 通用约定

### 2.1 基础信息
- Base URL: `/api`
- Content-Type: `application/json`
- 多语言：通过 `Accept-Language` 指定语言（例如 `zh-CN` / `en-US`）。

### 2.2 统一响应结构

成功响应：
```json
{
  "status": "success",
  "data": { },
  "meta": { "total": 1 }
}
```

错误响应：
```json
{
  "status": "error",
  "errorCode": "RESOURCE_NOT_FOUND",
  "message": "订单不存在。"
}
```

### 2.3 错误码定义

| 错误码 | 说明 | HTTP 状态码 |
| --- | --- | --- |
| RESOURCE_NOT_FOUND | 资源不存在 | 404 |
| INVALID_REQUEST | 参数无效 | 400 |
| INVALID_STATUS | 状态流转非法 | 400 |
| CAPACITY_NOT_ENOUGH | 行程容量不足 | 409 |
| INTERNAL_ERROR | 系统异常 | 500 |

## 3. 订单接口

### 3.1 获取订单列表
- `GET /api/orders`

响应：
```json
{
  "status": "success",
  "data": [
    {
      "id": 3000,
      "customerId": 1001,
      "tripId": 2000,
      "itemName": "Matcha Kit",
      "quantity": 2,
      "unitPrice": 32.5,
      "serviceFee": 6.0,
      "estimatedTax": 2.3,
      "currency": "CNY",
      "status": "CONFIRMED",
      "statusUpdatedAt": "2026-01-15T10:12:00"
    }
  ],
  "meta": { "total": 1 }
}
```

### 3.2 创建订单
- `POST /api/orders`

请求：
```json
{
  "customerId": 1001,
  "tripId": 2000,
  "itemName": "Matcha Kit",
  "quantity": 2,
  "unitPrice": 32.5,
  "serviceFee": 6.0,
  "estimatedTax": 2.3,
  "currency": "CNY",
  "status": "NEW"
}
```

### 3.3 更新订单
- `PUT /api/orders/{id}`

### 3.4 更新订单状态
- `PATCH /api/orders/{id}/status`

请求：
```json
{
  "status": "CONFIRMED"
}
```

**状态流转规则**：
```
NEW -> CONFIRMED -> PURCHASED -> DELIVERED -> SETTLED
```

### 3.5 删除订单
- `DELETE /api/orders/{id}`

## 4. 行程接口

### 4.1 获取行程列表
- `GET /api/trips`

### 4.2 创建行程
- `POST /api/trips`

请求：
```json
{
  "agentId": 1000,
  "origin": "Tokyo",
  "destination": "Shanghai",
  "departDate": "2026-02-10",
  "capacity": 6,
  "reservedCapacity": 0,
  "status": "DRAFT"
}
```

### 4.3 更新行程
- `PUT /api/trips/{id}`

### 4.4 预留行程容量
- `POST /api/trips/{id}/reserve`

请求：
```json
{
  "quantity": 1
}
```

### 4.5 删除行程
- `DELETE /api/trips/{id}`

## 5. 附录：字段说明

### 5.1 订单状态
- `NEW`：新建
- `CONFIRMED`：已确认
- `PURCHASED`：已采购
- `DELIVERED`：已交付
- `SETTLED`：已结算

### 5.2 行程状态
- `DRAFT`：草稿
- `PUBLISHED`：已发布
- `IN_PROGRESS`：进行中
- `COMPLETED`：已完成
