# REPORT-10: 真实栈双角色黑盒验收报告

**日期**: 2026-05-02
**范围**: 内部/小范围真实试运行前移动端可用性验收
**环境**: 本地完整 Compose 栈，真实后端 API，非 mock API

---

## 1. 结论

- **客户移动端黑盒**: PASS
- **采购者移动端黑盒**: PASS
- **覆盖视口**: `390x844`、`360x800`
- **P0/P1 可用性问题**: 本轮发现 1 个 P0，已修复并复测通过

本轮验证不使用前端 mock API。前端通过 Vite 代理访问本机 gateway，后端由 Compose 启动 `core-service`、`ai-service`、`im-service`、`auth-service`、`gateway-service`、`ocr-service` 与基础设施服务。

---

## 2. 执行证据

### 2.1 环境健康

- `docker compose config --quiet`: PASS
- `bash scripts/build-service-images.sh`: PASS
- Compose 服务状态:
  - `core-service`、`ai-service`、`im-service`、`auth-service`、`gateway-service`: healthy
  - `gateway`、`frontend`、`ocr-service`: running
- 健康检查:
  - `GET http://127.0.0.1/api/health`: PASS
  - `GET http://127.0.0.1/api/actuator/health`: PASS
  - `GET http://127.0.0.1/api/actuator/health/readiness`: PASS
  - `GET http://127.0.0.1:8000/health`: PASS

### 2.2 黑盒命令

```powershell
$env:RUN_REAL_MOBILE_BLACKBOX='1'
$env:BOBBUY_API_PROXY_TARGET='http://127.0.0.1'
$env:BOBBUY_WS_PROXY_TARGET='ws://127.0.0.1'
npx playwright test e2e/mobile_customer_blackbox.spec.ts e2e/mobile_agent_blackbox.spec.ts --workers=1
```

结果:

```text
4 passed
```

---

## 3. 客户路径

| 视口 | 登录 | 发现页 | 快捷下单 | 订单 | 账单确认 | 聊天文本/图片入口 | 横向溢出 |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| `390x844` | PASS | PASS | PASS | PASS | PASS | PASS | PASS |
| `360x800` | PASS | PASS | PASS | PASS | PASS | PASS | PASS |

客户路径覆盖：

- `/login` 真实账号登录。
- 商品发现页加载真实 `/api/mobile/products`。
- 快捷下单按钮可见可点，并有成功/失败反馈。
- 移动端导航可打开和关闭，不遮挡核心内容。
- 客户订单、账单、聊天入口可访问。
- 聊天输入、发送文本与图片上传按钮在手机视口内可触达。

---

## 4. 采购者路径

| 视口 | 登录 | Dashboard | Trips/Orders | Procurement HUD | AI 上架入口 | Stock/Suppliers | Picking | Users/Audit | 横向溢出 |
| :-- | :-- | :-- | :-- | :-- | :-- | :-- | :-- | :-- | :-- |
| `390x844` | PASS | PASS | PASS | PASS | PASS | PASS | PASS | PASS | PASS |
| `360x800` | PASS | PASS | PASS | PASS | PASS | PASS | PASS | PASS | PASS |

采购者路径覆盖：

- `/login` 真实账号登录。
- Dashboard、行程、订单、采购 HUD、库存、供应商、拣货、用户、审计入口均可通过移动端导航访问。
- AI 上架入口在手机端可触达。
- 拣货 checklist 使用 reviewed receipt + confirmed order 的真实数据源，checkbox 可操作。
- 密集页面在手机视口下无横向滚动阻断，关键 CTA 在视口内。

---

## 5. 发现并修复的问题

### P0: 客户首页越权请求采购者钱包导致登录态被清空

- **现象**: 客户真实登录后被重定向回 `/login`，移动端黑盒无法进入发现页。
- **根因**: `ClientHomeV2` 在客户视图中仍请求 `/api/procurement/wallets/PURCHASER`。真实后端返回 401 后，前端通用鉴权逻辑清理了客户 session。
- **修复**: 客户视图不再加载采购者钱包；钱包刷新只在非客户视图执行。
- **复测**: 真实栈客户与采购者移动端黑盒 `4 passed`。

### 测试稳定性修正

- `loginAsRole` 改为等待真实 `/api/auth/login` POST 响应、目标路由与页面主体可见，避免把登录过程误判为完成。
- 真实栈模式下黑盒测试不再注册 mock API route。
- 聊天消息断言改为取最后一条，避免历史消息与新消息文本重复导致 strict mode 冲突。

---

## 6. 剩余风险

- 本报告证明本地 Compose 真实栈与试运行等价账号下的移动端核心路径可完成；服务器试运行环境仍需在部署窗口复跑同一命令并归档 artifact。
- 当前黑盒任务流覆盖核心操作，不替代完整业务压力测试、长期监控、支付集成或公开 SaaS 级可访问性认证。
