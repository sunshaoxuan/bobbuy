package com.bobbuy.api;

public record AiFieldDiff(
    String field,
    String label,
    String oldValue,
    String newValue,
    boolean different,
    boolean identityField
) {
}
