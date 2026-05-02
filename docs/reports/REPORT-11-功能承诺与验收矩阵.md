# REPORT-11: 功能承诺与验收矩阵

**日期**: 2026-05-02
**目标**: 对 README、CURRENT STATE、PLAN-24、Runbook、前端路由、后端 Controller 中承诺的功能进行证据对账。

---

## 1. 结论

当前文档承诺的内部试运行核心功能均已有至少一种验证入口。真实 AI/OCR sample、真实 `e2e:ai`、Compose 健康、mock 与真实栈双角色移动端黑盒均已形成证据；旧库 adoption 因确认无历史数据，改为空库上线与备份恢复验收。

仍需后续补强但不阻断内部试运行的项：dependency-check medium/low 风险处置、服务器部署窗口复跑真实栈黑盒 artifact、长期监控告警、OAuth/SSO、mTLS/service mesh、独立 schema/契约测试。

---

## 2. 功能矩阵

| 模块 | 文档承诺 | 当前状态 | 验证证据 | 结论 |
| :-- | :-- | :-- | :-- | :-- |
| 认证 / 会话 | JWT 登录、HttpOnly refresh cookie、CSRF、logout、角色绑定 | 已实现 | 后端 auth 测试、前端 authStorage/api/UserRoleContext 测试、真实栈黑盒登录 | 已验证 |
| 权限隔离 | customer 只访问本人数据；agent 访问采购管理；header auth 生产禁用 | 已实现 | 后端安全集成测试、Playwright 角色门禁、真实栈黑盒 | 已验证 |
| 客户发现页 | 商品浏览、快捷下单、移动端导航 | 已实现 | `mobile_customer_blackbox.spec.ts` mock + real stack | 已验证 |
| 客户订单 | 查看订单、状态反馈 | 已实现 | Playwright 客户路径、真实栈黑盒 | 已验证 |
| 客户账单 | 查看账单、确认收货/账单、冻结后只读 | 已实现 | 后端采购/账本测试、Playwright 客户路径、真实栈黑盒 | 已验证 |
| 客户聊天 | 打开聊天、发送文本、图片按钮可触达 | 已实现 | ChatWidget 单测、WebSocket 鉴权测试、真实栈黑盒 | 已验证 |
| Dashboard | 统计卡、行程与订单摘要 | 已实现 | Dashboard 单测、真实栈采购者黑盒 | 已验证 |
| 行程 / 订单管理 | trips、orders、状态流转 | 已实现 | 后端订单/行程测试、Playwright 采购者路径 | 已验证 |
| Procurement HUD | 利润、账本、费用、物流、审计、钱包 | 已实现 | ProcurementDashboard 测试、采购者黑盒 | 已验证 |
| 小票识别 / 核销 | 小票上传、AI/规则回退、reviewed receipt、trace | 已实现 | 后端小票/拣货测试、采购者黑盒入口 | 已验证 |
| 拣货 | reviewed receipt + picking checklist 单一事实源 | 已实现 | 后端 picking 测试、真实栈采购者黑盒 checkbox | 已验证 |
| 库存 / AI 上架 | OCR-first、LLM 结构化、人工确认、Product.attributes | 已实现 | 后端 AI 上架测试、真实 sample gate、`e2e:ai`、采购者黑盒入口 | 已验证 |
| 供应商 | 供应商规则、上架规则提示 | 已实现 | Suppliers 测试、采购者黑盒入口 | 已验证 |
| 用户档案 | 参与者档案与客户信息 | 已实现 | 前端 users 路径、采购者黑盒 | 已验证 |
| 审计 | 财务/操作审计视图 | 已实现 | 后端审计相关测试、采购者黑盒入口 | 已验证 |
| WebSocket | STOMP CONNECT JWT、customer 上下文授权、失败降级 | 已实现 | 后端 WebSocket 测试、前端 hook 测试、聊天黑盒 | 已验证 |
| AI provider | Ollama -> Codex Bridge -> Codex CLI fallback；服务器不依赖本机 CLI | 已实现 | `LlmGatewayTest`、真实 sample gate、ai-service 日志 | 已验证 |
| Compose 健康 | core/ai/im/auth/gateway/service + OCR health | 已实现 | REPORT-07、REPORT-10 Compose 健康记录 | 已验证 |
| Flyway 空库 | 空库 schema 初始化、validate | 已实现 | Flyway migration、REPORT-12 空库上线计划 | 已验证 / 需部署窗口复跑 |
| 备份恢复 | PostgreSQL / MinIO / Nacos 最小恢复演练 | 已有 Runbook | REPORT-12 记录当前空库口径与执行要求 | 需周期化 |

---

## 3. 文档口径调整

- “真实旧库 adoption / restore drill”不再作为当前内部试运行 blocker，原因是确认无历史生产数据；改为“空库上线 + 备份恢复演练”。
- `Codex Bridge` 允许作为个人内部试运行 AI provider，但 API key 必须通过外部 secret 注入，不得进入 git。
- 双角色移动端黑盒分为 mock 快速回归与真实栈验收：mock 不能替代真实栈，但两者都已通过本轮验证。

---

## 4. 后续跟踪

1. 在服务器试运行环境复跑真实栈双角色黑盒并上传 Playwright artifact。
2. 继续处置 dependency-check medium/low。
3. 形成周期化备份恢复演练记录。
4. 若未来导入历史数据，再重新启用旧库 adoption / baseline / restore drill 门禁。
