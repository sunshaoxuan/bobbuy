# ARCH-13: 全栈容器化部署方案

**生效日期**: 2026-04-18
**状态**: 执行中；2026-04-28 已按当前 Compose 拓扑修订

---

## 1. 架构目标

实现“一键部署、环境一致、服务隔离”的生产级架构。将整个 BOBBuy 平台的所有组件（前/后端、数据库、中间件）封装进相互隔离的容器组。

## 2. 拓扑结构 (Topology)

所有容器加入 `bobbuy-net` (Bridge) 网络，实现通过内部服务名互访。

| 服务名 | 镜像 | 外部端口 | 内部端口 | 职责 |
| :--- | :--- | :--- | :--- | :--- |
| `gateway` | `nginx:alpine` | `80` | `80` | 对外入口，转发前端与 API |
| `frontend` | 项目 Dockerfile | - | `80` | 提供静态资源 |
| `gateway-service` | `Dockerfile.service` | - | `8080` | Spring Cloud Gateway 路由 |
| `core-service` | `Dockerfile.service` | - | `8081` | 订单、行程、采购、财务核心业务 |
| `ai-service` | `Dockerfile.service` | - | `8082` | AI 解析、翻译、商品上架、小票识别入口 |
| `im-service` | `Dockerfile.service` | - | `8083` | 聊天 REST 与 WebSocket(STOMP) |
| `auth-service` | `Dockerfile.service` | - | `8084` | 认证服务预留节点 |
| `ocr-service` | `bobbuy-ocr` | `8000` | `8000` | Python OCR 服务 |
| `postgres` | `postgres:18-alpine` | `5432` | `5432` | 核心业务数据持久化 |
| `minio` | `minio/minio` | `9000/9001` | `9000/9001` | 证据图与非结构化数据存储 |
| `redis` | `redis:alpine` | `6379` | `6379` | 缓存与 Session 管理 |
| `rabbitmq` | `rabbitmq:3-management-alpine` | 内网 | `5672/61613` | AMQP / STOMP broker relay |
| `nacos` | `nacos/nacos-server:v2.3.2-slim` | 内网 | `8848` | 服务发现与配置 |

## 3. 持久化策略 (Persistence)

通过 Docker Volumes 确保数据在容器销毁后依然保留：

- `./data/postgres_v18` -> `/var/lib/postgresql`
- `bobbuy_minio_data` -> `/data`
- `bobbuy_redis_data` -> `/data`
- `bobbuy_rabbitmq_data` -> `/var/lib/rabbitmq`

## 4. 关键环境变量配置

后端服务容器的核心连接参数采用环境变量注入，避免在代码中硬编码：

```yaml
SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/bobbuy
SPRING_DATA_REDIS_HOST: redis
SPRING_RABBITMQ_HOST: rabbitmq
BOBBUY_MINIO_ENDPOINT: http://minio:9000
BOBBUY_AI_LLM_MAIN_PROVIDER: auto
BOBBUY_AI_LLM_MAIN_URL: http://ccnode.briconbric.com:22545
BOBBUY_AI_LLM_CODEX_COMMAND: codex
```

## 6. 当前部署边界

- 当前 Compose 更接近集成/试运行部署，不等同于生产高可用方案。
- Codex CLI 兜底只适合本地或私有 gateway；Linux 容器内不应假定存在 Codex 登录态。
- 尚未引入 Flyway/Liquibase，生产前需要补数据库迁移治理。
- Nacos、RabbitMQ、MinIO、数据库密码与网络暴露策略仍需按生产安全标准加固。

## 5. 安全性考量

- **内部网络保护**：数据库和中间件端口虽开放于宿主机，但核心业务通信均走容器内网空间。
- **MinIO 策略**：默认情况下，`bobbuy` 存储桶启用预览权限以便前端展示，但建议在生产环境配置 CDN 或反向代理鉴权。

---
**核准人**: Antigravity AI
**大纲审计状态**: 已核对 (Linear Integrity Verified)
