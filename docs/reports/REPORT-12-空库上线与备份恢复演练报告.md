# REPORT-12: 空库上线与备份恢复演练报告

**日期**: 2026-05-02
**结论**: 当前内部试运行采用空库上线；旧库 adoption 因无历史数据标记为不适用。

---

## 1. 决策

此前 `REPORT-07` 将“真实旧库 adoption / restore drill”列为 blocker。当前用户已确认没有需要迁移的历史业务数据，因此该 blocker 调整为：

- **旧库 adoption**: 不适用
- **替代门禁**: 空库 Flyway 初始化、首启 seed 策略、真实栈登录与双角色核心流程、PostgreSQL/MinIO/Nacos 备份恢复演练

如果未来引入历史数据导入或迁移需求，必须重新启用旧库 adoption / baseline / migrate / validate / restore drill。

---

## 2. 空库上线验收口径

| 验证项 | 当前证据 | 状态 |
| :-- | :-- | :-- |
| Compose 配置渲染 | `docker compose config --quiet` | PASS |
| 服务 jar 预构建 | `bash scripts/build-service-images.sh` | PASS |
| 完整 Compose 栈 | postgres/minio/redis/rabbitmq/nacos/core/ai/im/auth/gateway/frontend/ocr | PASS |
| gateway health | `/api/health`、`/api/actuator/health`、`/api/actuator/health/readiness` | PASS |
| OCR health | `http://127.0.0.1:8000/health` | PASS |
| 真实栈登录 | customer / agent | PASS |
| 双角色移动端核心路径 | `mobile_customer_blackbox` + `mobile_agent_blackbox` real stack | PASS |
| AI provider secret 注入 | 本轮通过临时环境变量注入 Codex Bridge；未写入 git | PASS |

---

## 3. Seed 策略

- `BOBBUY_SEED_ENABLED=false` 仍是默认生产安全值。
- 本地/试运行验收可显式开启 `BOBBUY_SEED_ENABLED=true`。
- 采购者拣货黑盒所需的 confirmed order、reviewed receipt 与 picking checklist fixture 由 `BOBBUY_SEED_PICKING_FIXTURE_ENABLED=true` 单独开启，避免污染默认后端测试对单条 seed 订单的假设。
- 生产共享环境默认应保持两个 seed 开关均为 `false`。

---

## 4. Secret 策略

以下变量必须由 `.env`、服务器 secret manager 或 CI secret 注入，不得提交明文：

- `BOBBUY_SECURITY_JWT_SECRET`
- `BOBBUY_SECURITY_SERVICE_TOKEN`
- `POSTGRES_PASSWORD`
- `MINIO_ROOT_PASSWORD`
- `RABBITMQ_DEFAULT_PASS`
- `BOBBUY_AI_LLM_CODEX_BRIDGE_API_KEY`
- `BOBBUY_AI_SECRET_MASTER_PASSWORD`（仅当使用加密 bridge secret 时）

本轮真实 AI 与真实黑盒验证使用临时本机环境变量注入 Codex Bridge key，仓库未保存明文 key。

---

## 5. 备份恢复要求

内部试运行上线前至少保留以下演练记录：

1. PostgreSQL: `pg_dump` -> 新库 restore -> 校验核心表行数与 `flyway_schema_history`。
2. MinIO: 归档 bucket 对象 -> 恢复到独立 bucket -> 校验探针文件与至少一张业务图片。
3. Nacos: 归档 `infra/nacos/config` 或线上配置导出 -> 恢复到隔离命名空间。

本轮代码侧已完成空库真实栈与双角色黑盒证据；备份恢复演练应在试运行部署窗口用实际服务器卷与 bucket 复跑并补充 artifact。PLAN-57 已接管服务器窗口执行与放行复判；当前缺少 `SSH_TARGET` 与 `APP_DIR`，服务器恢复演练尚未执行，见 `REPORT-13`。

---

## 6. 放行影响

旧库 adoption 从 `BLOCKED` 调整为 `N/A` 后，内部试运行放行剩余关注点变为：

- 服务器部署窗口复跑完整门禁与真实栈双角色黑盒。
- 保持真实 AI sample gate 与 `e2e:ai` PASS。
- 确认 secret 注入、备份恢复、健康检查流程可复现。
- 提供 `SSH_TARGET` 与 `APP_DIR` 后执行 PLAN-57，才能把本报告从本地验收口径提升为服务器试运行放行证据。
