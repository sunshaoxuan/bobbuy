package com.bobbuy.api;

import com.bobbuy.api.AiOnboardingSuggestion;
import com.bobbuy.model.Product;
import com.bobbuy.model.ProductVisibility;
import com.bobbuy.model.Supplier;
import com.bobbuy.repository.ProductRepository;
import com.bobbuy.repository.SupplierRepository;
import com.bobbuy.service.AiOnboardingPipelineException;
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
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

  @Autowired
  private SupplierRepository supplierRepository;

  @BeforeEach
  void setUp() {
    productRepository.deleteAll();
    supplierRepository.deleteAll();
  }

  @Test
  public void testOnboardFromPhotoSuccess() {
    String mockJsonResponse = """
        {
          "name": "Matcha KitKat",
          "brand": "Nestle",
          "price": 15.5,
          "category": "Food"
        }
        """;
    mockPipeline(
        List.of("Nestle Matcha KitKat", "Price 15.5"),
        mockJsonResponse,
        "Deep research snippet description",
        null
    );
    when(webSearchService.search(contains("Matcha KitKat")))
        .thenReturn(List.of(new WebSearchService.SearchResult(
            "Matcha KitKat Original",
            "https://www.costco.com/matcha-kitkat",
            "Deep research snippet description",
            List.of("https://images.costco-static.com/matcha/hd.jpg")
        )));

    Optional<AiOnboardingSuggestion> result = onboardingService.onboardFromPhoto("fake-base64");

    assertTrue(result.isPresent());
    AiOnboardingSuggestion suggestion = result.get();
    assertEquals("Matcha KitKat", suggestion.name());
    assertEquals("Nestle", suggestion.brand());
    assertEquals(15.5, suggestion.price());
    assertEquals("Deep research snippet description", suggestion.description());
    assertEquals(1, suggestion.mediaGallery().size());
    assertEquals("https://images.costco-static.com/matcha/hd.jpg", suggestion.mediaGallery().get(0).getUrl());
    assertEquals("https://www.costco.com/matcha-kitkat", suggestion.mediaGallery().get(0).getSourceUrl());
    assertEquals("www.costco.com", suggestion.mediaGallery().get(0).getSourceDomain());
    assertNotNull(suggestion.trace());
    assertNotNull(suggestion.matchScore());
    assertTrue(suggestion.recognitionSummary().contains("name=Matcha KitKat"));
    assertTrue(suggestion.sourceDomains().contains("www.costco.com"));
    assertEquals("PUBLISHABLE", suggestion.trace().recognitionStatus());
    assertEquals("paddleocr", suggestion.trace().events().stream().findFirst().map(event -> event.model()).orElse(""));
    assertTrue(suggestion.trace().events().stream().anyMatch(event -> "OCR".equals(event.stage())));
    assertTrue(suggestion.trace().events().stream().anyMatch(event -> "LLM_STRUCTURING".equals(event.stage())));
  }

  @Test
  public void testOnboardFromPhotoStripsDataUrlPrefixBeforeVisionCall() {
    mockPipeline(
        List.of("Milk", "Test", "12.0"),
        """
            {
              "name": "Milk",
              "brand": "Test",
              "price": 12.0
            }
            """,
        "trusted source",
        null
    );
    when(webSearchService.search(anyString()))
        .thenReturn(List.of(new WebSearchService.SearchResult(
            "Trusted Retail",
            "https://www.costco.com/milk",
            "trusted source",
            List.of("https://images.costco-static.com/milk/hd.jpg")
        )));

    onboardingService.onboardFromPhoto("data:image/jpeg;base64,ZmFrZS1pbWFnZQ==");

    verify(llmGateway).performOcr("ZmFrZS1pbWFnZQ==");
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
    mockPipeline(
        List.of("BOBBuy 抹茶礼盒 SKU-998"),
        mockJsonResponse,
        "trusted source",
        "{\"matchScore\":55,\"reasoning\":\"同品牌同类目，货号片段接近。\"}"
    );
    when(webSearchService.search(anyString()))
        .thenReturn(List.of(new WebSearchService.SearchResult(
            "Trusted Retail",
            "https://www.costco.com/item/sku-9",
            "trusted source",
            List.of("https://images.examplecdn.com/sku-9.jpg")
        )));

    AiOnboardingSuggestion suggestion = onboardingService.onboardFromPhoto("fake-base64").orElseThrow();

    assertFalse(suggestion.existingProductFound());
    assertEquals(2, suggestion.similarProductCandidates().size());
    assertEquals("prd-strong", suggestion.similarProductCandidates().get(0).productId());
    assertTrue(suggestion.similarProductCandidates().get(0).matchSignals().contains("BRAND_EXACT"));
    assertTrue(suggestion.similarProductCandidates().get(0).matchSignals().contains("ITEM_NUMBER_FRAGMENT"));
    assertEquals("BOBBuy", suggestion.similarProductCandidates().get(0).brand());
    assertEquals("tea", suggestion.similarProductCandidates().get(0).categoryId());
    assertFalse(suggestion.similarProductCandidates().get(0).matchedFragments().isEmpty());
    assertTrue(suggestion.similarProductCandidates().get(0).score() > suggestion.similarProductCandidates().get(1).score());
  }

  @Test
  public void testOnboardFromPhotoRejectsDeniedSources() {
    mockPipeline(
        List.of("Milk", "Test", "12.0"),
        """
            {
              "name": "Milk",
              "brand": "Test",
              "price": 12.0
            }
            """,
        "bad",
        null
    );
    when(webSearchService.search(anyString()))
        .thenReturn(List.of(new WebSearchService.SearchResult(
            "Bad Source",
            "https://www.xiaohongshu.com/item/123",
            "bad",
            List.of("https://www.xiaohongshu.com/image/1.jpg")
        )));

    AiOnboardingPipelineException ex = assertThrows(AiOnboardingPipelineException.class,
        () -> onboardingService.onboardFromPhoto("fake-base64"));
    assertEquals("SOURCE_FILTER", ex.getStage());
    assertEquals("error.ai.source_filter_empty", ex.getMessageKey());
  }

  @Test
  public void testSemanticVerificationDetectsSpecChangeAndBuildsDiffs() {
    Product existing = new Product();
    existing.setId("prd-semantic");
    existing.setName(Map.of("zh-CN", "Acme Chips 100g"));
    existing.setDescription(Map.of("zh-CN", "Acme Chips 100g classic"));
    existing.setBrand("Acme");
    existing.setCategoryId("Food");
    existing.setBasePrice(10.0);
    existing.setVisibilityStatus(ProductVisibility.PUBLIC);
    productRepository.save(existing);

    String mockJsonResponse = """
        {
          "name": "Acme Chips 120g",
          "brand": "Acme",
          "price": 12,
          "category": "Food"
        }
        """;
    mockPipeline(
        List.of("Acme Chips 120g", "Acme", "12"),
        mockJsonResponse,
        "Acme 薯片 120g 新包装，规格从 100g 调整到 120g。",
        "{\"matchScore\":82,\"reasoning\":\"同品牌同品名，主要差异是净含量从100g升级到120g。\"}"
    );
    when(webSearchService.search(anyString()))
        .thenReturn(List.of(new WebSearchService.SearchResult(
            "Trusted Retail",
            "https://www.costco.com/item/acme-chips",
            "trusted source",
            List.of("https://images.examplecdn.com/acme-chips.jpg")
        )));

    AiOnboardingSuggestion suggestion = onboardingService.onboardFromPhoto("fake-base64").orElseThrow();

    assertEquals("prd-semantic", suggestion.existingProductId());
    assertTrue(suggestion.existingProductFound());
    assertNotNull(suggestion.verificationTarget());
    assertEquals("prd-semantic", suggestion.verificationTarget().productId());
    assertTrue(suggestion.matchScore() >= 70.0);
    assertTrue(suggestion.matchScore() <= 90.0);
    assertNotNull(suggestion.semanticReasoning());
    assertTrue(suggestion.fieldDiffs().stream().anyMatch(diff ->
        "netContent".equals(diff.field())
            && "100g".equalsIgnoreCase(diff.oldValue())
            && "120g".equalsIgnoreCase(diff.newValue())
            && diff.different()));
  }

  @Test
  public void testOnboardFromPhotoReportsOcrFailureWithoutCallingLlm() {
    when(llmGateway.performOcr(anyString())).thenReturn(List.of());

    AiOnboardingPipelineException ex = assertThrows(AiOnboardingPipelineException.class,
        () -> onboardingService.onboardFromPhoto("fake-base64"));

    assertEquals("OCR_FAILURE", ex.getStage());
    assertEquals("error.ai.ocr_failed", ex.getMessageKey());
    assertEquals(0, productRepository.count());
    verify(llmGateway, never()).generate(anyString(), any(), any());
  }

  @Test
  public void testSupplierRulesAreInjectedIntoOcrFirstPrompt() {
    Supplier supplier = new Supplier();
    supplier.setId("sup-rule");
    supplier.setName(Map.of("zh-CN", "规则供货商"));
    supplier.setOnboardingRules(Map.of("itemNumberPattern", "SKU-\\d{3}", "preferredBrand", "Nestle"));
    supplierRepository.save(supplier);

    String extractionJson = """
        {
          "name": "Matcha KitKat",
          "brand": "Nestle",
          "itemNumber": "SKU-123",
          "price": 15.5,
          "category": "Food"
        }
        """;
    AtomicReference<String> capturedPrompt = new AtomicReference<>();
    when(llmGateway.performOcr(anyString())).thenReturn(List.of("Nestle Matcha KitKat SKU-123"));
    when(llmGateway.generate(anyString(), org.mockito.ArgumentMatchers.<String>isNull(), org.mockito.ArgumentMatchers.<List<String>>isNull()))
        .thenAnswer(invocation -> {
      String prompt = invocation.getArgument(0, String.class);
      if (prompt.contains("=== 原始OCR文本 ===")) {
        capturedPrompt.set(prompt);
        return Optional.of(extractionJson);
      }
      if (prompt.contains("商品信息整合助手")) {
        return Optional.of("trusted source");
      }
      return Optional.empty();
    });
    when(webSearchService.search(anyString()))
        .thenReturn(List.of(new WebSearchService.SearchResult(
            "Trusted Retail",
            "https://www.costco.com/matcha-kitkat",
            "trusted source",
            List.of("https://images.costco-static.com/matcha/hd.jpg")
        )));

    AiOnboardingSuggestion suggestion = onboardingService.onboardFromPhoto("fake-base64").orElseThrow();

    assertEquals("SKU-123", suggestion.itemNumber());
    assertNotNull(capturedPrompt.get());
    assertTrue(capturedPrompt.get().contains("itemNumberPattern"));
    assertTrue(capturedPrompt.get().contains("SKU-\\d{3}"));
    assertTrue(capturedPrompt.get().contains("preferredBrand"));
  }

  private void mockPipeline(List<String> ocrLines,
                            String extractionJson,
                            String synthesisText,
                            String verificationJson) {
    when(llmGateway.performOcr(anyString())).thenReturn(ocrLines);
    // OCR-first default tests only exercise text prompts, so targetModel and image payload stay null.
    when(llmGateway.generate(anyString(), org.mockito.ArgumentMatchers.<String>isNull(), org.mockito.ArgumentMatchers.<List<String>>isNull()))
        .thenAnswer(invocation -> {
      String prompt = invocation.getArgument(0, String.class);
      if (prompt.contains("=== 原始OCR文本 ===")) {
        return Optional.of(extractionJson);
      }
      if (prompt.contains("商品信息整合助手")) {
        return Optional.of(synthesisText);
      }
      if (prompt.contains("\"matchScore\": 0-100 的数字")) {
        return verificationJson == null ? Optional.empty() : Optional.of(verificationJson);
      }
      return Optional.empty();
    });
  }
}
