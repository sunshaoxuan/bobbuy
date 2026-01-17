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

### 4.1 安装浏览器依赖
```bash
cd frontend
npx playwright install
```

### 4.2 执行脚本
```bash
cd frontend
npm run e2e
```

**说明**:
- 测试文件：`frontend/e2e/test_shopping_flow.spec.ts`
- 该脚本模拟“发布行程 -> 确认订单 -> 状态流转 -> 审计校验”的关键链路。

---

## 5. 常见问题

1. **覆盖率未达标**  
   优先补充关键业务路径与异常路径用例。
2. **E2E 无法启动**  
   确认端口未被占用，必要时关闭已有前端服务或调整配置。
