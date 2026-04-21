package com.bobbuy.api;

import com.bobbuy.api.AiOnboardingSuggestion;
import com.bobbuy.model.Product;
import com.bobbuy.model.ProductVisibility;
import com.bobbuy.repository.ProductRepository;
import com.bobbuy.service.AiProductOnboardingService;
import com.bobbuy.service.AiSearchService;
import com.bobbuy.service.LlmGateway;
import com.bobbuy.service.WebSearchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
public class AiProductOnboardingServiceTest {

  @Autowired
  private AiProductOnboardingService onboardingService;

  @MockBean
  private LlmGateway llmGateway;

  @MockBean
  private WebSearchService webSearchService;

  @MockBean
  private AiSearchService aiSearchService;

  @Autowired
  private ProductRepository productRepository;

  @BeforeEach
  void setUp() {
    productRepository.deleteAll();
  }

  @Test
  public void testOnboardFromPhotoSuccess() {
    // Mock VLM extraction
    String mockJsonResponse = """
        {
          "name": "Matcha KitKat",
          "brand": "Nestle",
          "price": 15.5,
          "category": "Food"
        }
        """;
    when(llmGateway.generate(anyString(), eq("llava"), anyList()))
        .thenReturn(Optional.of(mockJsonResponse));

    // Mock Web Research
    when(webSearchService.search(contains("Matcha KitKat")))
        .thenReturn(List.of(new WebSearchService.SearchResult(
            "Matcha KitKat Original",
            "http://example.com",
            "Deep research snippet description",
            List.of("http://example.com/hd.jpg")
        )));

    Optional<AiOnboardingSuggestion> result = onboardingService.onboardFromPhoto("fake-base64");

    assertTrue(result.isPresent());
    AiOnboardingSuggestion suggestion = result.get();
    assertEquals("Matcha KitKat", suggestion.name());
    assertEquals("Nestle", suggestion.brand());
    assertEquals(15.5, suggestion.price());
    assertEquals("Deep research snippet description", suggestion.description());
    assertEquals(1, suggestion.mediaGallery().size());
    assertEquals("http://example.com/hd.jpg", suggestion.mediaGallery().get(0).getUrl());
  }

  @Test
  public void testNameLevelCandidatesPreferBrandAndItemNumberSignals() {
    Product weakerMatch = new Product();
    weakerMatch.setId("prd-weak");
    weakerMatch.setName(Map.of("zh-CN", "抹茶礼盒"));
    weakerMatch.setBrand("Other");
    weakerMatch.setCategoryId("tea");
    weakerMatch.setItemNumber("A-111");
    weakerMatch.setVisibilityStatus(ProductVisibility.PUBLIC);

    Product strongerMatch = new Product();
    strongerMatch.setId("prd-strong");
    strongerMatch.setName(Map.of("zh-CN", "BOBBuy 抹茶礼盒大包装"));
    strongerMatch.setBrand("BOBBuy");
    strongerMatch.setCategoryId("tea");
    strongerMatch.setItemNumber("SKU-998");
    strongerMatch.setVisibilityStatus(ProductVisibility.PUBLIC);

    productRepository.saveAll(List.of(weakerMatch, strongerMatch));

    String mockJsonResponse = """
        {
          "name": "抹茶礼盒",
          "brand": "BOBBuy",
          "price": 88,
          "itemNumber": "SKU-9",
          "categoryId": "tea"
        }
        """;
    when(llmGateway.generate(anyString(), eq("llava"), anyList()))
        .thenReturn(Optional.of(mockJsonResponse));
    when(webSearchService.search(anyString())).thenReturn(List.of());

    AiOnboardingSuggestion suggestion = onboardingService.onboardFromPhoto("fake-base64").orElseThrow();

    assertFalse(suggestion.existingProductFound());
    assertEquals(2, suggestion.similarProductCandidates().size());
    assertEquals("prd-strong", suggestion.similarProductCandidates().get(0).productId());
    assertTrue(suggestion.similarProductCandidates().get(0).matchSignals().contains("BRAND_EXACT"));
    assertTrue(suggestion.similarProductCandidates().get(0).matchSignals().contains("ITEM_NUMBER_FRAGMENT"));
    assertTrue(suggestion.similarProductCandidates().get(0).score() > suggestion.similarProductCandidates().get(1).score());
  }
}
