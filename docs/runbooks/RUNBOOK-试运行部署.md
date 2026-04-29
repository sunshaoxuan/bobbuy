# RUNBOOK: BOBBuy 试运行部署

**适用范围**: 内部 / 小范围试运行  
**不适用范围**: 高可用生产集群、TLS/域名证书、Secret Manager、监控告警自动化

---

## 1. 配置优先级

部署配置按以下优先级生效：

1. 容器环境变量（`docker compose` / `.env`）
2. Nacos 配置（`infra/nacos/config/*.yaml`）
3. `application-{profile}.properties`
4. `application.properties`

约束：

- 共享 / 服务器部署必须显式设置 `SPRING_PROFILES_ACTIVE=prod`（Compose 已内置）。
- `BOBBUY_SECURITY_JWT_SECRET` 必须在 `.env` 中显式填写。
- `BOBBUY_SECURITY_HEADER_AUTH_ENABLED` 默认且必须保持为 `false`。
- Compose 仅允许 `core-service` 执行 Flyway migration，其余服务固定 `SPRING_FLYWAY_ENABLED=false`。

---

## 2. 环境准备

最低准备项：

- Docker Engine + Docker Compose Plugin
- 至少一台可运行全部容器的 Linux 主机
- 可写磁盘目录：`data/postgres`、`data/minio`、`data/redis`、`data/rabbitmq`、`data/logs`
- 计划使用的 AI provider：
  - **推荐**：Ollama / 私有兼容 gateway
  - **可选**：受控桌面环境中的 Codex CLI
  - **禁止作为服务器默认方案**：依赖个人 Windows 桌面 Codex CLI 登录态

数据库版本策略：

- 试运行 Compose 固定使用 `postgres:15-alpine`。
- 原因：避免继续承受 PostgreSQL 18 与 Flyway 10.15.2 的“高于已验证范围”提示风险。
- 如后续要升级 PostgreSQL 大版本，需先单独验证 Flyway 兼容性并更新本文档。

---

## 3. 生成 `.env`

1. 复制模板：
   ```bash
   cd /home/runner/work/bobbuy/bobbuy
   cp .env.template .env
   ```
2. 至少修改以下项目后再部署：
   - `BOBBUY_SECURITY_JWT_SECRET`
   - `POSTGRES_PASSWORD`
   - `MINIO_ROOT_PASSWORD`
   - `RABBITMQ_DEFAULT_PASS`
3. 按实际环境选择 AI 路径：
   - **Ollama / 私有兼容 gateway**：填写 `BOBBUY_AI_LLM_MAIN_URL`
   - **视觉 / edge 模型**：填写 `BOBBUY_AI_LLM_EDGE_URL`
   - **OCR 容器同编排部署**：保持 `BOBBUY_OCR_URL=http://ocr-service:8000`
   - **本地桌面 Codex CLI 兜底**：仅在受控本机设置 `BOBBUY_AI_LLM_CODEX_COMMAND=codex`
4. 仅限本地调试可保留的默认项：
   - `POSTGRES_PASSWORD=bobbuypassword`
   - `MINIO_ROOT_PASSWORD=bobbuypassword`
   - `RABBITMQ_DEFAULT_PASS=bobbuypassword`
   - `BOBBUY_BIND_HOST=127.0.0.1`

---

## 4. 首次启动

首次启动前先校验配置：

```bash
cd /home/runner/work/bobbuy/bobbuy
docker compose config
```

首次启动：

```bash
cd /home/runner/work/bobbuy/bobbuy
docker compose up -d --build
```

观察关键服务：

```bash
docker compose ps
docker compose logs -f core-service ai-service gateway-service
```

Flyway 验证点：

- 仅 `core-service` 日志应出现 migration 执行。
- `ai-service` / `im-service` / `auth-service` 不应并发执行 Flyway。
- 如是旧库首次纳管，只有在确认 schema 与基线一致且已完成备份后，才允许临时设置 `BOBBUY_FLYWAY_BASELINE_ON_MIGRATE=true`。

---

## 5. 健康检查与入口

默认仅绑定到宿主机 `127.0.0.1`：

- 网关入口: `http://127.0.0.1:${GATEWAY_HOST_PORT:-80}`
- API 健康检查: `http://127.0.0.1:${GATEWAY_HOST_PORT:-80}/api/actuator/health`
- Core readiness: `http://127.0.0.1:${GATEWAY_HOST_PORT:-80}/api/actuator/health/readiness`
- Nacos Console: `http://127.0.0.1:${NACOS_HOST_PORT:-8848}/nacos`
- MinIO API: `http://127.0.0.1:${MINIO_API_HOST_PORT:-9000}`
- MinIO Console: `http://127.0.0.1:${MINIO_CONSOLE_HOST_PORT:-9001}`
- OCR Service: `http://127.0.0.1:${OCR_HOST_PORT:-8000}`

容器内健康检查重点：

- `postgres`: `pg_isready`
- `minio`: `/minio/health/live`
- `nacos`: `/nacos/v1/console/health/readiness`
- `core-service` / `ai-service` / `im-service` / `auth-service` / `gateway-service`: `/actuator/health/readiness`

---

## 6. 登录账号策略

- 试运行 / 服务器部署默认 `BOBBUY_SEED_ENABLED=false`。
- 不要把 demo seed credentials 当作共享环境长期账号。
- 如仅做本地演示并显式开启 seed，可使用：
  - `agent / agent-pass`
  - `customer / customer-pass`
- 共享环境应由受控初始化流程创建真实试运行账号。

---

## 7. AI provider 选择策略

推荐顺序：

1. **Ollama / 私有兼容 gateway**
   - 适用于服务器、容器、受控内网
   - 通过 `BOBBUY_AI_LLM_MAIN_URL` / `BOBBUY_AI_LLM_EDGE_URL` 指定
2. **Codex CLI 本地兜底**
   - 仅适用于本地桌面、受控登录态
   - 不能作为 Linux 服务器容器默认前提

运行时口径：

- `LlmGateway` 启动日志会输出 configured provider、active provider、Ollama URL、Codex command。
- AI / OCR 不可用时，系统允许回落到现有 fallback 路径；本任务不把 AI 不可用写成“已恢复”。

---

## 8. 日志与排障

常用命令：

```bash
cd /home/runner/work/bobbuy/bobbuy
docker compose logs -f core-service
docker compose logs -f ai-service
docker compose logs -f gateway-service
docker compose logs -f nacos
```

重点排查项：

1. **登录失败 / 401**
   - 确认 `BOBBUY_SECURITY_JWT_SECRET` 非空
   - 确认 `BOBBUY_SECURITY_HEADER_AUTH_ENABLED=false`
2. **服务起不来**
   - 先看 `docker compose ps`
   - 再看对应服务 readiness 日志
3. **Flyway 报错**
   - 确认只有 `core-service` 启用了 Flyway
   - 旧库先备份，再评估是否允许 `baseline-on-migrate`
4. **AI 无响应**
   - 检查 `BOBBUY_AI_LLM_MAIN_URL` / `BOBBUY_AI_LLM_EDGE_URL`
   - 服务器不要默认依赖 Codex CLI
5. **MinIO 上传失败**
   - 检查 `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD`
   - 检查 `BOBBUY_MINIO_BUCKET`

---

## 9. 回滚步骤

回滚前提：先确认最近一次可用镜像 / 代码版本、数据库备份、MinIO 备份、Nacos 配置快照。

建议顺序：

1. `docker compose down`
2. 恢复 `.env` 到上一个已验证版本
3. 恢复 PostgreSQL 备份
4. 恢复 MinIO 对象与 Nacos 配置
5. 重新启动：
   ```bash
   docker compose up -d --build
   ```
6. 重新执行健康检查

注意：

- 不要在未恢复数据库/对象存储备份的情况下只回滚应用镜像。
- 不要把未验证的 Compose 全量启动写成“已回滚成功”。

---

## 10. 备份提醒

部署前和升级前至少备份：

- PostgreSQL 数据库
- MinIO bucket 数据
- Nacos `infra/nacos/config/*.yaml` 对应配置快照
- 当前 `.env`

当前仍未完成的运维边界：

- 高可用 / 多副本
- TLS / 域名证书
- Secret Manager
- 监控 / 告警
- 自动化备份恢复演练
