package com.bobbuy.service;

import com.bobbuy.api.AiOnboardingSuggestion;
import com.bobbuy.model.MediaGalleryItem;
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

@Service
public class AiProductOnboardingService {
    private static final Logger log = LoggerFactory.getLogger(AiProductOnboardingService.class);

    private final LlmGateway llmGateway;
    private final AiSearchService aiSearchService;
    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;

    public AiProductOnboardingService(LlmGateway llmGateway, 
                                     AiSearchService aiSearchService, 
                                     ProductRepository productRepository, 
                                     ObjectMapper objectMapper) {
        this.llmGateway = llmGateway;
        this.aiSearchService = aiSearchService;
        this.productRepository = productRepository;
        this.objectMapper = objectMapper;
    }

    public Optional<AiOnboardingSuggestion> onboardFromPhoto(String base64Image) {
        // 1. Vision Extract (Enhanced for Price Tiers)
        String prompt = """
            请分析这张商品实拍图（货架图），提取商品信息并以JSON格式输出。
            特别注意提取“会员价”、“优惠价”以及价格下方的“货号/Item Number”。
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
            Double basePrice = extracted.get("basePrice") instanceof Number n ? n.doubleValue() : null;

            // 2. Matching Logic (Incremental Update Detection)
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

            // 3. Deep Research (Secure via AiSearchService)
            String searchQuery = brand != null ? brand + " " + name : name;
            String searchResults = aiSearchService.search(searchQuery);

            List<MediaGalleryItem> gallery = new ArrayList<>();
            // In a real implementation, we would parse the searchResults for images and descriptions
            
            // 4. Map Tiers
            List<PriceTier> detectedTiers = new ArrayList<>();
            if (extracted.get("priceTiers") instanceof List<?> tiers) {
                for (Object t : tiers) {
                    if (t instanceof Map<?, ?> m) {
                        detectedTiers.add(new PriceTier(
                            (String) m.get("tierName"),
                            ((Number) m.get("price")).doubleValue(),
                            "JPY", // Default currency
                            false
                        ));
                    }
                }
            }

            return Optional.of(new AiOnboardingSuggestion(
                name,
                brand,
                searchResults, // Using search results as description for enrichment
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
}
