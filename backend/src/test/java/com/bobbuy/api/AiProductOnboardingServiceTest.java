package com.bobbuy.api;

import com.bobbuy.api.AiOnboardingSuggestion;
import com.bobbuy.service.AiProductOnboardingService;
import com.bobbuy.service.LlmGateway;
import com.bobbuy.service.WebSearchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
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
}
