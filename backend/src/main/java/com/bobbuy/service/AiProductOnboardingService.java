package com.bobbuy.service;

import com.bobbuy.api.AiOnboardingSuggestion;
import com.bobbuy.api.AiProductCandidate;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AiProductOnboardingService {
    private static final Logger log = LoggerFactory.getLogger(AiProductOnboardingService.class);

    private final LlmGateway llmGateway;
    private final AiSearchService aiSearchService;
    private final WebSearchService webSearchService;
    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;

    public AiProductOnboardingService(LlmGateway llmGateway,
                                     AiSearchService aiSearchService,
                                     WebSearchService webSearchService,
                                     ProductRepository productRepository,
                                     ObjectMapper objectMapper) {
        this.llmGateway = llmGateway;
        this.aiSearchService = aiSearchService;
        this.webSearchService = webSearchService;
        this.productRepository = productRepository;
        this.objectMapper = objectMapper;
    }

    public Optional<AiOnboardingSuggestion> onboardFromPhoto(String base64Image) {
        log.info("Starting AI onboarding from photo. Base64 length: {}", base64Image != null ? base64Image.length() : 0);
        if (base64Image == null || base64Image.isBlank()) {
            log.warn("Onboarding failed: Empty image data provided.");
            return Optional.empty();
        }
        // 1. Vision Extract (Edge Node - Llava)
        log.info("Phase 1: Dispatching to Vision Model (Llava)...");
        String prompt = """
            请分析这张商品实拍图（货架图），提取商品信息并以JSON格式输出。
            特别注意提取"会员价"、"优惠价"以及价格下方的"货号/Item Number"。
            需要包含以下字段：
            - name: 商品名称
            - brand: 品牌
            - basePrice: 标准零售价（数字）
            - itemNumber: 货号（如有）
            - priceTiers: 价格数组，每个对象含 tierName (如 "Member", "Sale"), price, note

            只输出JSON对象，不要有额外解释。
            """;

        Optional<String> visionResponse = llmGateway.generate(prompt, "llava", List.of(base64Image));
        if (visionResponse.isEmpty()) {
            log.error("Phase 1 Failed: Vision model returned empty response.");
            return Optional.empty();
        }
        log.info("Phase 1 Success: Vision extraction complete. Raw JSON length: {}", visionResponse.get().length());

        try {
            Map<String, Object> extracted = objectMapper.readValue(visionResponse.get(), new TypeReference<>() {});
            String name = (String) extracted.getOrDefault("name", "Unknown Product");
            String brand = (String) extracted.get("brand");
            String itemNumber = (String) extracted.get("itemNumber");
            Double basePrice = extracted.get("basePrice") instanceof Number n ? n.doubleValue()
                    : extracted.get("price") instanceof Number n2 ? n2.doubleValue() : null;

            // 2. Incremental Matching (itemNumber is the unique key)
            boolean existingFound = false;
            String existingId = null;
            List<AiProductCandidate> similarCandidates = List.of();
            if (itemNumber != null && !itemNumber.isBlank()) {
                Optional<Product> matched = productRepository.findByItemNumber(itemNumber);
                if (matched.isPresent()) {
                    existingFound = true;
                    existingId = matched.get().getId();
                    log.info("Phase 2: MATCH FOUND. Existing Product ID: {}, ItemNumber: {}", existingId, itemNumber);
                } else {
                    log.info("Phase 2: No existing product found for itemNumber: {}", itemNumber);
                }
            } else {
                log.info("Phase 2: Skipping incremental match (no itemNumber detected).");
            }
            if (!existingFound) {
                similarCandidates = findSimilarCandidates(name, brand);
                if (!similarCandidates.isEmpty()) {
                    log.info("Phase 2: Found {} similar name-level product candidates for '{}'.", similarCandidates.size(), name);
                }
            }

            // 3. Deep Research (Brave via WebSearchService)
            String searchQuery = brand != null ? brand + " " + name : name;
            log.info("Phase 3: Deep Researching for query: '{}'...", searchQuery);
            List<WebSearchService.SearchResult> searchResults = webSearchService.search(searchQuery);
            log.info("Phase 3 Success: Found {} search results.", searchResults.size());

            // Build media gallery from search results
            List<MediaGalleryItem> gallery = new ArrayList<>();
            String researchSnippet = "";
            if (!searchResults.isEmpty()) {
                WebSearchService.SearchResult topResult = searchResults.get(0);
                researchSnippet = topResult.snippet();
                for (String imageUrl : topResult.imageUrls()) {
                    gallery.add(new MediaGalleryItem(imageUrl, MediaType.IMAGE, new HashMap<>()));
                }
            }

            // 4. Knowledge Synthesis (Cloud Core - Qwen)
            String description = synthesize(name, brand, basePrice, visionResponse.get(), researchSnippet);

            // 5. Map Price Tiers
            List<PriceTier> detectedTiers = new ArrayList<>();
            if (extracted.get("priceTiers") instanceof List<?> tiers) {
                for (Object t : tiers) {
                    if (t instanceof Map<?, ?> m) {
                        detectedTiers.add(new PriceTier(
                            (String) m.get("tierName"),
                            ((Number) m.get("price")).doubleValue(),
                            "JPY",
                            false
                        ));
                    }
                }
            }

            log.info("Final Synthesis: Suggestion generated successfully for product: '{}'", name);
            if (detectedTiers.size() > 0) {
                log.info("Price Tiers Extracted: {} tiers found.", detectedTiers.size());
            }

            return Optional.of(new AiOnboardingSuggestion(
                name,
                brand,
                description,
                basePrice,
                null,
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
                base64Image
            ));

        } catch (Exception e) {
            log.error("Failed to parse vision response", e);
            return Optional.empty();
        }
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

            请将这两段信息整合为一段简洁专业的中文商品描述（100字以内），只输出描述文本。
            """, visionJson, searchSnippet);

        return llmGateway.generate(synthesisPrompt, null, null)
                .orElse(searchSnippet);
    }

    private List<AiProductCandidate> findSimilarCandidates(String name, String brand) {
        Set<String> queryTokens = normalizeTokens(brand + " " + name);
        if (queryTokens.isEmpty()) {
            return List.of();
        }
        return productRepository.findAll().stream()
            .map(product -> toCandidate(product, queryTokens))
            .filter(Objects::nonNull)
            .limit(3)
            .toList();
    }

    private AiProductCandidate toCandidate(Product product, Set<String> queryTokens) {
        String displayName = firstNonBlank(
            product.getName().get("zh-CN"),
            product.getName().get("ja-JP"),
            product.getName().get("en-US"),
            product.getId()
        );
        Set<String> productTokens = normalizeTokens(product.getBrand() + " " + displayName);
        if (productTokens.isEmpty()) {
            return null;
        }
        long sharedCount = queryTokens.stream().filter(productTokens::contains).count();
        if (sharedCount <= 0) {
            return null;
        }
        double overlap = (double) sharedCount / (double) Math.max(queryTokens.size(), productTokens.size());
        if (overlap < 0.34d && sharedCount < 2) {
            return null;
        }
        return new AiProductCandidate(
            product.getId(),
            displayName,
            product.getItemNumber(),
            sharedCount >= 2 ? "NAME_TOKEN_OVERLAP" : "NAME_PARTIAL_OVERLAP"
        );
    }

    private Set<String> normalizeTokens(String raw) {
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }
        return java.util.Arrays.stream(raw.toLowerCase(Locale.ROOT).split("[^\\p{IsAlphabetic}\\p{IsDigit}]+"))
            .map(String::trim)
            .filter(token -> token.length() >= 2)
            .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
