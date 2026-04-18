# 开发 Prompt G：生产级 AI 上架引擎 (The Production Onboarding Engine)

**状态**：等待评审
**目标**：驱动 AI 代理完成从“识别照片”到“完成上架/更新”的完整生产闭环。

---

## 1. 核心上下文 (Context)
- **项目**：BOBBuy (代购系统集成端)
- **主力引擎**：Brave Search (Google 已禁用)。必须通过 `AiSearchService` 进行安全解密调用。
- **双 LLM 策略**：
    - **Edge Node (Llava)**：负责多模态视觉解析，提取原始字段（名称、价格、货号）。
    - **Cloud Core (Qwen 14b)**：负责多源信息整合，将视觉提取结果与网页搜索结果合成为标准描述。
- **业务关键点**：`itemNumber` 为唯一匹配键。支持增量更新已有商品的价格阶梯 (`priceTiers`)。

---

## 2. 详细任务定义 (Task Specification)

### A. 后端：AI 分拆编排逻辑
在 `AiProductOnboardingService.java` 中实现 `onboardFromPhoto(String base64Image)`：
1.  **Vision 提取**：调用边缘节点模型解析照片，重点关注货号和阶梯价。
2.  **增量匹配**：利用 `productRepository.findByItemNumber` 检查数据库。如果命中，记录 `existingProductId`。
3.  **深度研究 (Brave)**：依据提取出的 `brand + name` 调用 `AiSearchService`。
4.  **知识合成 (Synthesis)**：将 Vision 提取的片段与 Search 返回的网页摘要发送给主模型 Qwen，产出最终结果。

### B. 后端：入库确认 API
在 `MobileProductController.java` 中新增 `POST /api/ai/onboard/confirm`：
- 接收 `AiOnboardingSuggestion` 对象。
- 如果存在 `existingProductId`，则调用 `store.patchProduct` 执行增量更新。
- 如果不存在，则调用 `store.createProduct` 创建新档案。

### C. 前端：UI 链路闭环
1.  **AiQuickAddModal.tsx**：当匹配到已有商品时，显示蓝色 `Alert` 提示：“为您找到了现有商品：{itemId}”。
2.  **StockMaster.tsx**：在编辑详情抽屉中，默认选中“价格阶梯”标签页以便 Agent 确认。

---

## 3. 开发约束 (Constraints)
- **安全性**：禁止在日志中打印解密后的 API Key。
- **稳定性**：对搜索 API 的网络调用必须设置 5s 超时。
- **一致性**：价格阶梯必须存入 PostgreSQL 的 JSONB 字段。
