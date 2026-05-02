# REPORT-08: 双角色移动端黑盒走查报告

**日期**: 2026-05-02
**范围**: 客户身份与采购者身份，手机视口 `390x844` / `360x800`
**结论**: mock 数据任务流已通过；真实试运行栈黑盒复验仍需在 AI provider 与旧库证据闭环后执行。

---

## 1. 走查目标

本轮不只验证页面能打开，而是按真实操作者路径检查手机端能否完成核心任务：

- 客户：登录、浏览商品、进入订单、确认账单/收货、进入聊天、发送文本、图片入口可触达。
- 采购者：登录、查看 dashboard、行程、订单、采购 HUD、小票工作台、拣货、库存、供应商、参与者管理。
- 每个关键页面都检查无横向滚动、移动端导航不会遮挡主操作、关键 CTA 在视口内可见可点。

---

## 2. 新增自动化

- `frontend/e2e/mobile_customer_blackbox.spec.ts`
- `frontend/e2e/mobile_agent_blackbox.spec.ts`
- `frontend/e2e/mobile_blackbox_helpers.ts`
- `frontend/e2e/responsive_helpers.ts`

测试默认使用真实前端交互与 mock API 数据，不依赖真实 AI/OCR provider。失败时沿用 Playwright 的 screenshot、trace、video artifact，便于直接复盘操作者卡点。

---

## 3. 本轮发现并修复的问题

| 优先级 | 问题 | 修复 |
| :-- | :-- | :-- |
| P0 | 客户发现页的 sticky `zen-home-nav` 在手机端层级高于应用头部，可能遮挡菜单按钮，导致操作者无法稳定打开导航。 | 移动端将 `zen-home-nav` 下移到头部下方并降低层级，同时提高 app header 层级。 |
| P1 | 手机端登录后 header 同时展示用户名、角色和语言/退出按钮，空间拥挤，影响导航可读性。 | 手机端已隐藏用户名/角色文本，仅保留必要操作。 |
| P1 | 库存页手机端点击 `Add New Product` 只新增空白卡片，不自动进入编辑表单，操作者难以继续填商品档案。 | 手机端新增商品后直接打开底部编辑抽屉。 |
| P2 | 供应商页缺少稳定测试锚点，黑盒任务流难以覆盖核心 CTA。 | 补充 `suppliers-title` 与 `suppliers-submit` 测试锚点。 |
| 测试夹具 | 采购页 `delivery-preparations` mock 缺失，fallback 对象会让表格崩溃，阻断黑盒测试进入真实交互。 | 补齐 delivery preparation 与 export mock，保持与接口契约一致。 |

---

## 4. 本轮验证结果

- `npm run e2e --prefix frontend -- e2e/mobile_agent_blackbox.spec.ts`
  - 结果：`2 passed`
  - 覆盖：`390x844`、`360x800`
- `npm run e2e --prefix frontend -- e2e/mobile_customer_blackbox.spec.ts`
  - 结果：`2 passed`
  - 覆盖：`390x844`、`360x800`

---

## 5. 放行状态

本轮证明了 mock 数据下的客户/采购者移动端核心路径可完成，并修复了走查中暴露的移动端 UX 问题。

但该证据不能替代真实试运行栈验收。`REPORT-07` 的最终结论继续保持 `NO_GO`，直到以下证据全部闭环：

1. 真实 AI/OCR sample gate PASS。
2. 真实 `RUN_AI_VISION_E2E=1 npm run e2e:ai` PASS。
3. 真实旧库 Flyway adoption / restore drill PASS。
4. 使用真实/试运行等价账号对本轮双角色移动端黑盒任务流完成复验。
