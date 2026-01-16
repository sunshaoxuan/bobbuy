# STD-04: API 接口设计规范

**版本**: 1.0
**生效日期**: 2026-01-13
**状态**: 执行中

---

## 1. RESTful 原则

- **资源定位**: 使用名词复数形式表示资源（如 `/users`, `/orders`）。
- **动词映射**:
    - `GET`: 查询资源。
    - `POST`: 新增资源。
    - `PUT`: 完整更新资源。
    - `PATCH`: 部分修改资源。
    - `DELETE`: 移除资源。

## 2. 响应格式规范

所有接口响应应遵循一致的结构：

```pseudo
// 成功响应
{
    "status": "success",
    "data": { ... },
    "meta": { "total": 100 }
}

// 错误响应
{
    "status": "error",
    "error_code": "RESOURCE_NOT_FOUND",
    "message": "描述信息"
}
```

## 3. 异常处理

- **捕获原则**: 核心业务逻辑应包裹在异常处理块中。
- **用户友好**: 严禁直接向客户端暴露底层堆栈信息。
- **日志记录**: 所有的服务端异常必须被记录。

```pseudo
TRY
{
    ProcessRequest();
}
CATCH (KnownException ex)
{
    Return Response.Fail(ex.Code, ex.Message);
}
CATCH (UnexpectedException ex)
{
    Log.Error(ex);
    Return Response.InternalError("服务器繁忙，请稍后再试");
}
```

## 4. 日志规范 (Logging Standard)
- **架构原则**: 严禁在 Controller 层重复编写基础请求日志。必须通过全局拦截器（HandlerInterceptor）实现全量 API 的统一记录。
- **强制字段**:
    - `trace_id`: 全链路追踪标识。
    - `cost_ms`: 接口响应耗时。
    - `status`: HTTP 状态码。
    - `user`: 当前操作者（支持 anonymous）。
- **日志级别**:
    - `INFO`: 全量 API 的入参/出参记录。
    - `WARN`: 业务边界告警。
    - `ERROR`: 系统故障及未捕获异常。

---
**统一的 API 架构规范严禁任何“特例”实现。**
