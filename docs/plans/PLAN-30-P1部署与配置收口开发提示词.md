# PLAN-30: P1 部署与配置收口开发提示词

**生效日期**: 2026-04-29
**状态**: 待评审
**关联计划**: [PLAN-24: 稳定上线差距收口优先级](PLAN-24-稳定上线差距收口优先级.md)
**前置任务**:
- [PLAN-28: P1 认证与权限生产化开发提示词](PLAN-28-P1认证与权限生产化开发提示词.md)
- [PLAN-29: P1 数据库迁移治理开发提示词](PLAN-29-P1数据库迁移治理开发提示词.md)
**事实基线**: [CURRENT-STATE-2026-04-28](../reports/CURRENT-STATE-2026-04-28.md)

---

## 1. 开发目标

把当前分散在 `.env.template`、Spring properties、Docker Compose、Nacos 配置与 README 中的部署配置收口成一套可执行、可审计、适合内部/小范围试运行的部署方案。

当前事实：

1. 认证已切到 JWT，生产必须配置 `BOBBUY_SECURITY_JWT_SECRET`。
2. 数据库已引入 Flyway，生产不应依赖 Hibernate 自动改表。
3. AI provider 仍涉及 Ollama、Codex CLI、OCR service、edge model 等多种路径。
4. Compose 中存在多服务、Nacos、PostgreSQL、MinIO、Redis、RabbitMQ、OCR service 等配置，容易发生环境漂移。
5. Flyway 10.15.2 在 PostgreSQL 18 上可迁移，但会提示该 PostgreSQL 版本高于 Flyway 已测试支持范围，需要明确版本策略。

---

## 2. 核心 Prompt 文本（请审查）

您可以直接将以下内容作为下一次开发任务的输入：

```text
### [任务: P1 部署与配置收口]

任务背景：
BOBBuy 已完成测试门禁、CI 固化、JWT 认证生产化与 Flyway 数据库迁移治理。下一步需要把部署与配置收口：让一台新机器可以按文档启动试运行环境，让生产禁用项清楚可见，让 AI provider / JWT / Flyway / MinIO / Nacos / Compose 的配置不再互相漂移。

目标：
1. 梳理 `.env.template`、`application*.properties`、Docker Compose、Nacos 配置、README 中的配置项，形成唯一优先级说明。
2. 明确内部试运行部署路径：启动、健康检查、登录账号、日志、回滚、迁移、AI provider 选择。
3. 明确公网/服务器部署禁用项：
   - 禁止开启 header auth。
   - 禁止依赖 Windows 桌面 Codex CLI 登录态。
   - 禁止生产缺失 JWT secret。
   - 禁止依赖 Hibernate `ddl-auto=update`。
4. 收敛默认密码、端口暴露、数据库版本与 Flyway 兼容性风险。
5. 更新文档与配置，使新环境部署过程可复现。

必须先做的排查：
1. 阅读部署入口：
   - `docker-compose.yml`
   - `.env.template`
   - `README.md`
   - `backend/Dockerfile`
   - `frontend/Dockerfile`
2. 阅读后端配置：
   - `backend/src/main/resources/application.properties`
   - `backend/src/main/resources/application-dev.properties`
   - `backend/src/main/resources/application-prod.properties`
   - `backend/src/test/resources/application.properties`
3. 阅读 Nacos 配置：
   - `infra/nacos/config/*.yaml`
4. 阅读 AI/OCR 相关配置与代码：
   - `backend/src/main/java/com/bobbuy/service/LlmGateway.java`
   - `backend/src/main/java/com/bobbuy/service/ImageStorageService.java`
   - `backend/src/main/java/com/bobbuy/service/ProcurementReceiptRecognitionService.java`
5. 阅读当前测试矩阵：
   - `docs/reports/TEST-MATRIX-本地与CI执行矩阵.md`

修复范围 A：配置优先级与模板收口
1. 更新 `.env.template`，把试运行必须配置项分组：
   - PostgreSQL / Flyway
   - JWT / header auth
   - MinIO
   - Redis / RabbitMQ
   - Nacos
   - AI LLM / OCR
   - Frontend / Gateway ports
2. 每个关键项写明：
   - 默认值。
   - 是否适合生产。
   - 是否必须显式覆盖。
3. 明确 Spring 配置优先级：
   - 环境变量。
   - Nacos。
   - application profile。
   - 默认 application.properties。
4. 避免同一配置项出现多个名字表达同一含义；如无法立即改名，建立映射说明。

修复范围 B：Compose 与服务边界
1. 检查 `docker-compose.yml` 中所有服务：
   - postgres
   - minio
   - redis
   - rabbitmq
   - nacos
   - core-service / ai-service / im-service / auth-service / gateway-service
   - frontend / nginx gateway / ocr-service
2. 收敛公网暴露端口：
   - 内部依赖服务默认不应无理由暴露到宿主公网。
   - 如为本地开发保留端口，文档必须说明。
3. 明确哪个服务执行 Flyway migration：
   - 推荐短期由 core-service 或主 backend 执行。
   - 其他服务默认禁用 Flyway，避免并发迁移竞态。
4. 明确数据库版本策略：
   - 当前 PostgreSQL 18 可用但 Flyway 10.15.2 提示未测试支持。
   - 选择其一：升级 Flyway、下调 PostgreSQL 至 Flyway 支持版本、或登记为试运行风险并设置验证门禁。
5. 确认 backend Dockerfile 依赖预构建 jar 的流程在 README / CI 中说明一致。

修复范围 C：AI provider 与 Codex 边界
1. 明确 AI provider 支持路径：
   - Ollama
   - OpenAI API 或兼容 API（如当前支持）
   - Codex CLI 本地兜底
   - 私有 Codex gateway（如后续计划）
2. 文档必须写清：
   - Codex CLI 只适合本地 Windows/桌面登录态或私有受控网关。
   - Linux 服务器容器内不能默认假设 Codex CLI 可用。
   - 服务器部署推荐使用 Ollama/OpenAI API/私有 gateway，而不是暴露个人 Codex CLI。
3. 启动日志中应能看出当前 active provider。
4. AI provider 不可用时的用户可见状态与 fallback 策略需在文档中明确；本任务不要求重写 AI 业务逻辑。

修复范围 D：试运行部署手册
1. 新增或更新部署文档，例如：
   - `docs/runbooks/RUNBOOK-试运行部署.md`
2. 至少包含：
   - 环境准备。
   - `.env` 生成与必改项。
   - 首次启动。
   - Flyway migration 验证。
   - 健康检查 URL。
   - 登录账号策略。
   - 日志路径。
   - 常见故障排查。
   - 回滚步骤。
   - 数据库/MinIO/Nacos 配置备份提醒。
3. 明确哪些命令用于本地，哪些用于服务器。

修复范围 E：安全默认值
1. `.env.template` 中默认密码必须标注“必须修改”。
2. `BOBBUY_SECURITY_HEADER_AUTH_ENABLED` 默认 false。
3. `BOBBUY_SECURITY_JWT_SECRET` 不得给生产可用默认值。
4. MinIO 默认账号、PostgreSQL 默认密码、RabbitMQ 默认账号如果保留，必须标注仅限本地。
5. 生产部署不得启用 seed demo credentials；如保留 seed，需要明确风险。

修复范围 F：验证与文档同步
1. 更新：
   - README
   - CURRENT STATE
   - TEST-MATRIX
   - PLAN-24
   - `.env.template`
   - Docker/Nacos 配置说明
2. 验证：
   - `docker compose config`
   - `cd backend && mvn test`
   - `cd frontend && npm test`
   - `cd frontend && npm run build`
   - 如改 Docker：backend/frontend image build
3. 如果无法完整启动 Compose，需要说明原因，并至少保证 `docker compose config` 通过。

验收命令：
1. `docker compose config`
2. `cd backend && mvn test`
3. `cd frontend && npm test`
4. `cd frontend && npm run build`
5. 如改 Docker：
   - `cd backend && mvn -DskipTests package`
   - `docker build backend -t bobbuy-backend-test`
   - `docker build frontend -t bobbuy-frontend-test`

交付要求：
1. 提供配置收口摘要：
   - 必改项。
   - 本地默认项。
   - 生产禁用项。
   - AI provider 选择。
2. 提供修改清单：
   - 配置文件。
   - Compose/Nacos。
   - 文档。
   - 是否修改代码。
3. 提供验证结果。
4. 登记仍未完成的部署边界：
   - 高可用。
   - TLS/域名证书。
   - 备份恢复自动化。
   - Secret manager。
   - 监控告警。

禁止事项：
1. 不得把个人 Codex CLI 暴露为默认服务器部署方案。
2. 不得让生产默认开启 header auth。
3. 不得提供生产可用的硬编码 JWT secret。
4. 不得让多个服务无协调地执行 Flyway migration。
5. 不得把未验证的完整 Compose 启动写成已通过。
6. 不得回滚非本任务相关的用户改动。

建议实现顺序：
1. 先整理配置清单，找出重复、冲突、缺失项。
2. 调整 `.env.template` 与 Compose/Nacos。
3. 写试运行部署 Runbook。
4. 更新 README / CURRENT STATE / TEST-MATRIX / PLAN-24。
5. 跑 `docker compose config` 与默认门禁。
6. 如发现配置项命名不一致，优先文档映射，谨慎改代码。
```

---

## 3. 审核重点

请重点审核以下问题：

1. PostgreSQL 版本策略：继续 PostgreSQL 18、下调版本，还是升级 Flyway。
2. 是否允许本任务只做配置与文档收口，不重写 AI provider 代码。
3. 是否要求完整 `docker compose up` 验证，还是先以 `docker compose config` 加单服务构建为验收。
4. Flyway migration 执行权是否固定为 core-service。
5. 是否现在引入 secret manager，还是先保留 `.env` 试运行方案。

---

## 4. 预期交付物

1. 收口后的 `.env.template`、Compose、Nacos、Spring 配置说明。
2. 试运行部署 Runbook。
3. README / CURRENT STATE / TEST-MATRIX / PLAN-24 同步。
4. 验证记录。
5. 一份简短执行报告，包含必改配置、生产禁用项、AI provider 策略和剩余部署风险。

---

## 5. 核准状态

**当前状态**: 待用户审查批准后执行。
