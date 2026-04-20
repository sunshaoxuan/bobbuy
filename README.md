# BOBBuy

**BOBBuy** 是一款先进的全球委托任务与代购服务平台（Global Errand & Personal Shopper Platform）。通过集成 AI 能力、可审计的采购协作链路与跨端订单闭环，平台致力于构建一个高效、信任且透明的跨境委托生态系统。

## 🌐 平台愿景
旨在链接全球范围内具有特定代购或代办需求的委托人（Customer）与能够提供专业服务的代理人（Agent/Merchant），打破地域限制，实现全球资源的无缝获取。

## ✨ 核心特性
- **💬 采购沟通留痕 (Procurement Chat)**：当前提供 REST 持久化消息、历史查询与轮询刷新；WebSocket 实时推送集群仍在下一里程碑。
- **🤖 AI 流程自动化 (AI Automation)**：
  - **AI 现场采集 (AI Sourcing)**：拍摄一张卖场货架照片，AI 自动识别品类、名称、货号、含税/免税价格，并自动上架。
  - **智能深度研究 (Deep Research)**：基于采集到的片段信息，自动进行全网搜索，补全商品详细介绍、规格参数并采集官方高清原图。
  - **语义提取与意图识别**：自动解析非结构化沟通记录，提取任务清单与关键要素。
  - **动态决策支持**：根据历史数据与市场趋势，提供最优路径建议与价格推演。
- **🧾 精准财务管理**：支持多维度订单拆分、自动分账、费用平摊及多币种结算。
- **🛡️ 信任与合规体系**：建立数字化的信用评分模型、内容审核机制及争议处理流程，确保交易安全。

## 🛠 技术架构
- **后端 (Backend)**：基于 **Spring Boot 3** 的高性能微服务架构，提供金融级的服务治理支持。
- **前端 (Frontend)**：**React 18** 结合 **Ant Design** 体系，提供移动优先（Mobile-First）且响应迅速的用户交互体验。
- **资源与代理**：**Nginx** 生产级静态资源服务与 API 反向代理。
- **数据与存储 (Storage)**：
  - **PostgreSQL 15**：依托 JSONB 特性和关系型约束，处理复杂元数据。
  - **MinIO**：兼容 S3 协议的大规模对象存储。
- **中间件 (Middleware)**：**Redis** 与 **RabbitMQ** 已纳入部署方案；当前核心业务闭环仍以 Spring Boot + PostgreSQL/MinIO + REST 接口为主。

## 🚀 发展路线
1. **Phase 1: 核心链路构建 (Core MVP)**：聚焦核心代购流程的数字化与 AI 辅助分析能力的落地。 [DONE]
2. **Phase 2: 全栈容器化与基础设施现代化 (Modernization)**：实现一键 Docker 编排，保障生产环境一致性，迁移至 PG/MinIO。 [IN PROGRESS]
3. **Phase 3: 采购 HUD 与 规模化运营 (Scale-up)**：推出实时利润看板，强化物流集成与全球服务网络。

## 📂 文档索引
- [MIGRATION-01: PostgreSQL 与 MinIO 迁移记录](docs/migrations/MIGRATION-01-PostgreSQL与MinIO容器化迁移.md)
- [ARCH-13: 全栈容器化部署方案](docs/architecture/ARCH-13-全栈容器化部署方案.md)
- [PROD-01: 原始需求清单](docs/design/PROD-01-原始需求清单.md)
- [ARCH-01: 平台技术架构选型](docs/architecture/ARCH-01-平台技术架构选型.md)
- [ARCH-11: 订单头行模型与业务幂等详细设计](docs/architecture/ARCH-11-订单头行模型与业务幂等详细设计说明书.md)
- [ARCH-12: 商品主数据模型与多语种交互设计](docs/architecture/ARCH-12-商品主数据模型与多语种交互设计.md)

## 🐳 快速开始 (Docker Compose)

本项目已实现标准全栈容器化，推荐直接使用 Docker 启动完整环境。

### 1. 启动全栈服务
在仓库根目录下执行：
```powershell
docker-compose -p bobbuy up -d
```

### 2. 访问入口
- **前端控制台**：[http://localhost](http://localhost)
- **后端 API**：[http://localhost/api](http://localhost/api) (或直接访问容器端口 8080)
- **MinIO 控制台**：[http://localhost:9001](http://localhost:9001) (默认 Access Key: `minioadmin`, Secret Key: `minioadmin`)

### 3. 数据持久化
所有数据库和存储数据默认挂载在项目根目录下的 `data/` 卷中。

### 4. 初始化数据说明
- 默认启动 **不会** 自动清空业务数据或重灌 Seed。
- 如需本地演示数据，请显式启用 `dev` profile 或设置 `bobbuy.seed.enabled=true`。

---
© 2026 BOBBuy 团队. 保留所有权利。
