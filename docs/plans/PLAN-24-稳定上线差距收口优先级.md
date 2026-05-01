# PLAN-24: 稳定上线差距收口优先级

**生效日期**: 2026-04-28
**状态**: 执行中
**目标版本**: 内部/小范围真实试运行上线
**事实基线**: [CURRENT-STATE-2026-04-28](../reports/CURRENT-STATE-2026-04-28.md)

---

## 1. 排序原则

本计划只处理“离稳定上线最近、风险最高、依赖最多”的事项。排序依据：

1. 是否阻断上线判断。
2. 是否影响数据安全、财务正确性或客户可见体验。
3. 是否是后续任务的前置条件。
4. 是否能用自动化验收固定下来。

---

## 2. 优先级总览

| 排名 | 优先级 | 任务 | 当前问题 | 上线影响 | 目标状态 |
| :-- | :-- | :-- | :-- | :-- | :-- |
| 1 | P0 | 真实 AI/OCR sample 与 E2E PASS | sample gate 已打到真实 `/api/ai/onboard/scan`，但当前 `0 PASS / 3 SCAN_FAIL`；本轮已修复 LLM 空响应兜底和 bridge 配置传递，剩余为注入可用 Codex Bridge key 或修复主 Ollama endpoint；`e2e:ai` 需在 sample PASS 后复跑 | 无法证明 sample 图片能正确填充商品档案字段 | 修复 provider/识别链路后，sample gate PASS，`RUN_AI_VISION_E2E=1 npm run e2e:ai` PASS 并归档 artifact |
| 2 | P0 | 真实旧库 adoption / restore drill | 仓库内仍无脱敏旧库副本 / 历史 schema dump | Flyway 上线缺少真实旧库证据 | 旧库 baseline / migrate / validate / restore drill 全流程留痕 |
| 3 | P1 | 处置 dependency-check medium/low | 最新 artifact 已为 `0 critical / 0 high / 13 medium / 2 low`；仍需按影响面登记 medium/low | 中期安全债务未完全收口 | medium/low 均有升级计划或正式风险登记 |
| 4 | P1 | 认证与权限生产化 | 已完成 JWT 登录、HttpOnly refresh cookie + CSRF、refresh token 会话治理、header auth 生产禁用、WebSocket 鉴权与最小 service token；mTLS、契约测试与 OAuth/SSO 仍未完成 | 不适合在缺少后续收口的情况下继续外扩或深拆服务 | JWT + cookie-based refresh + gateway/internal service token 稳定，剩余边界风险明确登记 |
| 5 | P1 | 数据库迁移治理 | 已补 Flyway 基线与空库验证，旧库 adoption/回滚策略仍未完全固化 | 数据结构升级仍需变更审查与备份流程 | Flyway 基线稳定运行，旧库升级手册补齐 |
| 6 | P1 | 部署与配置收口 | `.env`、Nacos、Compose、backend profile 有重复配置；Codex CLI 只适合本地 | 部署漂移与环境误判 | 试运行配置模板和生产禁用项明确 |
| 7 | P1 | AI/OCR 可靠性治理 | AI 上架/小票识别依赖多服务，fallback 多但观测不足 | 业务可用性和人工复核压力不稳定 | 可观测、可重试、可人工接管 |
| 8 | P2 | 微服务边界决策 | 单体 backend 与多模块服务壳并存 | 长期维护成本和部署边界不清 | 已通过 ADR 固定“主业务单体优先稳定”；后续拆分需满足额外门禁 |
| 9 | P2 | 生产运维基础 | 缺少完整日志、指标、告警、备份恢复文档和演练 | 故障不可控 | 最小运维手册与备份恢复演练 |
| 10 | P3 | 后续商业能力 | 支付、OAuth、地图路径规划仍未实现 | 不影响内部试运行，但影响商业化 | 独立里程碑推进 |

---

## 3. P0: 上线判断门禁

### 3.1 真实 AI/OCR sample 与 E2E PASS

**问题**

- Compose、gateway、OCR health 已通过，真实接口可达。
- `scripts/verify-ai-onboarding-samples.ps1` 带 token 已打到真实 `/api/ai/onboard/scan`，但当前结果为 `0 PASS / 3 SCAN_FAIL`。
- 本轮已修复主 LLM 空响应时不触发 fallback、OpenAI-compatible `message.content` 数组无法解析、Compose/Nacos 未传递 Codex Bridge 配置、服务器容器误选不可执行 Codex CLI 等问题。
- 最新真实 sample gate 仍失败，日志显示主 Ollama endpoint `http://ccnode.briconbric.com:22545/api/generate` 请求失败，且当前环境未配置 `BOBBUY_AI_LLM_CODEX_BRIDGE_URL/API_KEY`。
- `RUN_AI_VISION_E2E=1 npm run e2e:ai` 入口已可执行；在 sample gate PASS 前，不再把失败 e2e:ai 复跑当作放行证据。

**执行任务**

1. 给试运行环境注入可用 `BOBBUY_AI_LLM_CODEX_BRIDGE_URL` 与 API key，或修复 `BOBBUY_AI_LLM_MAIN_URL` 指向的 Ollama-compatible endpoint，使 `/api/ai/onboard/scan` 返回可解析结构化字段。
2. 用 `sample/IMG_1484.jpg`、`IMG_1638.jpg`、`IMG_1510.jpg` 复跑 sample gate。
3. 确认 `basePrice -> price`、`itemNumber`、`categoryId`、`attributes.pricePerUnit` 等关键字段与 golden 对齐。
4. 复跑 `RUN_AI_VISION_E2E=1 npm run e2e:ai` 并归档 Playwright artifact。

**验收标准**

- sample gate 返回 `gatePassed=true`。
- `e2e:ai` 全部通过。
- JSON/Markdown sample 报告与 Playwright screenshot/video/trace 可回看。

### 3.2 真实旧库 adoption / restore drill

**问题**

- 旧库基线、迁移与恢复仍缺真实输入。
- 空库 Flyway 验证不能替代真实旧库 adoption 证据。

**执行任务**

1. 获取脱敏旧库副本或历史 schema dump，并登记来源/时间/脱敏方式。
2. 在隔离 PostgreSQL 环境执行 baseline / migrate / validate / restore drill。
3. 把 `flyway_schema_history` 状态与恢复结果写入 `REPORT-07`。

**验收标准**

- 有可审计的旧库来源与恢复记录。
- adoption / restore drill 可复现。
- 若仍缺输入，发布结论必须继续保持 `NO_GO`。

### 3.3 dependency-check medium/low 风险登记

**问题**

- 最新 GitHub-hosted dependency-check artifact `6750657743` 已降至 `0 critical / 0 high / 13 medium / 2 low`。
- medium/low 不再按 P0 阻断当前真实证据收集，但仍需进入后续升级或豁免计划。

**执行任务**

1. 逐项登记 medium/low 的 dependency、CVE、scope、runtime exposure。
2. 能升级的补版本覆盖；不能升级的形成正式风险接受记录。
3. 保持后续 dependency-check artifact 可下载。

**验收标准**

- 无 critical/high 回归。
- medium/low 均有负责人和后续处理计划。

### 3.4 当前状态（2026-05-02 / PLAN-48）

- `backend mvn test`、`frontend npm ci && npm test`、`frontend npm run build` 与默认 Docker build 继续通过。
- `scripts/verify-ai-onboarding-samples.ps1` 的 gate/report-only 分流、`gatePassed` 汇总与失败非零退出码仍保持可用。
- `LlmGateway` 已补主 Ollama 空响应后的 Codex Bridge / Codex CLI fallback，OpenAI-compatible `message.content` 字符串/数组解析，以及不可执行 Codex CLI 不参与 provider 选择。
- Compose 与 Nacos `ai-service` 已补 `BOBBUY_AI_LLM_CODEX_BRIDGE_*` 与 `BOBBUY_AI_SECRET_MASTER_PASSWORD` 配置传递；仓库 `.env` 默认不再设置 `BOBBUY_AI_LLM_CODEX_COMMAND=codex`。
- `.github/workflows/codeql.yml` main run <https://github.com/sunshaoxuan/bobbuy/actions/runs/25217655038> 已成功。
- `.github/workflows/dependency-check.yml` main run <https://github.com/sunshaoxuan/bobbuy/actions/runs/25217516557> 已成功，artifact `dependency-check-report`（id `6750657743`）为 `0 critical / 0 high / 13 medium / 2 low`。
- `bash scripts/build-service-images.sh` 已通过；`Dockerfile.service` 复制宿主机构建好的 jar，Compose 不再受 Maven PKIX 阻塞。
- 本地临时 secret 下完整 Compose 栈已启动，gateway `/api/health`、`/api/actuator/health`、`/api/actuator/health/readiness` 与 OCR `/health` 均通过。
- `.github/workflows/ai-release-evidence.yml` 仍未形成真实 PASS run；本地真实 sample gate 已执行但失败，当前 blocker 已收敛到 AI provider 可用性：主 Ollama endpoint 请求失败且当前环境未注入 Codex Bridge key。
- 仓库工作区内仍未发现真实旧库副本 / 历史 schema dump，因此 adoption / restore drill 仍无可执行输入。
- 结论：默认门禁、安全 high、Compose/Nacos/OCR/gateway 基础健康已收口；当前剩余 blocker 为真实 AI/OCR sample gate、真实 `e2e:ai` PASS 证据与真实旧库 adoption。

---

## 4. P1: 试运行安全与数据可靠性

### 4.1 认证与权限生产化

**问题**

当前 `X-BOBBUY-ROLE` / `X-BOBBUY-USER` 适合本地和受控内网，不适合公网。

**执行任务**

1. 增加登录 API 与用户凭据模型，或接入独立 auth-service。
2. 使用 JWT/session 表达用户身份与角色，并把 WebSocket `/ws` 收口到 STOMP Bearer token。
3. 后端禁止公网请求直接信任角色 header；服务间 header 需由 gateway 签发或内网校验。
4. 补客户只能访问本人订单/账单/聊天上下文的集成测试。

**验收标准**

- 未登录访问受保护 API 返回 401。
- 非本人账单/订单访问返回 403/404。
- WebSocket 无 token / 伪造 token 被拒绝，且 AGENT/CUSTOMER 权限矩阵有自动化覆盖。

### 4.2 数据库迁移治理

**问题**

当前空库 schema 已改为 Flyway 基线 migration；2026-04-29 已补本地 PostgreSQL 15 `clean/migrate/validate` 与恢复演练，旧库 adoption、回滚与数据修复策略仍需继续收口到真实旧库副本演练。

**执行任务**

1. 固化 Flyway 基线 migration 与 `backend/src/main/resources/db/migration` 目录。
2. 保持 `backend` / `core-service` 为 migration 执行入口，避免多服务并发迁移。
3. 继续补旧库 adoption / 备份 / baseline-on-migrate 操作手册。
4. 继续评估回滚策略与数据修复脚本边界。

**验收标准**

- 空库可通过 migration 初始化。
- `core-service` / `backend` 不再依赖 Hibernate `ddl-auto=update`。
- 旧库升级流程、回滚与数据修复要求已在文档中明确风险边界。

### 4.3 部署与配置收口

**问题**

AI、MinIO、Nacos、Compose、profile 配置分散；Codex CLI fallback 对服务器部署有明确限制。
当前已补 `.env.template`、Compose、Nacos 默认值与试运行 Runbook，但 Secret Manager、高可用、TLS、监控告警仍未纳入本阶段。

**执行任务**

1. 整理 `.env.template`、Nacos config、`application-*.properties` 的配置优先级。
2. 明确服务器部署禁用 `codex-cli`，使用 Ollama/OpenAI API/私有 Codex gateway 三选一。
3. 写出试运行部署手册：启动、健康检查、回滚、日志路径。
4. 收敛默认密码与公网暴露端口。
5. 固定 Compose 仅由 `core-service` 执行 Flyway migration，并把 PostgreSQL 版本策略登记到文档。

**验收标准**

- 新机器按文档可完成一次干净部署。
- `docker compose config` 无关键变量缺失。
- AI provider 选择在日志中可见。
- README、CURRENT STATE、TEST-MATRIX、Runbook 与 `REPORT-04` / `REPORT-05` / `REPORT-06` 口径一致。

### 4.4 AI/OCR 可靠性治理

**问题**

AI 链路有 fallback，但缺少统一任务状态、重试、监控和人工队列。

**执行任务**

1. 给 AI 上架和小票识别统一记录 provider、模型、耗时、错误码、fallback 原因。
2. 区分“识别失败”“来源治理失败”“人工待复核”“可发布”。
3. 增加私有 Codex gateway 模式的配置文档；生产默认不依赖 Windows 桌面登录态。
4. 保证 AI 失败不破坏订单/账单主链路。

**验收标准**

- AI 服务不可用时，用户看到可操作状态而不是静默失败。
- 每次 AI 结果可追溯到 provider 与原始输入。
- 人工复核可以接管失败结果。

---

## 5. P2: 架构与运维长期稳定

### 5.1 微服务边界决策

当前 `backend` 是主业务单体，多模块服务更多是部署外壳。该路线已通过 [ADR-01-试运行阶段服务边界决策](../architecture/ADR-01-试运行阶段服务边界决策.md) 固定：

- **当前默认路线**：先保证单体业务与数据库稳定，多模块只作为部署适配层。
- **事实源边界**：`backend` 是源码主入口，`core-service` 是试运行部署中的核心业务事实源。
- **服务外壳**：`ai-service`、`im-service`、`auth-service` 继续复用共享代码与共享数据库，不宣称已完成独立业务拆分。
- **暂不建议**：在服务间鉴权、独立 schema、Tracing、SLO、独立 CI/CD 未完成前继续深拆微服务。
- **当前补充状态**：WebSocket 鉴权、HttpOnly refresh cookie + CSRF、refresh token 会话治理、最小 service token 与服务壳 smoke test 已完成；mTLS / service mesh、契约测试与 OAuth/SSO 继续登记为后续项。

**验收标准**

- 文档明确主开发入口和部署入口。
- 不再出现同一能力在单体和服务模块中双重漂移。
- 若未来要继续拆分，必须先补服务壳 smoke test、服务间鉴权、独立数据所有权与拆分后门禁。

**后续拆分条件**

1. 至少一个候选服务具备独立 schema 或独占表集。
2. 服务间鉴权与契约测试已经落地。
3. 已建立模块启动 smoke test 与拆分后 CI/CD。
4. 已具备基础 Tracing、服务级日志检索与 SLO。

### 5.2 生产运维基础

当前状态：

- 已补最小运维 Runbook（日志、健康检查、告警、故障处置、备份恢复）。
- 已补轻量 `/api/metrics` 与请求日志字段，支持 endpoint 请求次数、`4xx/5xx`、登录失败次数与全局 `5xx` 比率。
- 真实告警平台、集中日志、自动化备份与服务级 SLO 仍未完成。

**执行任务**

1. 日志：请求 trace、AI trace、审计 trace 统一检索。
2. 指标：健康检查、错误率、AI 成功率、WebSocket 连接数、DB 连接池。
3. 告警：服务不可用、AI fallback 激增、数据库失败、MinIO 上传失败。
4. 备份：PostgreSQL、MinIO、Nacos 配置备份与恢复演练。

**验收标准**

- 有最小运维 Runbook。
- 至少完成一次备份恢复演练记录。

---

## 6. 暂不作为上线阻塞

以下能力重要，但不阻断内部/小范围真实试运行：

1. 第三方在线支付。
2. 社交 OAuth / SSO。
3. 地图路径规划与实时配送追踪。
4. 无人值守 AI 自动改账。
5. 完整 SaaS 多租户计费与运营后台。

---

## 7. 推荐执行顺序

1. 修后端测试。
2. 修前端测试。
3. 对齐 CI 与 TEST-MATRIX。
4. 做认证替代方案。
5. 引入数据库迁移。
6. 收敛部署配置与 AI provider。
7. 加 AI/OCR 可观测与人工接管。
8. 写运维 Runbook。
9. 决定微服务边界长期路线。
10. 再排支付/OAuth/地图等商业化能力。
