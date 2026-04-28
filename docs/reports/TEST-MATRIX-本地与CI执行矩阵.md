# 本地 / CI 测试执行矩阵

> 2026-04-28 当前状态：前端生产构建与后端跳测编译通过；完整后端测试与前端单测门禁未恢复，详见
> [CURRENT-STATE-2026-04-28](CURRENT-STATE-2026-04-28.md)。

## 默认执行
- 前端构建：`cd /home/runner/work/bobbuy/bobbuy/frontend && npm run build`
- 前端单测：`cd /home/runner/work/bobbuy/bobbuy/frontend && npm test`
- 前端页面回归：`cd /home/runner/work/bobbuy/bobbuy/frontend && npm run e2e`
- 后端单元 / 集成测试：`cd /home/runner/work/bobbuy/bobbuy/backend && ./mvnw test`

## 环境门控
- `frontend/e2e/ai_onboarding.spec.ts` 依赖真实 AI 视觉链路，默认由 `RUN_AI_VISION_E2E` 控制，不纳入常规 CI。
- AI 专用回归入口：`cd /home/runner/work/bobbuy/bobbuy/frontend && npm run e2e:ai`（仅专用环境执行）。
- 聊天具备 WebSocket(STOMP) 推送；页面级回归仍可走 Playwright 路由桩，避免依赖外部模型与对象存储。
- AI 专用验收口径升级为业务结果断言：新建/命中已有商品、来源治理合规、确认后列表可查询且字段一致。
- 后端默认测试显式使用内存 H2，并将 AI 主模型 / OCR / 对象存储保持为 fake 或未配置状态，禁止默认 `mvn test` 外连真实 Ollama、Codex CLI、MinIO 与公网服务。

## 执行约束
- `npm run e2e` 已改为先检测 Playwright Chromium，缺失时才安装，避免每次测试重复下载。
- 轮询型页面默认保留最近一次成功快照；回归时需要同时验证刷新失败后的界面不清空。
- 后端测试基线已切回 OCR-first 口径；集成测试通过 seed 强制重置隔离共享数据，默认 `mvn test` 重新作为稳定门禁。

## 已知事项
- `parallel_validation` 的 CodeQL 扫描若出现超时，需要在 PR 备注中记录为“已知事项”，避免形成未知风险。

## 试运行前检查清单
- [ ] 前端 `npm run build` 通过，且 circular chunk 提示已显著下降或已有明确解释
- [ ] 前端 `npm test` 通过
- [ ] 前端 `npm run e2e` 通过
- [x] 后端 `./mvnw test` 通过；默认门禁使用 OCR-first mock/fake 配置与隔离测试数据
- [ ] 在 `RUN_AI_VISION_E2E=1` 下执行 `ai_onboarding.spec.ts`，验证新建与命中已有两条场景的真实闭环结果
- [ ] 聊天商品闭环可按 `trip / operator / publishStatus / candidateDecision` 检索与追溯
- [ ] CodeQL 若超时，已在发布说明登记风险与缓解动作
