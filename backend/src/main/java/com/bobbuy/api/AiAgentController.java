package com.bobbuy.api;

import com.bobbuy.api.response.ApiResponse;
import com.bobbuy.api.response.ApiException;
import com.bobbuy.api.response.ErrorCode;
import com.bobbuy.model.PriceTier;
import com.bobbuy.model.Product;
import com.bobbuy.model.ProductPatch;
import com.bobbuy.service.AiAgentService;
import com.bobbuy.service.AiProductOnboardingService;
import com.bobbuy.service.BobbuyStore;
import com.bobbuy.service.ImageStorageService;
import com.bobbuy.service.ProcurementHudService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiAgentController {
  private final AiAgentService aiAgentService;
  private final AiProductOnboardingService aiProductOnboardingService;
  private final BobbuyStore store;
  private final ImageStorageService imageStorageService;
  private final ProcurementHudService procurementHudService;

  @Value("${bobbuy.ai.default-reconcile-quantity:1}")
  private int defaultReconcileQuantity;

  public AiAgentController(AiAgentService aiAgentService,
                           AiProductOnboardingService aiProductOnboardingService,
                           BobbuyStore store,
                           ImageStorageService imageStorageService,
                           ProcurementHudService procurementHudService) {
    this.aiAgentService = aiAgentService;
    this.aiProductOnboardingService = aiProductOnboardingService;
    this.store = store;
    this.imageStorageService = imageStorageService;
    this.procurementHudService = procurementHudService;
  }

  @PostMapping("/parse")
  public ResponseEntity<ApiResponse<AiParseResponse>> parse(@Valid @RequestBody AiParseRequest request) {
    List<AiExtractedItemResponse> items = aiAgentService.parse(request.getText())
        .stream()
        .map(item -> new AiExtractedItemResponse(
            item.id(),
            item.originalName(),
            item.matchedName(),
            item.quantity(),
            item.note(),
            item.price(),
            item.confidence()
        ))
        .toList();
    return ResponseEntity.ok(ApiResponse.success(new AiParseResponse(items)));
  }

  @PostMapping("/experience/confirm")
  public ResponseEntity<ApiResponse<Void>> confirmMapping(@Valid @RequestBody AiConfirmMappingRequest request) {
    aiAgentService.confirmMapping(request.getOriginalName(), request.getMatchedName());
    return ResponseEntity.ok(ApiResponse.success(null));
  }

  @PostMapping("/translate")
  public ResponseEntity<ApiResponse<AiTranslateResponse>> translate(@Valid @RequestBody AiTranslateRequest request) {
    String translated = aiAgentService.translate(request.getText(), request.getTargetLocale())
        .orElse("");
    return ResponseEntity.ok(ApiResponse.success(new AiTranslateResponse(translated)));
  }

  @PostMapping("/onboard/scan")
  public ResponseEntity<ApiResponse<AiOnboardingSuggestion>> scan(@Valid @RequestBody AiOnboardScanRequest request) {
    return aiProductOnboardingService.onboardFromPhoto(request.getBase64Image())
        .map(suggestion -> ResponseEntity.ok(ApiResponse.success(suggestion)))
        .orElseThrow(() -> new ApiException(ErrorCode.INVALID_REQUEST, "error.ai.extract_failed"));
  }

  @PostMapping("/onboard/confirm")
  public ResponseEntity<ApiResponse<MobileProductResponse>> confirmOnboard(@Valid @RequestBody AiOnboardingSuggestion suggestion) {
    Product result;

    String evidenceImageUrl = imageStorageService.saveBase64(suggestion.originalPhotoBase64());
    
    if (suggestion.existingProductFound() && suggestion.existingProductId() != null) {
      // Incremental update: patch existing product with detected price tiers
      ProductPatch patch = new ProductPatch();
      if (suggestion.price() != null) {
        patch.setBasePrice(suggestion.price());
      }
      if (suggestion.detectedPriceTiers() != null && !suggestion.detectedPriceTiers().isEmpty()) {
        patch.setPriceTiers(suggestion.detectedPriceTiers());
      }
      
      List<com.bobbuy.model.MediaGalleryItem> gallery = new java.util.ArrayList<>(suggestion.mediaGallery() != null ? suggestion.mediaGallery() : List.of());
      if (evidenceImageUrl != null) {
        gallery.add(0, new com.bobbuy.model.MediaGalleryItem(evidenceImageUrl, com.bobbuy.model.MediaType.IMAGE, new HashMap<>()));
      }
      patch.setMediaGallery(gallery);
      
      if (suggestion.brand() != null) {
        patch.setBrand(suggestion.brand());
      }
      result = store.patchProduct(suggestion.existingProductId(), patch)
          .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "error.product.not_found"));
    } else {
      // Create new product
      Product newProduct = new Product();
      Map<String, String> nameMap = new HashMap<>();
      nameMap.put("zh-CN", suggestion.name());
      nameMap.put("ja-JP", suggestion.name());
      nameMap.put("en-US", suggestion.name());
      newProduct.setName(nameMap);

      Map<String, String> descMap = new HashMap<>();
      if (suggestion.description() != null) {
        descMap.put("zh-CN", suggestion.description());
        descMap.put("ja-JP", suggestion.description());
        descMap.put("en-US", suggestion.description());
      }
      newProduct.setDescription(descMap);

      newProduct.setBrand(suggestion.brand());
      newProduct.setBasePrice(suggestion.price() != null ? suggestion.price() : 0.0);
      
      List<com.bobbuy.model.MediaGalleryItem> gallery = new java.util.ArrayList<>(suggestion.mediaGallery() != null ? suggestion.mediaGallery() : List.of());
      if (evidenceImageUrl != null) {
        gallery.add(0, new com.bobbuy.model.MediaGalleryItem(evidenceImageUrl, com.bobbuy.model.MediaType.IMAGE, new HashMap<>()));
      }
      newProduct.setMediaGallery(gallery);
      
      newProduct.setStorageCondition(suggestion.storageCondition());
      newProduct.setOrderMethod(suggestion.orderMethod());
      newProduct.setCategoryId(suggestion.categoryId());
      newProduct.setItemNumber(suggestion.itemNumber());
      newProduct.setMerchantSkus(new HashMap<>());
      if (suggestion.detectedPriceTiers() != null) {
        newProduct.setPriceTiers(suggestion.detectedPriceTiers());
      }

      result = store.createProduct(newProduct);
    }

    procurementHudService.reconcileInventory(result.getId(), defaultReconcileQuantity);

    // Return a simple response with the product
    return ResponseEntity.ok(ApiResponse.success(
        new MobileProductResponse(result, suggestion.name(), suggestion.description())));
  }
}
