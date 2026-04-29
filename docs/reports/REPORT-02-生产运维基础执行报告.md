# REPORT-02: 生产运维基础执行报告

**日期**: 2026-04-29  
**关联计划**: [PLAN-33: P2 生产运维基础开发提示词](../plans/PLAN-33-P2-生产运维基础开发提示词.md)

---

## 1. 运维基础摘要

### 1.1 日志入口

- `docker compose logs -f gateway`
- `docker compose logs -f gateway-service`
- `docker compose logs -f core-service`
- `docker compose logs -f ai-service`
- `docker compose logs -f im-service`
- `docker compose logs -f auth-service`
- `docker compose logs -f postgres minio rabbitmq nacos redis ocr-service`

关键字段：

- HTTP：`method` / `path` / `status` / `cost` / `trace_id` / `user` / `role`
- AI / OCR：`provider` / `activeProvider` / `model` / `stage` / `latencyMs` / `errorCode` / `fallbackReason`

### 1.2 指标口径

- `/api/metrics`：用户 / 行程 / 订单数、订单状态、endpoint 请求次数、`p95` / `p99`、`4xx` / `5xx` 计数、登录失败次数、全局 `5xx` 比率
- AI / OCR、RabbitMQ、Redis、Nacos：当前仍以日志与人工巡检为主

### 1.3 告警规则

已落地到 [RUNBOOK-监控告警与故障处置](../runbooks/RUNBOOK-监控告警与故障处置.md)：

- 服务不可用
- 登录异常
- 订单 / 账单 / 钱包 5xx
- AI / OCR 降级
- PostgreSQL 异常
- MinIO 异常
- RabbitMQ 异常

### 1.4 备份恢复策略

已落地到 [RUNBOOK-备份恢复演练](../runbooks/RUNBOOK-备份恢复演练.md)：

- PostgreSQL：`pg_dump` 逻辑备份 + 新库恢复验证
- MinIO：`mc mirror` 或停机目录快照
- Nacos：`infra/nacos/config/*.yaml` + 运行态导出建议
- `.env`：仅私有安全存储，不提交真实 secret

### 1.5 故障处置清单

已覆盖：

- 服务启动失败
- core-service migration 失败
- JWT secret 缺失
- header auth 被误开
- PostgreSQL / MinIO / RabbitMQ / Nacos 不可用
- AI / OCR 不可用
- WebSocket 聊天不可用
- 前端空白页或 API 401 / 403

---

## 2. 修改清单

- 新增 `docs/runbooks/RUNBOOK-监控告警与故障处置.md`
- 新增 `docs/runbooks/RUNBOOK-备份恢复演练.md`
- 新增 `docs/reports/REPORT-02-生产运维基础执行报告.md`
- 小范围补充请求日志字段：增加 `role`
- 小范围补充 `/api/metrics`：增加请求计数、状态桶、`4xx` / `5xx` 计数、登录失败次数、全局 `5xx` 比率
- 同步 README、CURRENT STATE、试运行部署 Runbook、TEST-MATRIX、PLAN-24

---

## 3. 验证结果

- `cd /home/runner/work/bobbuy/bobbuy && docker compose config`：通过
- `cd /home/runner/work/bobbuy/bobbuy/backend && mvn test`：通过
- `cd /home/runner/work/bobbuy/bobbuy/frontend && npm test`：通过（依赖已先执行 `npm ci`）
- `cd /home/runner/work/bobbuy/bobbuy/frontend && npm run build`：通过

备份恢复演练：

- PostgreSQL / MinIO / Nacos 本地演练：已执行，详见下节

---

## 4. 备份恢复演练结果

**状态**: 已完成一次本地基础设施级演练

**环境**:

- 时间：2026-04-29
- 方式：`docker compose --env-file /tmp/plan33/.env up -d postgres minio`
- 目标：验证 PostgreSQL 逻辑备份 + 新库恢复、MinIO bucket 备份 + 独立 bucket 恢复、Nacos 配置归档

**实际命令摘要**:

```bash
cd /home/runner/work/bobbuy/bobbuy
docker compose --env-file /tmp/plan33/.env up -d postgres minio

cd backend
mvn -Dflyway.url=jdbc:postgresql://localhost:5432/bobbuy \
  -Dflyway.user=bobbuy \
  -Dflyway.password=plan33-password \
  flyway:migrate

docker compose --env-file /tmp/plan33/.env exec -T postgres pg_dump \
  -U bobbuy -d bobbuy --clean --if-exists --no-owner --no-privileges \
  > /tmp/plan33/backup/bobbuy-restore-drill.sql

cat /tmp/plan33/backup/bobbuy-restore-drill.sql | docker compose --env-file /tmp/plan33/.env exec -T postgres psql \
  -U bobbuy -d bobbuy_restore_verify

docker run --rm --network host -v /tmp/plan33/minio-source:/work \
  --entrypoint /bin/sh minio/mc -c '...mc mirror...'

tar -czf /tmp/plan33/backup/nacos-config-restore-drill.tgz \
  -C /home/runner/work/bobbuy/bobbuy infra/nacos/config
```

**结果**:

- PostgreSQL：恢复库 `bobbuy_restore_verify` 验证通过  
  - `bb_user=2`
  - `bb_order_header=1`
  - `bb_customer_payment_ledger=2`
- MinIO：`bobbuy-media-restore-verify/probe.txt` 恢复验证通过
- Nacos：`/tmp/plan33/backup/nacos-config-restore-drill.tgz` 归档成功

**本次未执行的验收项**:

- 未在该演练中拉起完整应用栈验证“服务可启动 / 登录可用 / 页面级图片预览”
- 原因：本次演练聚焦备份恢复链路本身，使用的是基础设施子集而非完整试运行编排
- 已提供完整应用验收命令与步骤，后续应在专门试运行窗口按 Runbook 做一次全链路恢复演练

---

## 5. 未完成边界

- 真实告警平台接入
- Prometheus / Grafana 或云监控
- 集中日志系统
- 自动化备份任务
- 定期恢复演练
- 服务级 SLO
