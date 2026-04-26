package com.bobbuy.api;

import com.bobbuy.model.OrderMethod;
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
            new AiOnboardingTrace("sample-low-score", "summary", List.of(), "EXISTING_PRODUCT", null),
            45.0,
            "fruit variant mismatch",
            List.of(),
            new AiVerificationTarget("prd-1000", "Acme Apple Box 1kg", List.of())
        );

        MobileProductResponse response = controller.confirmOnboard(suggestion).getBody().getData();

        assertThat(response.getProduct().getId()).isNotEqualTo("prd-1000");
        assertThat(response.getOnboardingTrace().resultDecision()).isEqualTo("NEW_PRODUCT");
    }
}
