package com.bobbuy.api;

import com.bobbuy.model.OrderMethod;
import com.bobbuy.model.Product;
import com.bobbuy.model.ProductVisibility;
import com.bobbuy.model.StorageCondition;
import com.bobbuy.service.BobbuyStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AiAgentControllerTest {

    @Autowired
    private BobbuyStore store;

    @Autowired
    private AiAgentController controller;

    @BeforeEach
    void setUp() {
        store.seed();
    }

    @Test
    void confirmOnboardCreatesNewProductWhenMatchScoreIsTooLow() {
        AiOnboardingSuggestion suggestion = new AiOnboardingSuggestion(
            "Acme Orange Box 1kg",
            "Acme",
            "orange box",
            10.0,
            "Food",
            null,
            StorageCondition.AMBIENT,
            OrderMethod.DIRECT_BUY,
            List.of(),
            Map.of(),
            true,
            "prd-1000",
            List.of(),
            ProductVisibility.DRAFTER_ONLY,
            List.of(),
            null,
            "sample-low-score",
            "summary",
            List.of(),
            List.of(),
            "V23",
            new AiOnboardingTrace("sample-low-score", "summary", List.of(), "EXISTING_PRODUCT", null,
                "ollama", "ollama", "qwen", "SEMANTIC_COMPARE", 10L, null, null, "low-match-score", 0, 1,
                "sample-low-score", null, "PENDING_MANUAL_REVIEW", true, null, null, List.of()),
            45.0,
            "fruit variant mismatch",
            List.of(),
            new AiVerificationTarget("prd-1000", "Acme Apple Box 1kg", List.of())
        );

        MobileProductResponse response = controller.confirmOnboard(suggestion).getBody().getData();

        assertThat(response.getProduct().getId()).isNotEqualTo("prd-1000");
        assertThat(response.getOnboardingTrace().resultDecision()).isEqualTo("NEW_PRODUCT");
        assertThat(response.getProduct().getVisibilityStatus()).isEqualTo(ProductVisibility.DRAFTER_ONLY);
    }

    @Test
    void confirmOnboardReusesExistingProductWhenRetryFindsSameItemNumber() {
        Product existing = new Product();
        existing.setId("prd-retry");
        existing.setName(Map.of("zh-CN", "Retry Matcha Kit"));
        existing.setDescription(Map.of("zh-CN", "retry"));
        existing.setBrand("BOBBuy Select");
        existing.setBasePrice(32.5);
        existing.setItemNumber("SKU-RETRY-1");
        existing.setVisibilityStatus(ProductVisibility.DRAFTER_ONLY);
        existing = store.createProduct(existing);
        AiOnboardingSuggestion suggestion = new AiOnboardingSuggestion(
            "Retry Matcha Kit",
            "BOBBuy Select",
            "retry",
            33.0,
            "cat-1000",
            existing.getItemNumber(),
            StorageCondition.AMBIENT,
            OrderMethod.DIRECT_BUY,
            List.of(),
            Map.of(),
            false,
            null,
            List.of(),
            ProductVisibility.DRAFTER_ONLY,
            List.of(),
            null,
            "sample-retry",
            "summary",
            List.of(),
            List.of(),
            "V23",
            new AiOnboardingTrace("sample-retry", "summary", List.of(), "NEW_PRODUCT", null,
                "ollama", "ollama", "qwen", "SEMANTIC_COMPARE", 10L, null, null, null, 0, 1,
                "sample-retry", null, "PUBLISHABLE", false, null, null, List.of()),
            90.0,
            "same product",
            List.of(),
            null
        );

        MobileProductResponse response = controller.confirmOnboard(suggestion).getBody().getData();

        assertThat(response.getProduct().getId()).isEqualTo(existing.getId());
        assertThat(response.getOnboardingTrace().resultDecision()).isEqualTo("EXISTING_PRODUCT");
    }
}
