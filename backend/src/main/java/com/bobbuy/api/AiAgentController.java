package com.bobbuy.api;

import com.bobbuy.api.response.ApiResponse;
import com.bobbuy.service.AiAgentService;
import com.bobbuy.service.AiProductOnboardingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
public class AiAgentController {
  private final AiAgentService aiAgentService;
  private final AiProductOnboardingService aiProductOnboardingService;

  public AiAgentController(AiAgentService aiAgentService, AiProductOnboardingService aiProductOnboardingService) {
    this.aiAgentService = aiAgentService;
    this.aiProductOnboardingService = aiProductOnboardingService;
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
        .orElseGet(() -> ResponseEntity.badRequest().body(ApiResponse.error("Failed to extract product info from image")));
  }
}
