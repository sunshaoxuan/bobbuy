# AI onboarding sample field verification report

- Mode: gate (default blocking mode)
- Total samples: 3
- PASS: 3
- FAIL: 0
- SCAN_FAIL: 0
- MISSING_FILE: 0
- SKIPPED(NEEDS_HUMAN_GOLDEN): 0
- gatePassed: True

| Sample | Status | Trace Stage | OCR/LLM | Fallback |
| :-- | :-- | :-- | :-- | :-- |
| IMG_1484.jpg | PASS | SEMANTIC_COMPARE | python-ocr/codex-bridge |  |
| IMG_1638.jpg | PASS | SEMANTIC_COMPARE | python-ocr/codex-bridge |  |
| IMG_1510.jpg | PASS | SEMANTIC_COMPARE | python-ocr/codex-bridge |  |

## IMG_1484.jpg

- Status: PASS
- Detail: Costco seafood label sample used for new-product AI onboarding verification.

| Expected Path | Actual Path | Expected | Actual | Result | Reason |
| :-- | :-- | :-- | :-- | :-- | :-- |
| name | name | Mixed Seafood | MIXED SEAFOOD | PASS | exact-or-synonym |
| brand | brand |  |  | PASS | optional-null |
| itemNumber | itemNumber | 53432 | 53432 | PASS | exact-or-synonym |
| basePrice | price | 2698 | 2698 | PASS | numeric-within-tolerance |
| categoryId | categoryId | cat-1000 | cat-1000 | PASS | exact-or-synonym |
| storageCondition | storageCondition | AMBIENT | AMBIENT | PASS | exact-or-synonym |
| orderMethod | orderMethod | DIRECT_BUY | DIRECT_BUY | PASS | exact-or-synonym |
| attributes.netContent | attributes.netContent |  |  | PASS | optional-null |
| attributes.pricePerUnit | attributes.pricePerUnit | 498円/100g | 498円/100g | PASS | exact-or-synonym |
| attributes.packSize | attributes.packSize |  |  | PASS | optional-null |

## IMG_1638.jpg

- Status: PASS
- Detail: Existing-product match sample kept in the dedicated AI E2E suite.

| Expected Path | Actual Path | Expected | Actual | Result | Reason |
| :-- | :-- | :-- | :-- | :-- | :-- |
| existingProductFound | existingProductFound | True | True | PASS | exact-or-synonym |
| existingProductId | existingProductId | prd-1638 | prd-1638 | PASS | exact-or-synonym |
| categoryId | categoryId | cat-1000 | cat-1000 | PASS | exact-or-synonym |
| storageCondition | storageCondition | AMBIENT | AMBIENT | PASS | exact-or-synonym |
| orderMethod | orderMethod | DIRECT_BUY | DIRECT_BUY | PASS | exact-or-synonym |
| attributes.netContent | attributes.netContent |  | 9g | PASS | optional-null |
| attributes.pricePerUnit | attributes.pricePerUnit |  |  | PASS | optional-null |
| attributes.packSize | attributes.packSize |  |  | PASS | optional-null |

## IMG_1510.jpg

- Status: PASS
- Detail: Reserved ambiguous sample for future manual golden refinement.

| Expected Path | Actual Path | Expected | Actual | Result | Reason |
| :-- | :-- | :-- | :-- | :-- | :-- |
| name | name |  | FAUX FUR MINI QUAD RUG | PASS | optional-null |
| brand | brand |  | MIOI CIIATEAU | PASS | optional-null |
| itemNumber | itemNumber |  |  | PASS | optional-null |
| basePrice | price |  | 2198 | PASS | optional-null |
| categoryId | categoryId |  |  | PASS | optional-null |
| storageCondition | storageCondition | AMBIENT | AMBIENT | PASS | exact-or-synonym |
| orderMethod | orderMethod | DIRECT_BUY | DIRECT_BUY | PASS | exact-or-synonym |
| attributes.netContent | attributes.netContent |  |  | PASS | optional-null |
| attributes.pricePerUnit | attributes.pricePerUnit |  |  | PASS | optional-null |
| attributes.packSize | attributes.packSize |  | 24”×39”(80cm×99cm) | PASS | optional-null |
