# PLAN-49: P0 双角色移动端黑盒走查提示词

**状态**: 执行中
**目标**: 在发版前以真实操作者视角验证客户与采购者手机端核心任务可完成。
**关联报告**: [REPORT-08](../reports/REPORT-08-双角色移动端黑盒走查报告.md)

---

## 1. 任务边界

本计划新增一层发布前验收：不是只检查页面是否渲染，而是在 `390x844` 与 `360x800` 手机视口下，从登录开始完成客户和采购者的核心业务路径。

mock 数据黑盒用于快速发现 UX/交互问题；真实试运行栈黑盒用于形成最终放行证据。两者都不能替代真实 AI sample gate、真实 `e2e:ai` 与旧库 adoption。

---

## 2. 已落地内容

- 新增客户任务流：`frontend/e2e/mobile_customer_blackbox.spec.ts`
- 新增采购者任务流：`frontend/e2e/mobile_agent_blackbox.spec.ts`
- 新增移动端黑盒辅助：`frontend/e2e/mobile_blackbox_helpers.ts`
- 扩展共享 mock：`frontend/e2e/responsive_helpers.ts`
- 修复手机端 UX:
  - 客户发现页 sticky 导航不再遮挡 app header。
  - 手机 header 不再挤占用户名/角色文本。
  - 库存页手机端新增商品后直接进入编辑抽屉。
  - 供应商页补充稳定测试锚点。

---

## 3. 当前验收结果

- `npm run e2e --prefix frontend -- e2e/mobile_agent_blackbox.spec.ts`: `2 passed`
- `npm run e2e --prefix frontend -- e2e/mobile_customer_blackbox.spec.ts`: `2 passed`

覆盖视口：

- `390x844`
- `360x800`

---

## 4. 下一步真实栈复验 PROMPT

请在真实/试运行等价环境执行以下任务，并只记录真实执行过的证据：

1. 拉取最新代码并确认 `REPORT-08` 中的 mock 双角色黑盒已在本地通过。
2. 使用试运行等价 secret 启动完整 Compose 栈，确认 gateway、frontend、OCR、核心服务健康。
3. 使用真实或试运行等价客户账号执行客户手机黑盒任务流：
   - 登录。
   - 浏览商品。
   - 查看订单。
   - 确认账单/收货。
   - 进入聊天并发送文本，确认图片入口可触达。
   - 全程确认无横向滚动、关键 CTA 可见可点。
4. 使用真实或试运行等价采购者账号执行采购者手机黑盒任务流：
   - 登录 dashboard。
   - 进入行程、订单、采购、小票、拣货、库存、供应商、参与者管理。
   - 尝试新增商品、打开 AI 上架入口、勾选拣货、查看结算/配送准备入口。
   - 全程确认无横向滚动、关键 CTA 可见可点。
5. 若发现 P0/P1 手机端可用性问题，立即修复并复跑对应黑盒任务流。
6. 更新 `REPORT-07` 与 `REPORT-08`：
   - 写明账号类型、视口、执行命令、结果、artifact 路径或链接。
   - 若真实栈黑盒未执行，`REPORT-07` 必须继续保持 `NO_GO`。

---

## 5. 放行判定

mock 双角色黑盒已通过，但当前仍不是最终放行证据。

发版候选只有在以下项目全部完成后才可从 `NO_GO` 改为可复判：

1. 真实 AI/OCR sample gate PASS。
2. 真实 `RUN_AI_VISION_E2E=1 npm run e2e:ai` PASS。
3. 真实旧库 Flyway adoption / restore drill PASS。
4. 真实/试运行等价环境双角色移动端黑盒 PASS。
