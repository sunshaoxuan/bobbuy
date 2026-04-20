# 本地 / CI 测试执行矩阵

## 默认执行
- 前端构建：`cd /home/runner/work/bobbuy/bobbuy/frontend && npm run build`
- 前端单测：`cd /home/runner/work/bobbuy/bobbuy/frontend && npm test`
- 前端页面回归：`cd /home/runner/work/bobbuy/bobbuy/frontend && npm run e2e`
- 后端单元 / 集成测试：`cd /home/runner/work/bobbuy/bobbuy/backend && ./mvnw test`

## 环境门控
- `frontend/e2e/ai_onboarding.spec.ts` 依赖真实 AI 视觉链路，默认由 `RUN_AI_VISION_E2E` 控制，不纳入常规 CI。
- 聊天与商城发布页面级回归默认走 Playwright 路由桩，避免依赖外部模型与对象存储。

## 执行约束
- `npm run e2e` 已改为先检测 Playwright Chromium，缺失时才安装，避免每次测试重复下载。
- 轮询型页面默认保留最近一次成功快照；回归时需要同时验证刷新失败后的界面不清空。
- 后端测试已收敛启动与安全默认日志；CI 仅保留真实失败与必要告警。

## 已知事项
- `parallel_validation` 的 CodeQL 扫描若出现超时，需要在 PR 备注中记录为“已知事项”，避免形成未知风险。
