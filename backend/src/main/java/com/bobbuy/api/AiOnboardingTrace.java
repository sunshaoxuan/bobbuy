package com.bobbuy.api;

import java.util.List;

public record AiOnboardingTrace(
    String inputSampleId,
    String recognitionSummary,
    List<String> sourceDomains,
    String resultDecision,
    String finalProductId
) {
}
