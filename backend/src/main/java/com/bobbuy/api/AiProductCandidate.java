package com.bobbuy.api;

public record AiProductCandidate(
    String productId,
    String displayName,
    String itemNumber,
    String matchReason
) {
}
