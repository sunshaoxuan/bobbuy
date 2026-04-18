# ARCH-13: 全栈容器化部署方案

**生效日期**: 2026-04-18
**状态**: 执行中

---

## 1. 架构目标

实现“一键部署、环境一致、服务隔离”的生产级架构。将整个 BOBBuy 平台的所有组件（前/后端、数据库、中间件）封装进相互隔离的容器组。

## 2. 拓扑结构 (Topology)

所有容器加入 `bobbuy-net` (Bridge) 网络，实现通过内部服务名互访。

| 服务名 | 镜像 | 外部端口 | 内部端口 | 职责 |
| :--- | :--- | :--- | :--- | :--- |
| `frontend` | `nginx:alpine` | `80` | `80` | 提供静态资源访问与 API 转发 |
| `backend` | `openjdk:17` | `8080` | `8080` | 核心业务逻辑实现 |
| `postgres` | `postgres:15-alpine` | `5432` | `5432` | 核心业务数据持久化 |
| `minio` | `minio/minio` | `9000/9001` | `9000/9001` | 证据图与非结构化数据存储 |
| `redis` | `redis:alpine` | `6379` | `6379` | 缓存与 Session 管理 |
| `rabbitmq` | `rabbitmq:3-mgmt` | `5672/15672` | `5672/15672` | 异步任务队列与消息通知 |

## 3. 持久化策略 (Persistence)

通过 Docker Volumes 确保数据在容器销毁后依然保留：

- `bobbuy_postgres_data` -> `/var/lib/postgresql/data`
- `bobbuy_minio_data` -> `/data`
- `bobbuy_redis_data` -> `/data`
- `bobbuy_rabbitmq_data` -> `/var/lib/rabbitmq`

## 4. 关键环境变量配置

后端容器 (`backend`) 的核心连接参数采用环境变量注入，避免在代码中硬编码：

```yaml
SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/bobbuy
SPRING_DATA_REDIS_HOST: redis
SPRING_RABBITMQ_HOST: rabbitmq
BOBBUY_MINIO_ENDPOINT: http://minio:9000
```

## 5. 安全性考量

- **内部网络保护**：数据库和中间件端口虽开放于宿主机，但核心业务通信均走容器内网空间。
- **MinIO 策略**：默认情况下，`bobbuy` 存储桶启用预览权限以便前端展示，但建议在生产环境配置 CDN 或反向代理鉴权。

---
**核准人**: Antigravity AI
**大纲审计状态**: 已核对 (Linear Integrity Verified)
