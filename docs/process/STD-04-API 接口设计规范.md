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

## 4. 日志规范

- **日志级别**:
    - `INFO`: 重要的业务里程碑（如“订单已支付”）。
    - `WARN`: 非中断性问题或可重试错误。
    - `ERROR`: 功能性故障。
- **内容要求**: 必须包含上下文标识（如 RequestID, UserID）以及操作结果。

---
**统一的 API 交互标准是前后端协作的基础。**
