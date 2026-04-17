package com.bobbuy.api;

import com.bobbuy.model.ProductPatch;
import com.bobbuy.service.BobbuyStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class MobileProductControllerTest {
    @Autowired
    private BobbuyStore store;
    @Autowired
    private MobileProductController controller;

    @BeforeEach
    void setUp() {
        store.seed();
    }

    @Test
    void getUsesFallbackLocaleForDisplayFields() {
        MobileProductResponse response = controller.get("prd-1000", Locale.JAPAN).getBody().getData();

        assertThat(response.getDisplayName()).isEqualTo("抹茶套装");
        assertThat(response.getDisplayDescription()).isEqualTo("京都风味抹茶组合");
    }

    @Test
    void patchMergesLocalizedFieldsIncrementally() {
        ProductPatch patch = new ProductPatch();
        patch.setName(Map.of("ja-JP", "抹茶キット", "en-US", "Matcha Kit Updated"));
        patch.setDescription(Map.of("en-US", "Kyoto style matcha combo"));
        patch.setMerchantSkus(Map.of("sup-1000", "TOKYO-MATCHA-NEW"));

        MobileProductResponse response = controller.patch("prd-1000", patch, Locale.JAPAN).getBody().getData();

        assertThat(response.getDisplayName()).isEqualTo("抹茶キット");
        assertThat(response.getDisplayDescription()).isEqualTo("京都风味抹茶组合");
        assertThat(response.getProduct().getName()).containsEntry("zh-CN", "抹茶套装");
        assertThat(response.getProduct().getName()).containsEntry("en-US", "Matcha Kit Updated");
        assertThat(response.getProduct().getDescription()).containsEntry("en-US", "Kyoto style matcha combo");
        assertThat(response.getProduct().getMerchantSkus()).containsEntry("sup-1000", "TOKYO-MATCHA-NEW");
    }
}
