package com.bobbuy.service;

import com.bobbuy.api.AiOnboardingSuggestion;
import com.bobbuy.api.AiOnboardingTrace;
import com.bobbuy.api.AiFieldDiff;
import com.bobbuy.api.AiProductCandidate;
import com.bobbuy.api.AiVerificationTarget;
import com.bobbuy.model.MediaGalleryItem;
import com.bobbuy.model.MediaType;
import com.bobbuy.model.OrderMethod;
import com.bobbuy.model.PriceTier;
import com.bobbuy.model.Product;
import com.bobbuy.model.ProductVisibility;
import com.bobbuy.model.StorageCondition;
import com.bobbuy.repository.ProductRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AiProductOnboardingService {
    private static final Logger log = LoggerFactory.getLogger(AiProductOnboardingService.class);
    // Allow one-third token overlap so short bilingual names can still surface as manual-review candidates.
    private static final double CANDIDATE_OVERLAP_THRESHOLD = 0.34d;
    // Below this score, name-level matches are too weak to justify manual-review noise in the UI.
    private static final double CANDIDATE_MIN_SCORE = 2.4d;
    private static final double SHARED_TOKEN_WEIGHT = 1.2d;
    private static final double OVERLAP_WEIGHT = 2.5d;
    private static final double BRAND_EXACT_BONUS = 2.2d;
    private static final double ITEM_NUMBER_FRAGMENT_BONUS = 1.8d;
    private static final double CATEGORY_MATCH_BONUS = 0.8d;
    private static final double STRONG_NAME_OVERLAP_BONUS = 0.8d;
    private static final double MEDIUM_NAME_OVERLAP_BONUS = 0.4d;
    // Keep audit metadata concise so chat cards remain readable while still showing the strongest evidence.
    private static final int MAX_MATCHED_AUDIT_FRAGMENTS = 4;
    private static final String SOURCE_POLICY_VERSION = "V23";
    private static final double SEMANTIC_OVERWRITE_THRESHOLD = 70d;
    private static final String STATUS_PUBLISHABLE = "PUBLISHABLE";
    private static final String STATUS_PENDING_MANUAL_REVIEW = "PENDING_MANUAL_REVIEW";
    private static final Pattern NET_CONTENT_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s?(kg|g|mg|lb|oz|l|ml)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PACK_PATTERN = Pattern.compile("(\\d+)\\s?(pack|pcs|piece|pieces|bottle|bottles|袋|包|盒|箱)", Pattern.CASE_INSENSITIVE);
    private static final Set<String> DENIED_SOURCE_KEYWORDS = Set.of(
        "xiaohongshu.com", "xhslink.com", "rednote", "weibo.com", "watermark"
    );
    private static final Set<String> LOW_TRUST_SOURCE_KEYWORDS = Set.of(
        "pinterest.", "imgur.", "douban.", "baidu.", "blogspot.", "wordpress."
    );
    private static final Set<String> OFFICIAL_STORE_KEYWORDS = Set.of(
        "official", "store", "shop", "mall"
    );
    private static final Set<String> TRUSTED_RETAIL_DOMAINS = Set.of(
        "costco.com", "walmart.com", "target.com", "amazon.", "rakuten.", "yodobashi.com", "biccamera.com", "nestle.jp"
    );
    private static final Map<String, List<String>> TOKEN_ALIASES = Map.of(
        "milk", List.of("牛乳", "ミルク"),
        "matcha", List.of("抹茶"),
        "tea", List.of("茶"),
        "chocolate", List.of("巧克力", "チョコ"),
        "pack", List.of("pkg", "pcs", "piece", "set"),
        "kg", List.of("kilogram"),
        "g", List.of("gram"),
        "l", List.of("liter"),
        "ml", List.of("milliliter")
    );
    private static final Set<String> FOOD_CATEGORY_HINTS = Set.of(
        "food", "foods", "snack", "snacks", "grocery", "groceries", "beverage", "drink", "fruit", "食品", "饮料", "飲料", "水果", "零食"
    );
    private static final Set<String> FLAVOR_IGNORED_TOKENS = Set.of(
        "brand", "food", "snack", "drink", "set", "box", "pack", "fresh", "organic"
    );

    private final LlmGateway llmGateway;
    private final AiSearchService aiSearchService;
    private final WebSearchService webSearchService;
    private final ProductRepository productRepository;
    private final com.bobbuy.repository.SupplierRepository supplierRepository;
    private final ObjectMapper objectMapper;

    public AiProductOnboardingService(LlmGateway llmGateway,
                                     AiSearchService aiSearchService,
                                     WebSearchService webSearchService,
                                     ProductRepository productRepository,
                                     com.bobbuy.repository.SupplierRepository supplierRepository,
                                     ObjectMapper objectMapper) {
        this.llmGateway = llmGateway;
        this.aiSearchService = aiSearchService;
        this.webSearchService = webSearchService;
        this.productRepository = productRepository;
        this.supplierRepository = supplierRepository;
        this.objectMapper = objectMapper;
    }

    public Optional<AiOnboardingSuggestion> onboardFromPhoto(String base64Image) {
        return onboardFromPhoto(base64Image, null);
    }

    public Optional<AiOnboardingSuggestion> onboardFromPhoto(String base64Image, String inputSampleId) {
        log.info("Starting AI onboarding from photo. Base64 length: {}", base64Image != null ? base64Image.length() : 0);
        if (base64Image == null || base64Image.isBlank()) {
            log.warn("Onboarding failed [VALIDATION]: empty image data provided.");
            throw new AiOnboardingPipelineException("VALIDATION", "error.ai.invalid_image", "Empty image data provided");
        }
        String normalizedBase64Image = normalizeBase64Image(base64Image);
        List<com.bobbuy.api.AiTraceEvent> traceEvents = new ArrayList<>();
        String inputRef = firstNonBlank(inputSampleId, "inline-image");
        if (normalizedBase64Image.isBlank()) {
            log.warn("Onboarding failed [VALIDATION]: image data is blank after normalization.");
            throw new AiOnboardingPipelineException("VALIDATION", "error.ai.invalid_image", "Image data is blank after normalization");
        }
        // 1. OCR Extract (Local Python Service)
        log.info("Phase 1: Dispatching to PaddleOCR service...");
        LocalDateTime ocrStartedAt = LocalDateTime.now();
        long ocrStartedNs = System.nanoTime();
        List<String> ocrLines = llmGateway.performOcr(normalizedBase64Image);
        long ocrLatencyMs = elapsedMs(ocrStartedNs);
        traceEvents.add(traceEvent(
            llmGateway.getOcrProvider(),
            llmGateway.getOcrProvider(),
            "paddleocr",
            "OCR",
            ocrLatencyMs,
            ocrLines.isEmpty() ? "OCR_FAILURE" : null,
            ocrLines.isEmpty() ? "OCR service returned no text." : null,
            null,
            0,
            1,
            inputRef,
            null,
            ocrLines.isEmpty() ? "FAILED_RECOGNITION" : "RECOGNIZED",
            ocrStartedAt,
            LocalDateTime.now()
        ));

        if (ocrLines.isEmpty()) {
            log.error("Onboarding failed [OCR_FAILURE]: OCR service returned no text.");
            throw new AiOnboardingPipelineException("OCR_FAILURE", "error.ai.ocr_failed", "Failed to extract text from image");
        }

        String rawOcrText = String.join("\n", ocrLines);
        log.info("Phase 1 Success: OCR complete. Extracted {} lines. Raw text: {}", ocrLines.size(), rawOcrText);

        // Pre-scan OCR for known item numbers (Costco 5-6 digits)
        List<Product> ocrMatchedProducts = new ArrayList<>();
        Pattern itemPattern = Pattern.compile("\\b(\\d{5,6})\\b");
        for (String line : ocrLines) {
            Matcher m = itemPattern.matcher(line);
            while (m.find()) {
                String candidate = m.group(1);
                productRepository.findByItemNumber(candidate).ifPresent(ocrMatchedProducts::add);
            }
        }

        String historicalContext = ocrMatchedProducts.stream()
            .map(p -> String.format("歷史檔案：货号 %s, 名称 %s, 品牌 %s",
                p.getItemNumber(),
                p.getName().getOrDefault("zh-CN", p.getId()),
                p.getBrand()))
            .distinct()
            .collect(Collectors.joining("\n"));

        if (!historicalContext.isEmpty()) {
            log.info("Phase 1.5: Found {} potential historical matches in OCR text.", ocrMatchedProducts.size());
        }

        // Fetch all merchant-specific rules
        String merchantRules = supplierRepository.findAll().stream()
            .map(s -> {
                String name = s.getName().getOrDefault("zh-CN", s.getId());
                Map<String, Object> rules = s.getOnboardingRules();
                if (rules == null || rules.isEmpty()) return null;
                return String.format("【%s】規則：%s", name, rules.toString());
            })
            .filter(Objects::nonNull)
            .collect(Collectors.joining("\n"));

        log.info("Phase 2: Active Merchant Rules: \n{}", merchantRules);

        // 2. LLM Logical Mapping (High Reasoning Model - e.g. Qwen3)
        log.info("Phase 2: Dispatching raw text to LLM for entity mapping...");
        String prompt = String.format("""
            你是一個專業的商品數據分析助手。請分析以下從商品圖片中提取的原始OCR文本，並將其整理成結構化JSON。

            === 原始OCR文本 ===
            %s

            === 商家規範 (優先執行) ===
            %s

            === 歷史檔案 (供匹配參考) ===
            %s

            === 提取原則 ===
            1. 【商品名稱】(name): 提取最顯著的完整商品名稱。
            2. 【品牌】(brand): 提取品牌、商標或製造商。
            3. 【貨號/品番】(itemNumber): 優先根據【商家規範】識別。如果沒有明確標籤，請在文本中尋找符合規範格式的編號。對於Costco商品，品番通常是5位或6位數字。
               - 優先尋找 "品番", "Item No", "SKU", "Part No" 等標識後的編碼。
               - 若標識缺失，請參考上面的 [商家規範] 和 [歷史檔案] 進行判斷。
               - ⚠️ 警告：嚴禁將重量（如 705g）、單價（如 498/100g）或條形碼誤認為貨號。
            4. 【價格】(basePrice): 提取最終結算總價，僅輸出數字。
            5. 【規格/淨含量】(netWeight): 提取重量、容量或包裝規格（如 705g, 500ml, 支裝）。
            6. 【單價】(pricePerUnit): 提取每單位價格（如 498円/100g）。

            === 輸出格式 ===
            只輸出JSON對象，不要有任何解釋。格式：
            {
              "name": "...",
              "brand": "...",
              "itemNumber": "...",
              "basePrice": 0.0,
              "pricePerUnit": "...",
              "netWeight": "...",
              "description": "簡短描述，包含你發現的關鍵特徵（如貨號、品牌、價格等）"
            }
            """, rawOcrText, merchantRules, historicalContext);

        LocalDateTime structuringStartedAt = LocalDateTime.now();
        long structuringStartedNs = System.nanoTime();
        Optional<String> llmResponse = llmGateway.generate(prompt, null, null);
        long structuringLatencyMs = elapsedMs(structuringStartedNs);
        traceEvents.add(traceEvent(
            llmGateway.getActiveMainProvider(),
            llmGateway.getActiveMainProvider(),
            llmGateway.getMainModel(),
            "LLM_STRUCTURING",
            structuringLatencyMs,
            llmResponse.isEmpty() ? "LLM_EMPTY_RESPONSE" : null,
            llmResponse.isEmpty() ? "LLM returned empty response." : null,
            null,
            0,
            1,
            inputRef,
            null,
            llmResponse.isEmpty() ? "FAILED_RECOGNITION" : "RECOGNIZED",
            structuringStartedAt,
            LocalDateTime.now()
        ));
        if (llmResponse.isEmpty()) {
            log.error("Onboarding failed [AI_RECOGNITION]: LLM returned empty response.");
            throw new AiOnboardingPipelineException("AI_RECOGNITION", "error.ai.recognition_failed", "LLM failed to map entities");
        }

        try {
            String rawResponse = llmResponse.get();
            // Strip <think>...</think> from reasoning models (e.g. qwen3)
            rawResponse = rawResponse.replaceAll("(?s)<think>.*?</think>", "").trim();
            // Ollama sometimes returns HTML error pages (e.g. 502/503) for very large images
            if (rawResponse.startsWith("<") || rawResponse.toLowerCase().startsWith("<!doctype")) {
                log.warn("Phase 1: Vision model returned HTML (likely error page). Treating as unknown product.");
                rawResponse = "{\"name\":\"Unknown Product\"}";
            } else if (rawResponse.contains("```json")) {
                int start = rawResponse.indexOf("```json") + 7;
                int end = rawResponse.indexOf("```", start);
                if (end > start) {
                    rawResponse = rawResponse.substring(start, end).trim();
                }
            } else if (rawResponse.indexOf('{') >= 0 && rawResponse.lastIndexOf('}') >= 0) {
                rawResponse = rawResponse.substring(rawResponse.indexOf('{'), rawResponse.lastIndexOf('}') + 1);
            } else {
                // Vision model returned plain text - extract product name from first line as fallback
                String firstLine = rawResponse.lines().filter(l -> !l.isBlank()).findFirst().orElse("Unknown Product");
                String productName = firstLine.replaceAll("[*#\\-]", "").trim();
                if (productName.length() > 100) productName = productName.substring(0, 100);
                rawResponse = String.format("{\"name\":\"%s\"}", productName.replace("\"", "'"));
                log.warn("Phase 1: Vision model returned plain text, using fallback name: '{}'", productName);
            }
            // Fix LLM-generated price formatting: remove thousand separators inside numbers e.g. 1,768 -> 1768
            rawResponse = rawResponse.replaceAll("(\\d),(\\d{3})", "$1$2");
            // Fix trailing commas before closing brace/bracket (another common LLM mistake)
            rawResponse = rawResponse.replaceAll(",\\s*([}\\]])", "$1");
            Map<String, Object> extracted = objectMapper.readValue(rawResponse, new TypeReference<>() {});
            String name = stringValue(extracted.getOrDefault("name", "Unknown Product"));
            String brand = stringValue(extracted.get("brand"));
            String itemNumber = stringValue(extracted.get("itemNumber"));
            String description = stringValue(extracted.get("description"));
            String categoryHint = firstNonBlank(stringValue(extracted.get("categoryId")), stringValue(extracted.get("category")));
            Double basePrice = extracted.get("basePrice") instanceof Number n ? n.doubleValue()
                    : extracted.get("price") instanceof Number n2 ? n2.doubleValue() : null;
            Map<String, String> extractedAttributes = extractStructuredAttributes(
                extracted,
                name,
                description,
                categoryHint
            );

            // 2. Incremental Matching (itemNumber is the unique key)
            boolean existingFound = false;
            String existingId = null;
            List<AiProductCandidate> similarCandidates = List.of();
            Product verificationProduct = null;
            if (itemNumber != null && !itemNumber.isBlank()) {
                Optional<Product> matched = productRepository.findByItemNumber(itemNumber);
                if (matched.isPresent()) {
                    existingFound = true;
                    verificationProduct = matched.get();
                    existingId = verificationProduct.getId();
                    log.info("Phase 2: MATCH FOUND. Existing Product ID: {}, ItemNumber: {}", existingId, itemNumber);
                } else {
                    log.info("Phase 2: No existing product found for itemNumber: {}", itemNumber);
                }
            } else {
                log.info("Phase 2: Skipping incremental match (no itemNumber detected).");
            }

            // ItemNumber Recovery Logic
            if (itemNumber == null || itemNumber.isBlank() || itemNumber.length() < 5) {
                // 1. Try to find in description (synthesized by LLM/Web)
                if (description != null && !description.isBlank()) {
                    Pattern p = Pattern.compile("(?:编号|货号|SKU|品番|Item No)[:：\\s]*(\\d{5,8})", Pattern.CASE_INSENSITIVE);
                    Matcher m = p.matcher(description);
                    if (m.find()) {
                        itemNumber = m.group(1);
                        log.info("Phase 4: Recovered itemNumber from description: {}", itemNumber);
                    }
                }
                // 2. Try to find in OCR raw text (5-6 digit rule for Costco/Sam's)
                if (itemNumber == null || itemNumber.isBlank() || itemNumber.length() < 5) {
                    Pattern p = Pattern.compile("\\b(\\d{5,6})\\b");
                    Matcher m = p.matcher(rawOcrText);
                    while (m.find()) {
                        String candidate = m.group(1);
                        // Skip if it looks like price
                        if (basePrice == null || !candidate.equals(String.valueOf(basePrice.intValue()))) {
                            itemNumber = candidate;
                            log.info("Phase 4: Recovered itemNumber from raw OCR (5-6 digit fallback): {}", itemNumber);
                            break;
                        }
                    }
                }
            }

            if (verificationProduct == null) {
                SimilarProductSelection similarSelection = findSimilarCandidates(name, brand, itemNumber, categoryHint);
                similarCandidates = similarSelection.candidates();
                verificationProduct = similarSelection.bestProduct();
                if (!similarCandidates.isEmpty()) {
                    log.info("Phase 2: Found {} similar name-level product candidates for '{}'.", similarCandidates.size(), name);
                }
            }

            String recognitionSummary = buildRecognitionSummary(name, brand, itemNumber, basePrice, extracted);
            // 3. Deep Research (Brave via WebSearchService)
            String searchQuery = brand != null ? brand + " " + name : name;
            log.info("Phase 3: Deep Researching for query: '{}'...", searchQuery);
            List<WebSearchService.SearchResult> searchResults;
            LocalDateTime governanceStartedAt = LocalDateTime.now();
            try {
                searchResults = webSearchService.search(searchQuery);
            } catch (Exception ex) {
                log.error("Onboarding failed [WEB_RESEARCH]: web search error for query='{}'", searchQuery, ex);
                throw new AiOnboardingPipelineException("WEB_RESEARCH", "error.ai.web_search_failed", "Web search failed");
            }
            log.info("Phase 3 Success: Found {} search results.", searchResults.size());

            // Build media gallery from search results
            List<MediaGalleryItem> gallery = new ArrayList<>();
            List<String> rejectedSourceDomains = new ArrayList<>();
            LinkedHashSet<String> collectedSourceDomains = new LinkedHashSet<>();
            String researchSnippet = "";
            for (WebSearchService.SearchResult result : searchResults) {
                String sourceUrl = firstNonBlank(result.url(), "");
                String sourceDomain = extractDomain(sourceUrl);
                SourceType sourceType = classifySourceType(sourceDomain, sourceUrl, result.title(), brand);
                if (!sourceType.allowed()) {
                    if (!sourceDomain.isBlank()) {
                        rejectedSourceDomains.add(sourceDomain);
                    }
                    continue;
                }
                collectedSourceDomains.add(sourceDomain);
                if (researchSnippet.isBlank()) {
                    researchSnippet = firstNonBlank(result.snippet(), "");
                }
                for (String imageUrl : result.imageUrls()) {
                    if (!isAllowedImageUrl(imageUrl)) {
                        rejectedSourceDomains.add(extractDomain(imageUrl));
                        continue;
                    }
                    gallery.add(new MediaGalleryItem(
                        imageUrl,
                        MediaType.IMAGE,
                        new HashMap<>(),
                        true,
                        sourceUrl,
                        sourceDomain,
                        sourceType.name()
                    ));
                }
            }
            if (gallery.isEmpty()) {
                traceEvents.add(traceEvent(
                    "source-governance",
                    "source-governance",
                    SOURCE_POLICY_VERSION,
                    "SOURCE_GOVERNANCE",
                    0L,
                    "FAILED_SOURCE_GOVERNANCE",
                    "No usable images after source filtering.",
                    rejectedSourceDomains.isEmpty() ? "source-filter-empty" : String.join(",", rejectedSourceDomains),
                    0,
                    1,
                    inputRef,
                    null,
                    "FAILED_SOURCE_GOVERNANCE",
                    governanceStartedAt,
                    LocalDateTime.now()
                ));
                log.error("Onboarding failed [SOURCE_FILTER]: no usable gallery images after policy filtering. rejectedDomains={}", rejectedSourceDomains);
                throw new AiOnboardingPipelineException("SOURCE_FILTER", "error.ai.source_filter_empty", "No usable images after source filtering");
            }
            traceEvents.add(traceEvent(
                "source-governance",
                "source-governance",
                SOURCE_POLICY_VERSION,
                "SOURCE_GOVERNANCE",
                0L,
                null,
                null,
                rejectedSourceDomains.isEmpty() ? null : "filtered-untrusted-sources",
                0,
                1,
                inputRef,
                null,
                "RECOGNIZED",
                governanceStartedAt,
                LocalDateTime.now()
            ));

            // 4. Knowledge Synthesis (Cloud Core - Qwen)
            LocalDateTime synthesisStartedAt = LocalDateTime.now();
            long synthesisStartedNs = System.nanoTime();
            String finalDescription = synthesize(name, brand, basePrice, rawResponse, researchSnippet);
            traceEvents.add(traceEvent(
                llmGateway.getActiveMainProvider(),
                llmGateway.getActiveMainProvider(),
                llmGateway.getMainModel(),
                "SEMANTIC_SYNTHESIS",
                elapsedMs(synthesisStartedNs),
                finalDescription == null || finalDescription.isBlank() ? "FALLBACK_EMPTY_SYNTHESIS" : null,
                finalDescription == null || finalDescription.isBlank() ? "Synthesis fell back to search snippet." : null,
                finalDescription == null || finalDescription.isBlank() ? "empty-synthesis-fallback" : null,
                0,
                1,
                inputRef,
                null,
                "RECOGNIZED",
                synthesisStartedAt,
                LocalDateTime.now()
            ));

            // Final ItemNumber Mapping Recovery (from synthesis)
            if (itemNumber == null || itemNumber.isBlank()) {
                Pattern p = Pattern.compile("(?:编号|货号|SKU|品番|Item No)[:：\\s]*(\\d{5,8})", Pattern.CASE_INSENSITIVE);
                Matcher m = p.matcher(finalDescription);
                if (m.find()) {
                    itemNumber = m.group(1);
                    log.info("Phase 4: Recovered itemNumber from final synthesis: {}", itemNumber);
                }
            }

            LocalDateTime verificationStartedAt = LocalDateTime.now();
            long verificationStartedNs = System.nanoTime();
            VerificationAssessment verificationAssessment = verifyAgainstExistingProduct(
                verificationProduct,
                name,
                brand,
                finalDescription,
                categoryHint,
                itemNumber,
                basePrice,
                extractedAttributes
            );
            if (!existingFound && verificationProduct != null && verificationAssessment.matchScore() >= SEMANTIC_OVERWRITE_THRESHOLD) {
                existingFound = true;
                existingId = verificationProduct.getId();
            }
            boolean manualReviewRequired = verificationProduct != null && verificationAssessment.matchScore() < SEMANTIC_OVERWRITE_THRESHOLD;
            String recognitionStatus = manualReviewRequired ? STATUS_PENDING_MANUAL_REVIEW : STATUS_PUBLISHABLE;
            traceEvents.add(traceEvent(
                llmGateway.getActiveMainProvider(),
                llmGateway.getActiveMainProvider(),
                llmGateway.getMainModel(),
                "SEMANTIC_COMPARE",
                elapsedMs(verificationStartedNs),
                null,
                null,
                manualReviewRequired ? "low-match-score" : null,
                0,
                1,
                inputRef,
                existingId,
                recognitionStatus,
                verificationStartedAt,
                LocalDateTime.now()
            ));
            List<AiFieldDiff> fieldDiffs = createFieldDiffs(
                verificationProduct,
                name,
                brand,
                finalDescription,
                categoryHint,
                itemNumber,
                basePrice,
                extractedAttributes
            );

            // 5. Map Price Tiers
            List<PriceTier> detectedTiers = new ArrayList<>();
            if (extracted.get("priceTiers") instanceof List<?> tiers) {
                for (Object t : tiers) {
                    if (t instanceof Map<?, ?> m) {
                        detectedTiers.add(new PriceTier(
                            (String) m.get("tierName"),
                            m.get("price") instanceof Number n ? n.doubleValue() : 0.0,
                            "JPY",
                            false
                        ));
                    }
                }
            }

            String resultDecision = existingFound ? "EXISTING_PRODUCT" : "NEW_PRODUCT";
            com.bobbuy.api.AiTraceEvent lastEvent = traceEvents.get(traceEvents.size() - 1);
            AiOnboardingTrace trace = new AiOnboardingTrace(
                firstNonBlank(inputSampleId, ""),
                recognitionSummary,
                List.copyOf(collectedSourceDomains),
                resultDecision,
                null,
                lastEvent.provider(),
                lastEvent.activeProvider(),
                lastEvent.model(),
                lastEvent.stage(),
                lastEvent.latencyMs(),
                lastEvent.errorCode(),
                lastEvent.errorMessage(),
                lastEvent.fallbackReason(),
                lastEvent.retryCount(),
                lastEvent.attemptNo(),
                lastEvent.inputRef(),
                lastEvent.outputRef(),
                recognitionStatus,
                manualReviewRequired,
                traceEvents.get(0).createdAt(),
                LocalDateTime.now(),
                List.copyOf(traceEvents)
            );

            return Optional.of(new AiOnboardingSuggestion(
                name,
                brand,
                finalDescription,
                basePrice,
                categoryHint,
                itemNumber,
                StorageCondition.AMBIENT,
                OrderMethod.DIRECT_BUY,
                gallery,
                new HashMap<>(),
                existingFound,
                existingId,
                similarCandidates,
                ProductVisibility.DRAFTER_ONLY,
                detectedTiers,
                base64Image,
                firstNonBlank(inputSampleId, ""),
                recognitionSummary,
                List.copyOf(collectedSourceDomains),
                rejectedSourceDomains.stream().filter(domain -> domain != null && !domain.isBlank()).distinct().toList(),
                SOURCE_POLICY_VERSION,
                trace,
                verificationAssessment.matchScore(),
                verificationAssessment.reasoning(),
                fieldDiffs,
                buildVerificationTarget(verificationProduct)
            ));

        } catch (AiOnboardingPipelineException e) {
            throw e;
        } catch (Exception e) {
            log.error("Onboarding failed [AI_RECOGNITION]: Failed to parse vision response", e);
            throw new AiOnboardingPipelineException("AI_RECOGNITION", "error.ai.recognition_failed", "Failed to parse vision response");
        }
    }

    private String normalizeBase64Image(String base64Image) {
        if (base64Image == null) {
            return "";
        }
        String trimmed = base64Image.trim();
        int markerIndex = trimmed.indexOf("base64,");
        if (markerIndex >= 0) {
            return trimmed.substring(markerIndex + "base64,".length()).trim();
        }
        return trimmed;
    }

    private long elapsedMs(long startedNs) {
        return Math.max(0L, (System.nanoTime() - startedNs) / 1_000_000L);
    }

    private com.bobbuy.api.AiTraceEvent traceEvent(String provider,
                                                   String activeProvider,
                                                   String model,
                                                   String stage,
                                                   Long latencyMs,
                                                   String errorCode,
                                                   String errorMessage,
                                                   String fallbackReason,
                                                   Integer retryCount,
                                                   Integer attemptNo,
                                                   String inputRef,
                                                   String outputRef,
                                                   String status,
                                                   LocalDateTime createdAt,
                                                   LocalDateTime updatedAt) {
        return new com.bobbuy.api.AiTraceEvent(
            firstNonBlank(provider, "unconfigured"),
            firstNonBlank(activeProvider, "unconfigured"),
            firstNonBlank(model, ""),
            firstNonBlank(stage, ""),
            latencyMs,
            errorCode,
            truncateTraceMessage(errorMessage),
            fallbackReason,
            retryCount,
            attemptNo,
            firstNonBlank(inputRef, ""),
            outputRef,
            firstNonBlank(status, STATUS_PENDING_MANUAL_REVIEW),
            createdAt,
            updatedAt
        );
    }

    private String truncateTraceMessage(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            return null;
        }
        String sanitized = errorMessage.replaceAll("\\s+", " ").trim();
        return sanitized.length() > 180 ? sanitized.substring(0, 180) : sanitized;
    }

    /**
     * Synthesis step: sends Vision extraction and web research snippets to the
     * Cloud Core (Qwen) model to produce a final enriched description.
     */
    private String synthesize(String name, String brand, Double price, String visionJson, String searchSnippet) {
        if (searchSnippet == null || searchSnippet.isBlank()) {
            return "";
        }
        String synthesisPrompt = String.format("""
            你是一个商品信息整合助手。以下是两段关于同一商品的信息：
            1. AI 视觉识别结果：%s
            2. 网页搜索摘要：%s

            请将这两段信息整合为一段简洁专业的中文商品描述（100字以内），只输出描述文本，不要有<think>标签。
            """, visionJson, searchSnippet);

        String result = safeGenerate(synthesisPrompt).orElse(searchSnippet);
        // Strip any <think>...</think> blocks that reasoning models may include
        return result.replaceAll("(?s)<think>.*?</think>", "").trim();
    }

    private VerificationAssessment verifyAgainstExistingProduct(Product verificationProduct,
                                                                String name,
                                                                String brand,
                                                                String description,
                                                                String categoryHint,
                                                                String itemNumber,
                                                                Double basePrice,
                                                                Map<String, String> extractedAttributes) {
        if (verificationProduct == null) {
            return new VerificationAssessment(0d, "未找到可对照的历史档案，建议以新商品流程处理。");
        }
        String newSummary = buildIncomingSummary(name, brand, description, categoryHint, itemNumber, basePrice, extractedAttributes);
        String existingSummary = buildExistingSummary(verificationProduct);
        VerificationAssessment heuristic = buildHeuristicAssessment(
            verificationProduct,
            name,
            brand,
            description,
            categoryHint,
            itemNumber,
            basePrice,
            extractedAttributes
        );
        String prompt = """
            你是商品建档校验助手。请根据以下两个商品语义摘要，输出 JSON：
            {
              "matchScore": 0-100 的数字,
              "reasoning": "一句到两句中文解释"
            }
            需要重点判断是否属于同一商品、是否只是规格变更，以及食品类必须关注净含量与口味。
            历史档案摘要：
            %s

            当前识别摘要：
            %s
            """.formatted(existingSummary, newSummary);
        try {
            Optional<String> llmResponse = safeGenerate(prompt);
            if (llmResponse.isPresent()) {
                Map<String, Object> parsed = objectMapper.readValue(llmResponse.get(), new TypeReference<>() {});
                Double llmScore = parsed.get("matchScore") instanceof Number n ? n.doubleValue() : null;
                String llmReasoning = parsed.get("reasoning") instanceof String text ? text.trim() : "";
                if (llmScore != null && !llmReasoning.isBlank()) {
                    double blended = Math.max(0d, Math.min(100d, (llmScore + heuristic.matchScore()) / 2d));
                    return new VerificationAssessment(blended, llmReasoning);
                }
            }
        } catch (Exception ex) {
            log.debug("Semantic verification fell back to heuristic mode.", ex);
        }
        return heuristic;
    }

    private VerificationAssessment buildHeuristicAssessment(Product verificationProduct,
                                                            String name,
                                                            String brand,
                                                            String description,
                                                            String categoryHint,
                                                            String itemNumber,
                                                            Double basePrice,
                                                            Map<String, String> extractedAttributes) {
        Map<String, String> existingAttributes = extractStructuredAttributes(
            Map.of(),
            selectProductName(verificationProduct),
            firstNonBlank(selectProductDescription(verificationProduct), ""),
            verificationProduct.getCategoryId()
        );
        double score = 0d;
        List<String> reasons = new ArrayList<>();
        String existingName = selectProductName(verificationProduct);
        String existingBrand = firstNonBlank(verificationProduct.getBrand(), "");
        String existingCategory = firstNonBlank(verificationProduct.getCategoryId(), "");
        String existingItemNumber = firstNonBlank(verificationProduct.getItemNumber(), "");

        if (!existingBrand.isBlank() && !firstNonBlank(brand, "").isBlank() && existingBrand.equalsIgnoreCase(brand)) {
            score += 15d;
            reasons.add("品牌一致");
        }
        double coreNameSimilarity = computeSimilarity(stripMeasurementTokens(existingName), stripMeasurementTokens(name));
        if (coreNameSimilarity > 0) {
            score += coreNameSimilarity * 40d;
            if (coreNameSimilarity >= 0.85d) {
                reasons.add("核心品名高度重合");
            } else if (coreNameSimilarity >= 0.5d) {
                reasons.add("核心品名存在较强重合");
            }
        }
        double fullNameSimilarity = computeSimilarity(existingName, name);
        score += fullNameSimilarity * 15d;

        if (!existingCategory.isBlank() && !firstNonBlank(categoryHint, "").isBlank() && existingCategory.equalsIgnoreCase(categoryHint)) {
            score += 10d;
            reasons.add("类目一致");
        }
        if (!existingItemNumber.isBlank() && !firstNonBlank(itemNumber, "").isBlank()) {
            if (existingItemNumber.equalsIgnoreCase(itemNumber)) {
                score += 20d;
                reasons.add("货号完全一致");
            } else if (existingItemNumber.toLowerCase(Locale.ROOT).contains(itemNumber.toLowerCase(Locale.ROOT))
                || itemNumber.toLowerCase(Locale.ROOT).contains(existingItemNumber.toLowerCase(Locale.ROOT))) {
                score += 10d;
                reasons.add("货号片段接近");
            }
        }

        for (String identityField : requiredIdentityFields(firstNonBlank(categoryHint, verificationProduct.getCategoryId()))) {
            String oldValue = firstNonBlank(existingAttributes.get(identityField), "");
            String newValue = firstNonBlank(extractedAttributes.get(identityField), "");
            if (oldValue.isBlank() || newValue.isBlank()) {
                continue;
            }
            if (oldValue.equalsIgnoreCase(newValue)) {
                score += 10d;
                reasons.add(identityFieldLabel(identityField) + "一致");
            } else {
                score -= 5d;
                reasons.add(identityFieldLabel(identityField) + "不一致");
            }
        }

        if (basePrice != null && verificationProduct.getBasePrice() > 0d) {
            double priceGapRatio = Math.abs(basePrice - verificationProduct.getBasePrice()) / verificationProduct.getBasePrice();
            if (priceGapRatio <= 0.1d) {
                score += 4d;
            } else if (priceGapRatio >= 0.5d) {
                score -= 4d;
                reasons.add("价格差异较大");
            }
        }

        if (!firstNonBlank(description, "").isBlank()) {
            score += computeSimilarity(description, selectProductDescription(verificationProduct)) * 6d;
        }

        double finalScore = Math.max(0d, Math.min(100d, score));
        if (!existingItemNumber.isBlank() && existingItemNumber.equalsIgnoreCase(firstNonBlank(itemNumber, ""))) {
            finalScore = Math.max(finalScore, 96d);
        }
        if (reasons.isEmpty()) {
            reasons.add(finalScore >= SEMANTIC_OVERWRITE_THRESHOLD ? "语义指纹整体接近" : "语义指纹差异明显");
        }
        return new VerificationAssessment(finalScore, String.join("，", reasons) + "。");
    }

    private List<AiFieldDiff> createFieldDiffs(Product verificationProduct,
                                               String name,
                                               String brand,
                                               String description,
                                               String categoryHint,
                                               String itemNumber,
                                               Double basePrice,
                                               Map<String, String> extractedAttributes) {
        if (verificationProduct == null) {
            return List.of();
        }
        Map<String, String> existingAttributes = extractStructuredAttributes(
            Map.of(),
            selectProductName(verificationProduct),
            firstNonBlank(selectProductDescription(verificationProduct), ""),
            verificationProduct.getCategoryId()
        );
        List<AiFieldDiff> diffs = new ArrayList<>();
        diffs.add(toFieldDiff("name", "商品名称", selectProductName(verificationProduct), name, false));
        diffs.add(toFieldDiff("brand", "品牌", verificationProduct.getBrand(), brand, false));
        diffs.add(toFieldDiff("categoryId", "类目", verificationProduct.getCategoryId(), categoryHint, false));
        diffs.add(toFieldDiff("itemNumber", "货号", verificationProduct.getItemNumber(), itemNumber, false));
        diffs.add(toFieldDiff("price", "价格", formatNumericValue(verificationProduct.getBasePrice()), formatNumericValue(basePrice), false));
        for (String identityField : requiredIdentityFields(firstNonBlank(categoryHint, verificationProduct.getCategoryId()))) {
            String oldValue = existingAttributes.get(identityField);
            String newValue = extractedAttributes.get(identityField);
            if (firstNonBlank(oldValue, "").isBlank() && firstNonBlank(newValue, "").isBlank()) {
                continue;
            }
            diffs.add(toFieldDiff(identityField, identityFieldLabel(identityField), oldValue, newValue, true));
        }
        if (!firstNonBlank(description, "").isBlank() || !firstNonBlank(selectProductDescription(verificationProduct), "").isBlank()) {
            diffs.add(toFieldDiff("description", "描述", selectProductDescription(verificationProduct), description, false));
        }
        return diffs;
    }

    private AiFieldDiff toFieldDiff(String field, String label, String oldValue, String newValue, boolean identityField) {
        String normalizedOld = firstNonBlank(oldValue, "");
        String normalizedNew = firstNonBlank(newValue, "");
        boolean different = !normalizedOld.equalsIgnoreCase(normalizedNew);
        return new AiFieldDiff(field, label, normalizedOld, normalizedNew, different, identityField);
    }

    private AiVerificationTarget buildVerificationTarget(Product verificationProduct) {
        if (verificationProduct == null) {
            return null;
        }
        return new AiVerificationTarget(
            verificationProduct.getId(),
            selectProductName(verificationProduct),
            verificationProduct.getMediaGallery() == null ? List.of() : List.copyOf(verificationProduct.getMediaGallery())
        );
    }

    private Map<String, String> extractStructuredAttributes(Map<String, Object> extracted, String name, String description, String categoryHint) {
        Map<String, String> attributes = new HashMap<>();
        putIfPresent(attributes, "netContent", firstNonBlank(stringValue(extracted.get("netWeight")), stringValue(extracted.get("netContent"))));
        putIfPresent(attributes, "flavor", extracted.get("flavor"));
        putIfPresent(attributes, "specification", firstNonBlank((String) extracted.get("specification"), (String) extracted.get("size")));
        String combinedText = joinNonBlank(name, description, stringValue(extracted.get("specification")), stringValue(extracted.get("size")));
        Matcher netContentMatcher = NET_CONTENT_PATTERN.matcher(combinedText);
        if (!attributes.containsKey("netContent") && netContentMatcher.find()) {
            attributes.put("netContent", (netContentMatcher.group(1) + netContentMatcher.group(2)).toLowerCase(Locale.ROOT));
        }
        Matcher packMatcher = PACK_PATTERN.matcher(combinedText);
        if (!attributes.containsKey("packSize") && packMatcher.find()) {
            attributes.put("packSize", (packMatcher.group(1) + packMatcher.group(2)).toLowerCase(Locale.ROOT));
        }
        if (isFoodCategory(categoryHint) && !attributes.containsKey("flavor")) {
            String flavor = inferFlavorLikeToken(name);
            if (!flavor.isBlank()) {
                attributes.put("flavor", flavor);
            }
        }
        return attributes;
    }

    private List<String> requiredIdentityFields(String categoryHint) {
        if (isFoodCategory(categoryHint)) {
            return List.of("netContent", "flavor");
        }
        return List.of();
    }

    private boolean isFoodCategory(String categoryHint) {
        String normalized = firstNonBlank(categoryHint, "").trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return false;
        }
        return FOOD_CATEGORY_HINTS.stream().anyMatch(normalized::contains);
    }

    private String identityFieldLabel(String field) {
        return switch (field) {
            case "netContent" -> "净含量";
            case "flavor" -> "口味";
            case "packSize" -> "包装规格";
            default -> field;
        };
    }

    private String buildIncomingSummary(String name,
                                        String brand,
                                        String description,
                                        String categoryHint,
                                        String itemNumber,
                                        Double basePrice,
                                        Map<String, String> extractedAttributes) {
        return """
            品名=%s
            品牌=%s
            类目=%s
            货号=%s
            价格=%s
            描述=%s
            身份属性=%s
            """.formatted(
            firstNonBlank(name, ""),
            firstNonBlank(brand, ""),
            firstNonBlank(categoryHint, ""),
            firstNonBlank(itemNumber, ""),
            formatNumericValue(basePrice),
            firstNonBlank(description, ""),
            extractedAttributes
        );
    }

    private String buildExistingSummary(Product product) {
        Map<String, String> attributes = extractStructuredAttributes(
            Map.of(),
            selectProductName(product),
            firstNonBlank(selectProductDescription(product), ""),
            product.getCategoryId()
        );
        return """
            品名=%s
            品牌=%s
            类目=%s
            货号=%s
            价格=%s
            描述=%s
            身份属性=%s
            """.formatted(
            selectProductName(product),
            firstNonBlank(product.getBrand(), ""),
            firstNonBlank(product.getCategoryId(), ""),
            firstNonBlank(product.getItemNumber(), ""),
            formatNumericValue(product.getBasePrice()),
            firstNonBlank(selectProductDescription(product), ""),
            attributes
        );
    }

    private String selectProductName(Product product) {
        return firstNonBlank(
            product.getName() == null ? null : product.getName().get("zh-CN"),
            product.getName() == null ? null : product.getName().get("ja-JP"),
            product.getName() == null ? null : product.getName().get("en-US"),
            product.getId()
        );
    }

    private String selectProductDescription(Product product) {
        return firstNonBlank(
            product.getDescription() == null ? null : product.getDescription().get("zh-CN"),
            product.getDescription() == null ? null : product.getDescription().get("ja-JP"),
            product.getDescription() == null ? null : product.getDescription().get("en-US"),
            ""
        );
    }

    private void putIfPresent(Map<String, String> attributes, String key, Object value) {
        String normalized = stringValue(value);
        if (!normalized.isBlank()) {
            attributes.put(key, normalized);
        }
    }

    private String stringValue(Object value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value).trim();
    }

    private String inferFlavorLikeToken(String name) {
        List<String> tokens = normalizeOrderedTokens(name);
        List<String> candidates = tokens.stream()
            .filter(token -> token.length() > 1)
            .filter(token -> !NET_CONTENT_PATTERN.matcher(token).matches())
            .filter(token -> FLAVOR_IGNORED_TOKENS.stream().noneMatch(token::equalsIgnoreCase))
            .toList();
        if (candidates.isEmpty()) {
            return "";
        }
        return candidates.get(candidates.size() - 1);
    }

    private List<String> normalizeOrderedTokens(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return List.of(value.toLowerCase(Locale.ROOT).split("[^a-z0-9\\u4e00-\\u9fa5]+")).stream()
            .filter(token -> token != null && !token.isBlank())
            .toList();
    }

    private String stripMeasurementTokens(String value) {
        return NET_CONTENT_PATTERN.matcher(firstNonBlank(value, "")).replaceAll(" ").replaceAll("\\s+", " ").trim();
    }

    private double computeSimilarity(String left, String right) {
        Set<String> leftTokens = normalizeTokens(left);
        Set<String> rightTokens = normalizeTokens(right);
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) {
            return 0d;
        }
        Set<String> intersection = new LinkedHashSet<>(leftTokens);
        intersection.retainAll(rightTokens);
        Set<String> union = new LinkedHashSet<>(leftTokens);
        union.addAll(rightTokens);
        return union.isEmpty() ? 0d : (double) intersection.size() / union.size();
    }

    private String formatNumericValue(Number value) {
        if (value == null) {
            return "";
        }
        double number = value.doubleValue();
        if (number == Math.rint(number)) {
            return String.format(Locale.ROOT, "%.0f", number);
        }
        return String.format(Locale.ROOT, "%.2f", number);
    }

    private Optional<String> safeGenerate(String prompt) {
        Optional<String> response = llmGateway.generate(prompt, null, null);
        return response == null ? Optional.empty() : response;
    }

    private SimilarProductSelection findSimilarCandidates(String name, String brand, String itemNumber, String categoryHint) {
        Set<String> queryTokens = normalizeTokens(joinNonBlank(brand, name, itemNumber, categoryHint));
        if (queryTokens.isEmpty()) {
            return new SimilarProductSelection(null, List.of());
        }
        List<CandidateWithProduct> matches = productRepository.findAll().stream()
            .map(product -> {
                AiProductCandidate candidate = toCandidate(product, queryTokens, itemNumber, categoryHint);
                return candidate == null ? null : new CandidateWithProduct(product, candidate);
            })
            .filter(Objects::nonNull)
            .map(CandidateWithProduct.class::cast)
            .sorted(Comparator.comparingDouble((CandidateWithProduct match) -> match.candidate().score()).reversed())
            .toList();
        Product bestProduct = matches.isEmpty() ? null : matches.get(0).product();
        List<AiProductCandidate> candidates = matches.stream()
            .map(CandidateWithProduct::candidate)
            .limit(3)
            .toList();
        return new SimilarProductSelection(bestProduct, candidates);
    }

    private AiProductCandidate toCandidate(Product product, Set<String> queryTokens, String itemNumber, String categoryHint) {
        Map<String, String> localizedNames = product.getName() == null ? Map.of() : product.getName();
        String displayName = firstNonBlank(
            localizedNames.get("zh-CN"),
            localizedNames.get("ja-JP"),
            localizedNames.get("en-US"),
            product.getId()
        );
        Set<String> productTokens = normalizeTokens(
            joinNonBlank(
                product.getBrand(),
                displayName,
                product.getItemNumber(),
                product.getCategoryId(),
                product.getMerchantSkus() == null ? null : String.join(" ", product.getMerchantSkus().values())
            )
        );
        if (productTokens.isEmpty()) {
            return null;
        }
        long sharedCount = queryTokens.stream().filter(productTokens::contains).count();
        if (sharedCount <= 0) {
            return null;
        }
        long unionCount = queryTokens.size() + productTokens.size() - sharedCount;
        double overlap = unionCount <= 0 ? 0d : (double) sharedCount / (double) unionCount;
        if (overlap < CANDIDATE_OVERLAP_THRESHOLD && sharedCount < 2) {
            return null;
        }
        List<String> signals = new ArrayList<>();
        List<String> matchedFragments = queryTokens.stream()
            .filter(productTokens::contains)
            .limit(MAX_MATCHED_AUDIT_FRAGMENTS)
            .toList();
        double score = sharedCount * SHARED_TOKEN_WEIGHT + overlap * OVERLAP_WEIGHT;
        if (brandExact(queryTokens, product.getBrand())) {
            signals.add("BRAND_EXACT");
            score += BRAND_EXACT_BONUS;
        }
        if (itemNumberFragmentMatch(itemNumber, product.getItemNumber())) {
            signals.add("ITEM_NUMBER_FRAGMENT");
            score += ITEM_NUMBER_FRAGMENT_BONUS;
        }
        if (categoryHint != null
            && !categoryHint.isBlank()
            && product.getCategoryId() != null
            && categoryHint.equalsIgnoreCase(product.getCategoryId())) {
            signals.add("CATEGORY_MATCH");
            score += CATEGORY_MATCH_BONUS;
        }
        if (sharedCount >= 3) {
            signals.add("NAME_STRONG_OVERLAP");
            score += STRONG_NAME_OVERLAP_BONUS;
        } else if (sharedCount >= 2) {
            signals.add("NAME_TOKEN_OVERLAP");
            score += MEDIUM_NAME_OVERLAP_BONUS;
        } else {
            signals.add("NAME_PARTIAL_OVERLAP");
        }
        if (signals.isEmpty() || score < CANDIDATE_MIN_SCORE) {
            return null;
        }
        String primaryReason = signals.contains("BRAND_EXACT") && signals.contains("ITEM_NUMBER_FRAGMENT")
            ? "BRAND_AND_ITEM_NUMBER_FRAGMENT"
            : signals.get(0);
        return new AiProductCandidate(
            product.getId(),
            displayName,
            product.getItemNumber(),
            primaryReason,
            signals,
            score,
            product.getBrand(),
            product.getCategoryId(),
            matchedFragments,
            resolveAliasSources(matchedFragments)
        );
    }

    private Set<String> normalizeTokens(String raw) {
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }
        LinkedHashSet<String> tokens = java.util.Arrays.stream(raw.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+"))
            .map(String::trim)
            .filter(token -> token.length() >= 2)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        LinkedHashSet<String> expanded = new LinkedHashSet<>(tokens);
        for (String token : tokens) {
            for (Map.Entry<String, List<String>> entry : TOKEN_ALIASES.entrySet()) {
                if (entry.getKey().equals(token) || entry.getValue().contains(token)) {
                    expanded.add(entry.getKey());
                    expanded.addAll(entry.getValue());
                }
            }
        }
        return expanded;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private boolean brandExact(Set<String> queryTokens, String brand) {
        if (brand == null || brand.isBlank()) {
            return false;
        }
        Set<String> brandTokens = normalizeTokens(brand);
        return !brandTokens.isEmpty() && queryTokens.containsAll(brandTokens);
    }

    private boolean itemNumberFragmentMatch(String queryItemNumber, String productItemNumber) {
        if (queryItemNumber == null || queryItemNumber.isBlank() || productItemNumber == null || productItemNumber.isBlank()) {
            return false;
        }
        Set<String> queryFragments = normalizeTokens(queryItemNumber.replace('-', ' '));
        Set<String> productFragments = normalizeTokens(productItemNumber.replace('-', ' '));
        return !queryFragments.isEmpty() && queryFragments.stream().anyMatch(productFragments::contains);
    }

    private String joinNonBlank(String... values) {
        return java.util.Arrays.stream(values)
            .filter(Objects::nonNull)
            .filter(value -> !value.isBlank())
            .collect(Collectors.joining(" "));
    }

    private List<String> resolveAliasSources(List<String> matchedFragments) {
        if (matchedFragments == null || matchedFragments.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> aliasSources = new LinkedHashSet<>();
        for (String fragment : matchedFragments) {
            for (Map.Entry<String, List<String>> entry : TOKEN_ALIASES.entrySet()) {
                if (entry.getKey().equals(fragment) || entry.getValue().contains(fragment)) {
                    aliasSources.add(entry.getKey());
                }
            }
        }
        return List.copyOf(aliasSources);
    }

    private String buildRecognitionSummary(String name,
                                           String brand,
                                           String itemNumber,
                                           Double basePrice,
                                           Map<String, Object> extracted) {
        StringBuilder summary = new StringBuilder();
        summary.append("name=").append(firstNonBlank(name, "UNKNOWN"));
        if (brand != null && !brand.isBlank()) {
            summary.append(", brand=").append(brand);
        }
        if (itemNumber != null && !itemNumber.isBlank()) {
            summary.append(", itemNumber=").append(itemNumber);
        }
        if (basePrice != null) {
            summary.append(", price=").append(basePrice);
        }
        Object tiers = extracted.get("priceTiers");
        if (tiers instanceof List<?> list && !list.isEmpty()) {
            summary.append(", tierCount=").append(list.size());
        }
        return summary.toString();
    }

    private String extractDomain(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (host == null) {
                return "";
            }
            return host.toLowerCase(Locale.ROOT);
        } catch (Exception e) {
            return "";
        }
    }

    private boolean isAllowedImageUrl(String imageUrl) {
        String normalized = firstNonBlank(imageUrl, "").toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return false;
        }
        return DENIED_SOURCE_KEYWORDS.stream().noneMatch(normalized::contains)
            && LOW_TRUST_SOURCE_KEYWORDS.stream().noneMatch(normalized::contains);
    }

    private SourceType classifySourceType(String domain, String sourceUrl, String title, String brand) {
        String normalized = (firstNonBlank(domain, "") + " " + firstNonBlank(sourceUrl, "") + " " + firstNonBlank(title, ""))
            .toLowerCase(Locale.ROOT);
        if (DENIED_SOURCE_KEYWORDS.stream().anyMatch(normalized::contains)) {
            return SourceType.DENIED;
        }
        if (LOW_TRUST_SOURCE_KEYWORDS.stream().anyMatch(normalized::contains)) {
            return SourceType.LOW_TRUST_AGGREGATOR;
        }
        String normalizedBrandToken = normalizeBrandToken(brand);
        if (!normalizedBrandToken.isBlank()
            && firstNonBlank(domain, "").toLowerCase(Locale.ROOT).contains(normalizedBrandToken)) {
            return SourceType.BRAND_SITE;
        }
        if (OFFICIAL_STORE_KEYWORDS.stream().anyMatch(normalized::contains)) {
            return SourceType.OFFICIAL_STORE;
        }
        if (TRUSTED_RETAIL_DOMAINS.stream().anyMatch(normalized::contains)) {
            return SourceType.TRUSTED_RETAIL;
        }
        return SourceType.LOW_TRUST_AGGREGATOR;
    }

    private String normalizeBrandToken(String brand) {
        if (brand == null || brand.isBlank()) {
            return "";
        }
        String token = brand.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]", "");
        return token.length() >= 4 ? token : "";
    }

    private enum SourceType {
        OFFICIAL_SITE(true),
        BRAND_SITE(true),
        OFFICIAL_STORE(true),
        TRUSTED_RETAIL(true),
        LOW_TRUST_AGGREGATOR(false),
        DENIED(false);

        private final boolean allowed;

        SourceType(boolean allowed) {
            this.allowed = allowed;
        }

        public boolean allowed() {
            return allowed;
        }
    }

    private record CandidateWithProduct(Product product, AiProductCandidate candidate) {
    }

    private record SimilarProductSelection(Product bestProduct, List<AiProductCandidate> candidates) {
    }

    private record VerificationAssessment(double matchScore, String reasoning) {
    }
}
