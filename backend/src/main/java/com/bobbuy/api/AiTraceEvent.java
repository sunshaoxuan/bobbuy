package com.bobbuy.api;

import java.time.LocalDateTime;

public record AiTraceEvent(
    String provider,
    String activeProvider,
    String model,
    String stage,
    Long latencyMs,
    String errorCode,
    String errorMessage,
    String fallbackReason,
    Integer retryCount,
    Integer attemptNo,
    String inputRef,
    String outputRef,
    String status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
