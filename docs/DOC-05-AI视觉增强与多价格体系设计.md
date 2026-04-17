# DOC-05-AI 视觉增强与多价格体系设计

## 1. 背景与目标
为了进一步提升采买效率并支持灵活的业务策略，系统需要增强 AI 视觉功能以支持**已有商品的快速更新**。同时，引入**多价格体系**，允许采买员（Agent）针对不同客户设定不同的价格，且该过程对客户保持不可见（隐蔽性）。

## 2. 核心功能设计

### 2.1 AI 视觉增强：商品增量更新
目前的 AI Onboarding 仅支持新建。增强后，系统应具备识别并更新已有商品的能力。

- **识别逻辑**：
    - AI 从照片中提取名称、品牌、价格、货号（SKU）。
    - 系统优先通过 `merchant_skus` 进行精确匹配。
    - 若无货号，则通过语义搜索或名称模糊匹配寻找相似商品。
- **更新维度**：
    - **基础价格 (Base Price)**：更新最新的市场价。
    - **优惠信息 (Promotion)**：包括优惠价 (Discount Price) 和优惠期间 (Discount Period: Start/End)。
    - **素材增强 (Media)**：自动将识别到的新照片加入图集。
- **UI 交互**：
    - 扫描完成后，若发现匹配项，提示用户“发现已有商品，是否更新？”。
    - 显示对比视图（旧价格 vs 新价格）。

### 2.2 多价格体系 (Multi-Price System)
支持一个商品对应多个价格维度，并根据采买场景动态选择。

- **数据模型演进**：
    - 在 `Product` 中增加 `price_tiers` (jsonb)，存储 `{"tier_name": price}`。
    - 增加 `customer_price_overrides` (jsonb)，存储 `{"customer_id": price}`，实现特定客户的定价逻辑。
- **采买员（Agent）操作流**：
    - 在采买工作台 (Procurement HUD) 中，当录入某位客户的商品时，Agent 可以点击价格旁边的“切换阶梯”按钮。
    - 界面显示：公价、阶梯 A、阶梯 B、客户专享价。
    - 选中后，该价格将应用到对应的 `OrderLine`。
- **隐蔽性设计**：
    - 客户端的订单详情和支付页面仅显示为其选定的最终价格。
    - 任何关于“价格阶梯”或“切换”的 UI 仅在 Agent 角色的界面中渲染。

## 3. 技术方案 (Logical Model Only)

### 3.1 实体变更 (Logical Entity)
- **Product (bb_product)**:
    - `discountPrice`: double
    - `discountStart`: datetime
    - `discountEnd`: datetime
    - `priceTiers`: Map<String, Double> (JSONB)
    - `customerPrices`: Map<Long, Double> (JSONB, Key is CustomerID)

### 3.2 服务逻辑
- **AiProductUpdateService**:
    - 接收识别结果。
    - 执行 `findMatchingProduct`。
    - 若匹配，则合并增量字段。
- **PriceResolutionService**:
    - 输入：`productId`, `customerId`。
    - 输出：适用价格（优先级：客户专享价 > 阶梯价 > 基础价）。

## 4. 后续步骤
1. 修改后端 `Product` 实体类，增加价格及优惠字段。
2. 扩展 `AiProductOnboardingService` 以支持更新逻辑。
3. 前端 `StockMaster` 抽屉中增加多价格编辑组件。
4. 前端 `ProcurementHUD` 增加价格选择小挂件（仅 Agent 可见）。
