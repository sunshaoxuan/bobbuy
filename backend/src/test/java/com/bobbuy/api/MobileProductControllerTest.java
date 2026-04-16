package com.bobbuy.api;

import com.bobbuy.model.ProductPatch;
import com.bobbuy.service.AuditLogService;
import com.bobbuy.service.BobbuyStore;
import com.bobbuy.service.LocalizedJsonbReaderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MobileProductControllerTest {
    private MobileProductController controller;

    @BeforeEach
    void setUp() {
        BobbuyStore store = new BobbuyStore(new AuditLogService());
        store.seed();
        controller = new MobileProductController(store, new LocalizedJsonbReaderService());
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

        MobileProductResponse response = controller.patch("prd-1000", patch, Locale.JAPAN).getBody().getData();

        assertThat(response.getDisplayName()).isEqualTo("抹茶キット");
        assertThat(response.getDisplayDescription()).isEqualTo("京都风味抹茶组合");
        assertThat(response.getProduct().getName()).containsEntry("zh-CN", "抹茶套装");
        assertThat(response.getProduct().getName()).containsEntry("en-US", "Matcha Kit Updated");
        assertThat(response.getProduct().getDescription()).containsEntry("en-US", "Kyoto style matcha combo");
    }
}
