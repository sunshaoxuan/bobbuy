# PLAN-50: 真实 AI/OCR 放行链路与真实栈复验提示词

**状态**: 已完成可执行部分
**日期**: 2026-05-02
**结果报告**: [REPORT-09-PLAN50-AI放行链路执行报告](../reports/REPORT-09-PLAN50-AI放行链路执行报告.md)

---

## 1. 目标

优先解阻真实 AI/OCR 商品识别链路，使 sample gate 从 `SCAN_FAIL` 变为字段级 PASS，再执行真实 `RUN_AI_VISION_E2E=1 npm run e2e:ai`。在此基础上继续追踪真实栈双角色移动端黑盒与旧库 adoption。

---

## 2. 执行要求

1. 先确认 Codex Bridge 或 Ollama-compatible endpoint 在容器内可达。
2. 修复 `/api/ai/onboard/scan` 的真实失败原因，不放宽 golden，不绕过 sample gate。
3. 保持 `IMG_1484.jpg`、`IMG_1638.jpg`、`IMG_1510.jpg` 字段级验证。
4. sample gate PASS 后再运行真实 `e2e:ai`。
5. 所有 secret 只能通过环境变量或外部 secret 注入，不得提交明文 key 或解密主密码。
6. 文档只写真实执行过的证据。

---

## 3. 本轮完成项

- Codex Bridge `/chat/completions` 请求体显式序列化为 JSON。
- sample 字段识别补齐食品类目推断、OCR 原文属性恢复、单位价格归一化、分散品番恢复与相似商品匹配降噪。
- seed 商品 `prd-1638` 拉平为真实抹茶样例商品档案。
- AI E2E 测试适配真实 OCR + LLM 延迟与新商品/已有商品两种合理分支。
- 真实 sample gate 通过：`3 PASS / 0 FAIL / 0 SCAN_FAIL`。
- 真实 `RUN_AI_VISION_E2E=1 npm run e2e:ai` 通过：`2 passed`。

---

## 4. 保留阻断项

1. 真实旧库 Flyway adoption / restore drill 仍缺少脱敏旧库 dump 或历史 schema dump。
2. 双角色移动端黑盒仍需在真实/试运行等价环境下用非 mock API 复验。
3. Codex Bridge key 的试运行/服务器注入流程必须落到外部 secret 管理，仓库内不得保存明文。

---

## 5. 后续入口

下一轮优先执行：

1. 获取脱敏旧库 dump，完成 restore、Flyway baseline/migrate/validate、pg_dump/restore drill。
2. 使用真实/试运行等价账号复验客户与采购者移动端黑盒任务流。
3. 将 PLAN-50 的 sample gate 与 `e2e:ai` 证据迁移到托管 workflow 或试运行窗口 artifact，确保 release candidate 可追溯。
