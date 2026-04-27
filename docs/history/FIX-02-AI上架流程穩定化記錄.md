# FIX-02: AI 上架流程穩定化記錄

**生效日期**: 2026-04-27
**狀態**: 已完成

---

## 1. 概述
為了解決 AI 拍照上架流程中出現的數據庫持久化失效、OCR 識別與 AI 整理失敗、以及 UI 交互邏輯缺陷等問題，執行了本次穩定化修復。

## 2. 修復內容

### 2.1 基礎設施與持久化
- **數據庫統一**：修正 `docker-compose.yml`，強制 `ai-service`、`core-service` 等所有服務使用共享的 Postgres 實例。
- **Schema 同步**：修復了 `bb_product` 表缺少 `is_recommended`、`item_number` 等字段的問題，確保 AI 整理後的實體能正確寫入。

### 2.2 AI 管線優化
- **術語更新**：將 UI 中的「AI 識別」更新為「**OCR 識別 + AI 整理**」，以匹配實際技術架構。
- **解析增強**：更新了 `AiProductOnboardingService` 中的正則表達式，支持過濾 LLM 可能返回的 `<think>` 思考塊。

### 2.3 UI/UX 修復
- **交互保護**：在 `AiQuickAddModal` 中增加了加載狀態鎖定，解決了用戶在處理過程中重複點擊導致的邏輯衝突。

## 3. 驗證結果
- **批量導入**：成功導入 `sample/` 目錄下的全部 10 張商品圖片。
- **數據核對**：數據庫 `bb_product` 表記錄數由 1 增加至 11。
- **UI 確認**：在「庫存主表」畫面可正常看到新導入的商品及其價格、規格等信息。

---

## 4. 附錄：物理實現參考
- 涉及邏輯字段變更：
  - `isRecommended` (Boolean)
  - `itemNumber` (String)
  - `isTemporary` (Boolean)
  - `priceTiers` (JSONB)
