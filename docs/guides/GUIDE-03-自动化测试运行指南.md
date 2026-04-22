# GUIDE-03: 自动化测试运行指南

**生效日期**: 2026-01-16  
**状态**: 执行中

---

## 1. 目的

统一前后端自动化测试的执行方式，确保覆盖率与验收门槛可重复验证。

---

## 2. 前端测试（Vitest）

### 2.1 运行单测
```bash
cd frontend
npm test
```

### 2.2 运行覆盖率
```bash
cd frontend
npm run test:coverage
```

**关注点**:
- `frontend/src/pages/Orders.tsx` 行覆盖率需 ≥ 60%。
- `frontend/src/pages/Trips.tsx` 行覆盖率建议 ≥ 60%。

---

## 3. 后端测试（JUnit + JaCoCo）

### 3.1 运行测试与校验
```bash
cd backend
mvn verify
```

### 3.2 覆盖率报告
- HTML: `backend/target/site/jacoco/index.html`
- CSV: `backend/target/site/jacoco/jacoco.csv`

**关注点**:
- `com.bobbuy.service.BobbuyStore` 行覆盖率需 ≥ 50%。
- `com.bobbuy.api.OrderController` 行覆盖率需 ≥ 50%。

---

## 4. E2E 测试（Playwright）

### 4.1 执行脚本
```bash
cd frontend
npm run e2e:prepare
npm run e2e
```

**说明**:
- `npm run e2e:prepare` 会先检测 Playwright Chromium 是否已存在，仅在缺失时安装。
- `npm run e2e` 默认串联 `e2e:prepare` 与 `playwright test`，适合本地与 CI 统一执行。
- 常规本地 E2E（`npm run e2e`）统一复用 `frontend/e2e/responsive_helpers.ts` 的共享 mock；其中 `**/api/**` 为 fallback mock，用于吸收未显式断言的 API，避免 Vite proxy `ECONNREFUSED` 噪声。

### 4.2 AI 专用回归（条件化）
`frontend/e2e/ai_onboarding.spec.ts` 不并入常规 `npm run e2e`，仅在专用环境执行。

```bash
cd frontend
npm run e2e:ai
```

Windows CMD:
```bat
cd frontend
set RUN_AI_VISION_E2E=1 && npx playwright test e2e/ai_onboarding.spec.ts
```

最小环境要求：
- 前端：`RUN_AI_VISION_E2E=1`
- 后端 profile：建议 `SPRING_PROFILES_ACTIVE=dev,ai-hermes`
- 视觉模型：`bobbuy.ai.llm.edge.url` / `bobbuy.ai.llm.edge.model` 可访问（默认 `llava`）
- 主模型：`bobbuy.ai.llm.main.url` / `bobbuy.ai.llm.main.model` 可访问（用于描述补全/兜底）
- 对象存储：MinIO 可访问（`bobbuy.minio.*`，默认 `http://localhost:9000`）
- 样本图片：`sample/IMG_1484.jpg`、`sample/IMG_1638.jpg`（相对仓库根目录）
- 预置数据：启用 `dev` profile 或显式 `bobbuy.seed.enabled=true`，确保可进入 `/stock-master` 并完成确认后写入

成功口径：
- `IMG_1484` 流程出现 `data-ai-status="SUCCESS"`，且 `Item Number` 含 `53432`
- `IMG_1638` 流程出现 `data-testid="ai-existing-product-alert"`

失败口径：
- 任一用例未达到上述稳定标记或流程超时，即视为 AI 专用回归失败

---

## 5. 常见问题

1. **覆盖率未达标**  
   优先补充关键业务路径与异常路径用例。
2. **E2E 无法启动**  
   确认端口未被占用，必要时关闭已有前端服务或调整配置。
3. **后端测试日志过多**  
   测试 profile 默认已关闭启动横幅、请求访问日志与常见 AI/FX 噪音；如仍需排查，可临时提升对应 logger 级别。
