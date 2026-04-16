package com.bobbuy.service;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class LocalizedJsonbReaderService {
    private static final String DEFAULT_LOCALE = "zh-CN";

    public <T> T read(Map<String, T> localizedValues) {
        return read(localizedValues, LocaleContextHolder.getLocale());
    }

    public <T> T read(Map<String, T> localizedValues, Locale appLocale) {
        if (localizedValues == null || localizedValues.isEmpty()) {
            return null;
        }

        for (String candidate : resolveCandidates(appLocale)) {
            T value = findValue(localizedValues, candidate);
            if (value != null) {
                return value;
            }
        }

        return localizedValues.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private List<String> resolveCandidates(Locale appLocale) {
        List<String> candidates = new ArrayList<>();
        if (appLocale != null) {
            addCandidate(candidates, appLocale.toLanguageTag());
            addCandidate(candidates, appLocale.getLanguage());
        }
        addCandidate(candidates, DEFAULT_LOCALE);
        addCandidate(candidates, Locale.SIMPLIFIED_CHINESE.getLanguage());
        return candidates;
    }

    private void addCandidate(List<String> candidates, String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return;
        }
        String normalized = normalizeLocale(candidate);
        if (candidates.stream().noneMatch(existing -> normalizeLocale(existing).equals(normalized))) {
            candidates.add(candidate);
        }
    }

    private <T> T findValue(Map<String, T> localizedValues, String candidate) {
        String normalizedCandidate = normalizeLocale(candidate);
        return localizedValues.entrySet().stream()
                .filter(entry -> normalizeLocale(entry.getKey()).equals(normalizedCandidate))
                .map(Map.Entry::getValue)
                .filter(value -> value != null)
                .findFirst()
                .orElse(null);
    }

    private String normalizeLocale(String locale) {
        return locale.replace('_', '-').toLowerCase(Locale.ROOT);
    }
}
