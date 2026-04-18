package com.bobbuy.api;

import com.bobbuy.model.MediaGalleryItem;
import com.bobbuy.model.OrderMethod;
import com.bobbuy.model.PriceTier;
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
    List<PriceTier> detectedPriceTiers
) {
}
