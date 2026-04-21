# RELEASE-V15: 试运行前最后收口与真实 AI 验收计划

## 当前可承诺能力
- 前端基础回归链路：`npm run build` / `npm test` / `npm run e2e`
- 后端基础回归链路：`./mvnw test`
- 聊天商品闭环：图片确认 → 候选人工选择 → 临时商品创建 → 发布商城（含失败重试）
- 闭环审计字段：`operatorId`、`imageFlowStatus`、`candidateSelectionResult`、`candidateAudit`
- AI 视觉门控验收：`RUN_AI_VISION_E2E=1` 启用后执行 `frontend/e2e/ai_onboarding.spec.ts`

## 当前不承诺能力
- WebSocket 实时推送
- 消息队列异步补偿链路
- 名称级候选自动合并（仍要求人工确认）
- 向量检索与无人值守纠偏

## 已知风险与缓解
1. **CodeQL 超时**
   - 风险：发布窗口可能无法获得完整扫描结论。
   - 缓解：发布记录与 PR 备注中登记“已知风险”；后续独立任务继续追踪。
2. **AI 视觉仍为受控门槛能力**
   - 风险：依赖模型、样例文件与后端环境，不纳入常规 CI。
   - 缓解：通过环境变量门控并使用稳定验收标记（`ai-onboarding-stage` / `ai-onboarding-result-subtitle` / `ai-existing-product-alert`）。
3. **轮询架构边界**
   - 风险：不承诺秒级实时。
   - 缓解：保留最近成功快照、失败不清空界面、重试不丢上下文。

## AI 视觉最小执行说明（受控验收）
### 启用条件
- 后端可访问真实视觉模型与依赖服务
- 样例图片存在：`sample/IMG_1484.jpg`、`sample/IMG_1638.jpg`
- 执行命令：
  - `cd frontend`
  - `RUN_AI_VISION_E2E=1 npm run e2e -- ai_onboarding.spec.ts`

### 样例与预期
1. `IMG_1484.jpg`
   - 预期进入成功态标记：`data-ai-status="SUCCESS"`
   - 预期 `Item Number` 包含 `53432`
   - 预期可查看 `Price Tiers` 内容
2. `IMG_1638.jpg`
   - 预期出现既有商品提示标记：`data-testid="ai-existing-product-alert"`

### 判定标准
- 任一用例失败即视为 AI 视觉验收失败
- 成功时记录：样例文件、关键识别字段、候选行为结果
