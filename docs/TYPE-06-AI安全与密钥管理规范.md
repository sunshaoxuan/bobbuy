# AI 安全与密钥管理规范

此文档定义了 Bobbuy 项目中 AI 相关密钥（如 Brave Search / Google Search）的安全管理标准，确保敏感信息不在仓库中以明文形式存在。

## 1. 存储格式与算法

所有系统外接 API 密钥在配置文件（`application-ai-hermes.properties`）中均为加密存储。

- **加密算法**: `AES-256-GCM`
- **密钥派生 (KDF)**: `PBKDF2-HMAC-SHA256`
- **迭代次数**: `200,000`
- **配置项**:
  - `salt`: KDF 盐值 (Base64)
  - `nonce`: 初始化向量 (Base64)
  - `ciphertext`: 密文 (Base64)
  - `tag`: 认证标签 (Base64)

## 2. 运行时解密流程

系统启动或首次调用服务时，执行以下逻辑：

1. **获取主密码**: 从环境变量 `AI_ENCRYPTION_PWD` 获取操作员密码。
2. **派生密钥**: 使用配置的 `salt` 和 `iterations` 通过 `PBKDF2WithHmacSHA256` 生成 256 位 AES 密钥。
3. **执行解密**: 使用 Java JCE (`AES/GCM/NoPadding`) 解析密文。
   - *注意*: JCE 实现要求将 `tag` 附加在 `ciphertext` 之后作为单一 byte 数组处理。

## 3. 多端点编排 (Dual-LLM Orchestration)

系统采用 **Main/Edge 模式** 进行任务分发：

- **主推力 (Main)**: `ccnode.briconbric.com`，负责复杂逻辑、多级数据提取。
- **边缘节点 (Edge)**: `192.168.20.218`，负责本地全量图扫描、低延迟 OCR。

### 路由规则：
- 携带 `images` 列表的任务 -> 自动路由至 **Edge**。
- 指定模型为 `llava` 的任务 -> 自动路由至 **Edge**。
- 纯文本提取、研究合并任务 -> 路由至 **Main**。
- **故障转移**: 主节点不可用时，尝试自动回退至边缘节点处理纯文本任务。
