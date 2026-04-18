package com.bobbuy.service;

import com.bobbuy.api.AiOnboardingSuggestion;
import com.bobbuy.model.MediaGalleryItem;
import com.bobbuy.model.MediaType;
import com.bobbuy.model.OrderMethod;
import com.bobbuy.model.PriceTier;
import com.bobbuy.model.Product;
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
import java.util.UUID;

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
        // 1. Vision Extract (Edge Node - Llava)
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
            return Optional.empty();
        }

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
            if (itemNumber != null && !itemNumber.isBlank()) {
                Optional<Product> matched = productRepository.findByItemNumber(itemNumber);
                if (matched.isPresent()) {
                    existingFound = true;
                    existingId = matched.get().getId();
                    log.info("Matching existing product found for itemNumber: {}", itemNumber);
                }
            }

            // 3. Deep Research (Brave via WebSearchService)
            String searchQuery = brand != null ? brand + " " + name : name;
            List<WebSearchService.SearchResult> searchResults = webSearchService.search(searchQuery);

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
                detectedTiers
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
}
