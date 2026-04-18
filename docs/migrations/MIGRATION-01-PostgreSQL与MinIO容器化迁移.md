# MIGRATION-01: PostgreSQL 与 MinIO 容器化迁移记录

**生效日期**: 2026-04-18
**状态**: 已完成

---

## 1. 概述

为了提升 BOBBuy 平台的生产就绪度（Production Readiness）和可移植性，我们将核心存储从本地文件系统和嵌入式 H2 数据库迁移到了容器化的 PostgreSQL 和 MinIO (S3 兼容存储)。

## 2. 数据库迁移 (H2 -> PostgreSQL)

### 2.1 物理驱动与环境适配
- **变更点**：将 `spring.datasource.url` 从 `jdbc:h2:file:./data` 切换为 `jdbc:postgresql://postgres:5432/bobbuy`。
- **关键修复**：在 `docker-compose.yml` 中显式指定 `org.postgresql.Driver`，以覆盖 Spring Boot 对 H2 的默认优先级。

### 2.2 JSONB 类型适配
- **痛点**：PostgreSQL 对 JSONB 类型有严格验证，原有 `AttributeConverter` 产生的 `String` 类型会被 PG 拒绝。
- **方案**：
    - 删除了手动转换类（如 `OrderLineListJsonConverter`）。
    - 采用 Hibernate 6 原生注解 `@JdbcTypeCode(SqlTypes.JSON)`。
    - 涉及实体：`Product`, `OrderHeader`, `Category`, `Supplier`。

## 3. 存储迁移 (Local FS -> MinIO)

### 3.1 架构调整
- **旧方案**：通过 `/uploads` 静态目录直接读写宿主机文件。
- **新方案**：通过 `ImageStorageService` 封装 `MinioClient`，实现对象存储。
- **访问路径**：通过 MinIO 端点（如 `minio:9000/bobbuy/filename.jpg`）直接读取。

### 3.2 文件同步
- **上架证据图**：AI 识别过程中的 Base64 原始图现在自动上传至 MinIO `bobbuy` 桶。
- **权限控制**：Bucket 设置为 `public-read`，简化前端展示逻辑。

## 4. 后续注意事项
- **备份**：生产环境需对 Docker Volume `bobbuy_postgres_data` 和 `bobbuy_minio_data` 执行定期快照。
- **清理**：旧的 `/backend/data/` 目录和 `./uploads/` 文件夹已不再使用，可安全删除。

---
**核准人**: Antigravity AI
**记录日期**: 2026-04-18
