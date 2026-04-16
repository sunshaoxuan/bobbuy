package com.bobbuy.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LocalizedJsonbReaderServiceTest {
    private LocalizedJsonbReaderService service;

    @BeforeEach
    void setUp() {
        service = new LocalizedJsonbReaderService();
    }

    @Test
    void readsExactLocaleFirst() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("zh-CN", "抹茶套装");
        values.put("en-US", "Matcha Kit");

        assertThat(service.read(values, Locale.US)).isEqualTo("Matcha Kit");
    }

    @Test
    void fallsBackToDefaultLocaleWhenAppLocaleMissing() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("zh-CN", "抹茶套装");
        values.put("en-US", "Matcha Kit");

        assertThat(service.read(values, Locale.JAPAN)).isEqualTo("抹茶套装");
    }

    @Test
    void fallsBackToFirstAvailableWhenNoDefaultLocale() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("en-US", "Matcha Kit");
        values.put("fr-FR", "Kit Matcha");

        assertThat(service.read(values, Locale.JAPAN)).isEqualTo("Matcha Kit");
    }
}
