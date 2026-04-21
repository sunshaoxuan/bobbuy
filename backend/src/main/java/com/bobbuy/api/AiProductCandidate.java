package com.bobbuy.api;

public record AiProductCandidate(
    String productId,
    String displayName,
    String itemNumber,
    String matchReason,
    java.util.List<String> matchSignals,
    double score,
    String brand,
    String categoryId,
    java.util.List<String> matchedFragments,
    java.util.List<String> aliasSources
) {
}
