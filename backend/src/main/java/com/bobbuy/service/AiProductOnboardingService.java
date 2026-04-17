package com.bobbuy.service;

import com.bobbuy.api.AiOnboardingSuggestion;
import com.bobbuy.model.MediaGalleryItem;
import com.bobbuy.model.OrderMethod;
import com.bobbuy.model.StorageCondition;
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
  private final WebSearchService webSearchService;
  private final ObjectMapper objectMapper;

  public AiProductOnboardingService(LlmGateway llmGateway, WebSearchService webSearchService, ObjectMapper objectMapper) {
    this.llmGateway = llmGateway;
    this.webSearchService = webSearchService;
    this.objectMapper = objectMapper;
  }

  public Optional<AiOnboardingSuggestion> onboardFromPhoto(String base64Image) {
    // 1. Vision Extract
    String prompt = """
        请分析这张商品实拍图（货架图），提取商品的基本信息并以JSON格式输出。
        需要包含以下字段：
        - name: 商品名称（尽量准确）
        - brand: 品牌（如有）
        - price: 价格（数字，如有免税提示请解析出含税价）
        - category: 品类（如：食品, 服装, 日用品）
        - itemNumber: 货号/货架号（如有）
        
        只输出JSON对象，不要有额外解释。
        """;

    Optional<String> visionResponse = llmGateway.generate(prompt, "llava", List.of(base64Image));
    if (visionResponse.isEmpty()) {
      return Optional.empty();
    }

    try {
      Map<String, Object> extracted = objectMapper.readValue(visionResponse.get(), new TypeReference<>() {
      });
      String name = (String) extracted.getOrDefault("name", "Unknown Product");
      String brand = (String) extracted.get("brand");
      String itemNumber = (String) extracted.get("itemNumber");
      Double price = extracted.get("price") instanceof Number n ? n.doubleValue() : null;

      // 2. Deep Research
      String searchQuery = brand != null ? brand + " " + name : name;
      List<WebSearchService.SearchResult> searchResults = webSearchService.search(searchQuery);

      String description = "";
      List<MediaGalleryItem> gallery = new ArrayList<>();
      Map<String, String> attributes = new HashMap<>();

      if (!searchResults.isEmpty()) {
        WebSearchService.SearchResult topResult = searchResults.get(0);
        description = topResult.snippet();
        for (String imgUrl : topResult.imageUrls()) {
          Map<String, String> titleMap = new HashMap<>();
          titleMap.put("zh-CN", "商品官方图");
          titleMap.put("en-US", "Official Product Image");
          gallery.add(new MediaGalleryItem(imgUrl, com.bobbuy.model.MediaType.IMAGE, titleMap));
        }
      }

      // 3. Final Consolidation
      return Optional.of(new AiOnboardingSuggestion(
          name,
          brand,
          description,
          price,
          null, // Category mapping would need more logic
          itemNumber,
          StorageCondition.AMBIENT, // Default to Ambient
          OrderMethod.DIRECT_BUY,   // Default to Direct Buy
          gallery,
          attributes
      ));

    } catch (Exception e) {
      log.error("Failed to parse vision response", e);
      return Optional.empty();
    }
  }
}
