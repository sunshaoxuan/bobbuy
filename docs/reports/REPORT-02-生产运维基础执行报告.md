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
- `cd /home/runner/work/bobbuy/bobbuy/frontend && npm ci && npm test`：通过
- `cd /home/runner/work/bobbuy/bobbuy/frontend && npm run build`：通过

备份恢复演练：

- PostgreSQL / MinIO / Nacos 本地演练：见本报告后续更新结果

---

## 4. 备份恢复演练结果

- **状态**: 待执行 / 待补充
- **说明**: 本任务要求先提供可执行 Runbook；若本地容器环境允许，再补本地演练结果与命令。

---

## 5. 未完成边界

- 真实告警平台接入
- Prometheus / Grafana 或云监控
- 集中日志系统
- 自动化备份任务
- 定期恢复演练
- 服务级 SLO
