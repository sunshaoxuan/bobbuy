# ARCH-12: 商品主数据模型与多语种交互设计

**生效日期**: 2026-04-16  
**状态**: 草案  
**引用**: [ARCH-09-多语言与AI翻译引擎详细设计](./ARCH-09-多语言与AI翻译引擎详细设计.md), [STD-02-文档编写规范](../process/STD-02-文档编写规范.md)

---

## 1. 总体目标
本设计旨在构建一个高扩展性、支持全球化（多语种）且具备复杂属性管理能力的商品主数据系统。设计核心涵盖了媒体图集、多供应商编码映射、分类动态属性以及移动端的高效率交互模式。

## 2. 逻辑数据模型 (Logical Data Model)

### 2.1 核心实体：商品 (Product)
| 字段名称 | 类型 | 说明 | 多语种支持 |
| :--- | :--- | :--- | :--- |
| `id` | GUID | 平台唯一标识 | 否 |
| `name` | JSONB | 商品显示名称 (Key: Locale, Value: String) | **是** |
| `description` | JSONB | 详细描述 (Rich Text/MT) | **是** |
| `brand` | String | 品牌名称 | 否 |
| `base_price` | Decimal | 平台建议起步价 | 否 |
| `media_gallery` | JSONB | 媒体数组，见 2.2 | **部分 (标题)** |
| `storage_condition` | Enum | [AMBIENT, FRESH, CHILLED, FROZEN] | 否 |
| `order_method` | Enum | [PRE_ORDER, DIRECT_BUY] | 否 |
| `category_id` | GUID | 关联分类，决定动态属性模板 | 否 |

### 2.2 媒体图集 (Media Gallery)
存储为 JSONB 数组，每个对象包含：
- `url`: 媒体资源链接。
- `type`: `image` 或 `video`。
- `title`: JSONB (多语种标题)。

### 2.3 供应商与多渠道编码 (Merchant SKU Mapping)
为支持同一商品在不同供应商处的编码管理，引入：
- **供应商 (Supplier/Merchant)**: `id`, `name`, `contact_info`。
- **关联映射 (MerchantSku)**: `{ product_id, merchant_id, sku_code, last_price, stock_status }`。

### 2.4 分类驱动的动态属性 (Category-Specific Attributes)
- **分类 (Category)**:
    - 包含 `attribute_definitions`: JSONB 定义（如 `[{name: "Color", type: "Select", options: ["Red", "Blue"]}]`）。
- **商品属性值 (ProductAttributeValue)**: 
    - `{ product_id, attr_name, value }`。

---

## 3. 多语种交互设计 (Data I18n Interaction)

### 3.1 字段交互模式 (Field Level L10n)
为了不让界面因多语种而显得拥挤，前端采用 **“局部切换 + 智能回退”** 模式：
1. **主语种焦点**: 默认显示当前 UI 语言对应的语种内容。
2. **地球图标/快捷切换**: 鼠标悬停或点击字段旁的“地球”图标，展开微型语言面板（如 `[CN] [EN] [JP]`）。
3. **级联翻译**: 点击面板中的语种，输入框切换为对应语种数据。若该语种缺失，显示灰色的 AI 建议翻译（引用 ARCH-09 引擎结果）。
4. **回退逻辑 (Fallback)**:
   - 渲染时，若 `locale_A` 缺失，则查找顺序：`APP_LOCALE` -> `DEFAULT_LOCALE (zh-CN)` -> `FIRST_AVAILABLE`。

---

## 4. 移动端与交互优化 (Mobile & UX)

### 4.1 响应式抽屉 (Responsive Drawer)
- **PC/Pad**: 侧边抽屉（Sider Drawer），宽度固定或百分比。
- **Mobile**: 转换为 **底部浮层 (Bottom Sheet)**。向上滑动可扩充为全屏。
- **编辑即保存 (Autosave)**:
  - 针对移动端高频、碎片化操作，移除显式的“保存”按钮。
  - 使用 **Debounced Sync (防抖同步)** 技术：用户停止输入 500ms 后，自动向后端发起 `PATCH` 请求。
  - 界面左上角显示微小的“云端保存中/已保存”微标（Sync Indicator）。

### 4.2 输入效率
- 集成 **AI Scan**（引用 UID1120 设计）：拍摄条码或外观，自动识别并预填充字段。

---

## 5. 待办事项 (Next Steps)
1. 定义具体的 PostgreSQL `JSONB` 索引策略，确保多语种搜索效率。
2. 细化 `category_id` 对应的属性校验器（Validator）。
3. 原型验证：多语种切换在移动端的平滑度。

---
**核准**: Architect  
**存档**: docs/architecture/ARCH-12-商品主数据模型 with absolute path.
