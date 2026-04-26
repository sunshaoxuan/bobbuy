package com.bobbuy.api;

import com.bobbuy.model.MediaGalleryItem;
import com.bobbuy.model.OrderMethod;
import com.bobbuy.model.PriceTier;
import com.bobbuy.model.ProductVisibility;
import com.bobbuy.model.StorageCondition;

import java.util.List;
import java.util.Map;

public record AiOnboardingSuggestion(
    String name,
    String brand,
    String description,
    Double price,
    String categoryId,
    String itemNumber,
    StorageCondition storageCondition,
    OrderMethod orderMethod,
    List<MediaGalleryItem> mediaGallery,
    Map<String, String> attributes,
    boolean existingProductFound,
    String existingProductId,
    List<AiProductCandidate> similarProductCandidates,
    ProductVisibility visibilityStatus,
    List<PriceTier> detectedPriceTiers,
    String originalPhotoBase64,
    String inputSampleId,
    String recognitionSummary,
    List<String> sourceDomains,
    List<String> rejectedSourceDomains,
    String sourcePolicyVersion,
    AiOnboardingTrace trace,
    Double matchScore,
    String semanticReasoning,
    List<AiFieldDiff> fieldDiffs,
    AiVerificationTarget verificationTarget
) {
}
