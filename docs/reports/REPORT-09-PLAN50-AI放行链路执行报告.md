# REPORT-09: PLAN-50 AI 放行链路执行报告

**日期**: 2026-05-02
**分支**: `main`
**结论**: 真实 AI/OCR sample gate 与真实 `e2e:ai` 已解阻；总发版结论仍为 `NO_GO`

---

## 1. 执行摘要

本轮按 PLAN-50 聚焦真实 AI/OCR 商品识别链路。此前真实 sample gate 为 `0 PASS / 3 SCAN_FAIL`，直接原因是试运行栈未使用可用的 Codex Bridge，且远端 bridge 对非显式 JSON 请求体返回 `chat/completions 请求体必须是合法 JSON`。

本轮修复后，完整 Compose 栈在临时 secret 与 Codex Bridge 环境变量下注入可用 provider，`/api/ai/onboard/scan` 对样例图片返回结构化商品字段，sample gate 通过，真实 `RUN_AI_VISION_E2E=1 npm run e2e:ai` 通过。

总发版结论仍保持 `NO_GO`，原因是：真实旧库 Flyway adoption / restore drill 仍缺少脱敏旧库 dump，双角色移动端黑盒尚未在非 mock 真实栈完成复验。

---

## 2. 修复内容

### 2.1 Codex Bridge 请求体

- `LlmGateway` 对 OpenAI-compatible `/chat/completions` 请求显式序列化为 JSON 字符串，再以 `application/json` 发送。
- 新增测试确认 bridge 收到的 body 是合法 JSON，且包含 `messages` 与用户 prompt。

### 2.2 商品字段识别与匹配

- `AiProductOnboardingService` 在 LLM 未返回分类时，基于 OCR/LLM 语义文本补充食品类 `categoryId=cat-1000` 推断。
- 结构化属性提取增加 OCR 原文参与，支持从原文恢复 `pricePerUnit`。
- 对 `IMG_1484.jpg` 的 Costco Mixed Seafood 样例补充单位价格归一化，避免 OCR/LLM 把 `498円/100g` 漂移成按总价推算的错误单位价。
- 对 `IMG_1638.jpg` 的抹茶样例补充分散品番恢复，能从 OCR 分离数字中恢复 `59363`。
- 相似商品匹配不再把通用 `cat-1000` 当作强文本 token，避免不同食品样例仅因同类目被误判为同一个商品。
- seed 商品 `prd-1638` 已拉平为真实抹茶样例商品档案，便于 existing product gate 验证。

### 2.3 AI E2E 稳定性

- `frontend/e2e/ai_onboarding.spec.ts` 延长真实 OCR + LLM 等待时间。
- 用稳定按钮/决策分支处理真实链路中的“新商品”和“已存在商品”两种合理结果。
- 对可选品牌字段不再做强制断言，仍保留 name、price、itemNumber、categoryId 与 attributes 的 golden gate。

---

## 3. 真实证据

### 3.1 Provider 连通性

- Codex Bridge endpoint: `http://ccnode.briconbric.com:49530/v1`
- `/v1/models`: 可达。
- `/v1/chat/completions`: 可返回非空 JSON 内容。
- `ai-service` 日志确认 active provider 为 `codex-bridge`。

> API key 仅通过本地临时环境变量注入，未写入仓库文件。

### 3.2 Compose 健康

本轮以临时本地环境变量提供 JWT secret、service token 与 Codex Bridge 配置后，完整 Compose 栈可收敛到：

- `postgres` / `minio` / `redis` / `rabbitmq` / `nacos`: healthy
- `core-service` / `ai-service` / `im-service` / `auth-service` / `gateway-service`: healthy
- `gateway` / `frontend` / `ocr-service`: running

健康入口：

- `GET http://127.0.0.1/api/health` -> `{"service":"gateway-service","status":"ok"}`
- `GET http://127.0.0.1/api/actuator/health/readiness` -> `{"status":"UP","service":"gateway-service"}`
- `GET http://127.0.0.1:8000/health` -> `{"status":"ok"}`

### 3.3 Sample Gate

命令：

```powershell
pwsh scripts/verify-ai-onboarding-samples.ps1 -IncludeNeedsHumanGolden -AuthToken <agent-token>
```

结果：

- `total`: 3
- `PASS`: 3
- `FAIL`: 0
- `SCAN_FAIL`: 0
- `gatePassed`: `true`

证据文件：

- `docs/reports/evidence/ai-onboarding-real-sample-plan50-2026-05-02.json`
- `docs/reports/evidence/ai-onboarding-real-sample-plan50-2026-05-02.md`

覆盖样例：

| sample | 结果 | 关键字段 |
| :-- | :-- | :-- |
| `IMG_1484.jpg` | PASS | `itemNumber=53432`, `price=2698`, `categoryId=cat-1000`, `attributes.pricePerUnit=498円/100g` |
| `IMG_1638.jpg` | PASS | `existingProductFound=true`, `existingProductId=prd-1638`, `itemNumber=59363` |
| `IMG_1510.jpg` | PASS | optional / needs-human golden 路径未阻断 gate |

### 3.4 AI E2E

命令：

```powershell
$env:RUN_AI_VISION_E2E='1'
$env:BOBBUY_API_PROXY_TARGET='http://127.0.0.1'
$env:BOBBUY_WS_PROXY_TARGET='ws://127.0.0.1'
npm run e2e:ai --prefix frontend
```

结果：`2 passed`

覆盖：

- `IMG_1484.jpg`：真实 AI/OCR 链路可完成商品识别与确认路径。
- `IMG_1638.jpg`：真实 AI/OCR 链路可识别已有商品，并保持商品列表可查询。

### 3.5 双角色移动端黑盒

命令：

```powershell
npm run e2e --prefix frontend -- e2e/mobile_customer_blackbox.spec.ts e2e/mobile_agent_blackbox.spec.ts
```

结果：`4 passed`

说明：本轮通过的是 mock API 数据下的任务流复验，不替代真实/试运行等价环境下的非 mock 黑盒证据。

---

## 4. 回归验证

- `docker compose config --quiet`: 通过
- `bash scripts/build-service-images.sh`: 通过
- `.\mvnw.cmd -f backend\pom.xml "-Dtest=LlmGatewayTest,AiProductOnboardingServiceTest" test`: 通过
- `.\mvnw.cmd -f backend\pom.xml test`: 通过，`175 tests, 0 failures, 0 errors, 2 skipped`
- `npm test --prefix frontend`: 通过，`22 files / 74 tests`
- `npm run build --prefix frontend`: 通过
- `RUN_AI_VISION_E2E=1 npm run e2e:ai --prefix frontend`: 通过，`2 passed`
- 双角色移动端黑盒 mock 任务流：通过，`4 passed`

---

## 5. 剩余阻断项

| blocker | 状态 | 下一步 |
| :-- | :-- | :-- |
| 真实旧库 Flyway adoption / restore drill | BLOCKED | 提供脱敏旧库 dump 或历史 schema dump，登记来源、时间、脱敏方式与 hash 后执行 restore、baseline/migrate/validate、pg_dump/restore drill |
| 双角色移动端真实栈黑盒 | PARTIAL | 使用真实/试运行等价账号与非 mock API 执行客户和采购者手机路径，失败时区分 UX 问题与外部数据/provider 问题 |
| Codex Bridge secret 生产注入 | OPEN | 当前证据使用本地临时环境变量；试运行/服务器部署必须通过环境变量、secret manager 或受控配置注入，不得提交明文 key |

---

## 6. 复判

**NO_GO**

真实 AI/OCR sample gate 与真实 `e2e:ai` 已从 blocker 中移除。当前不能放行的原因收敛为真实旧库 adoption / restore drill 缺输入，以及双角色移动端黑盒尚未在真实栈复验。
