package com.bobbuy.api;

import com.bobbuy.model.MediaGalleryItem;

import java.util.List;

public record AiVerificationTarget(
    String productId,
    String displayName,
    List<MediaGalleryItem> mediaGallery
) {
}
