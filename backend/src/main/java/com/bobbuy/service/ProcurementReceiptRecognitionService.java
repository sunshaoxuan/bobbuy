package com.bobbuy.service;

import com.bobbuy.api.response.ApiResponse;
import com.bobbuy.model.OrderHeader;
import com.bobbuy.model.OrderLine;
import com.bobbuy.service.client.AiReceiptRecognitionClient;
import com.bobbuy.service.client.AiReceiptRecognitionRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ProcurementReceiptRecognitionService {
  private static final Logger log = LoggerFactory.getLogger(ProcurementReceiptRecognitionService.class);
  private static final Pattern JSON_BLOCK_PATTERN = Pattern.compile("```(?:json)?\\s*(\\{.*}|\\[.*])\\s*```", Pattern.DOTALL);
  private static final String AI_MODE = "AI";
  private static final String FALLBACK_MODE = "RULE_FALLBACK";
  private static final String REVIEW_STATUS_PENDING = "PENDING_REVIEW";
  private static final String STATUS_RECOGNIZED = "RECOGNIZED";
  private static final String STATUS_FAILED_RECOGNITION = "FAILED_RECOGNITION";
  private static final String STATUS_PENDING_MANUAL_REVIEW = "PENDING_MANUAL_REVIEW";
  private static final String STATUS_REVIEWED = "REVIEWED";
  private static final double DEFAULT_AI_CONFIDENCE = 0.86d;
  private static final double DEFAULT_FALLBACK_CONFIDENCE = 0.42d;
  // Keep name overlap as the primary signal while still letting price help disambiguate very similar receipt lines.
  private static final double MATCH_SCORE_THRESHOLD = 0.45d;
  private static final double TOKEN_SCORE_WEIGHT = 0.8d;
  private static final double PRICE_SCORE_WEIGHT = 0.2d;

  private final LlmGateway llmGateway;
  private final ObjectMapper objectMapper;
  private final AiReceiptRecognitionClient aiReceiptRecognitionClient;
  private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;
  private final boolean remoteEnabled;

  @Autowired
  public ProcurementReceiptRecognitionService(LlmGateway llmGateway,
                                              ObjectMapper objectMapper,
                                              ObjectProvider<AiReceiptRecognitionClient> aiReceiptRecognitionClientProvider,
                                              ObjectProvider<CircuitBreakerFactory<?, ?>> circuitBreakerFactoryProvider,
                                              @Value("${bobbuy.ai.remote.enabled:false}") boolean remoteEnabled) {
    this.llmGateway = llmGateway;
    this.objectMapper = objectMapper;
    this.aiReceiptRecognitionClient = aiReceiptRecognitionClientProvider.getIfAvailable();
    this.circuitBreakerFactory = circuitBreakerFactoryProvider.getIfAvailable();
    this.remoteEnabled = remoteEnabled;
  }

  ProcurementReceiptRecognitionService(LlmGateway llmGateway, ObjectMapper objectMapper) {
    this.llmGateway = llmGateway;
    this.objectMapper = objectMapper;
    this.aiReceiptRecognitionClient = null;
    this.circuitBreakerFactory = null;
    this.remoteEnabled = false;
  }

  public Map<String, Object> recognize(String base64Image, String fileName, List<OrderHeader> orders) {
    if (remoteEnabled && aiReceiptRecognitionClient != null) {
      return recognizeRemotely(base64Image, fileName, orders);
    }
    return recognizeLocally(base64Image, fileName, orders);
  }

  public Map<String, Object> recognizeLocally(String base64Image, String fileName, List<OrderHeader> orders) {
    ReceiptRecognitionAttempt attempt = extractWithAi(base64Image, fileName);
    if (attempt.extraction().isPresent()) {
      return reconcile(attempt.extraction().get(), orders, attempt.trace());
    }
    return buildFallback(orders, attempt.trace());
  }

  private Map<String, Object> recognizeRemotely(String base64Image, String fileName, List<OrderHeader> orders) {
    AiReceiptRecognitionRequest request = new AiReceiptRecognitionRequest(base64Image, fileName, orders);
    Map<String, Object> fallbackTrace = buildTrace(
        remoteEnabled ? "remote-ai-service" : "unconfigured",
        remoteEnabled ? "remote-ai-service" : "unconfigured",
        "receipt-recognizer",
        "REMOTE_OCR",
        0L,
        "REMOTE_UNAVAILABLE",
        "Remote receipt recognition degraded to local fallback.",
        "remote-service-fallback",
        0,
        1,
        firstNonBlank(fileName, "inline-receipt"),
        null,
        STATUS_FAILED_RECOGNITION,
        LocalDateTime.now(),
        LocalDateTime.now());
    if (circuitBreakerFactory == null) {
      try {
        return extractRemoteResult(request, orders);
      } catch (Exception ex) {
        log.warn("Remote AI receipt recognition failed without circuit breaker: {}", ex.getMessage());
        return buildFallback(orders, fallbackTrace);
      }
    }
    return circuitBreakerFactory.create("aiService").run(
        () -> extractRemoteResult(request, orders),
        throwable -> {
          log.warn("Remote AI receipt recognition degraded to fallback: {}", throwable.getMessage());
          return buildFallback(orders, fallbackTrace);
        });
  }

  private Map<String, Object> extractRemoteResult(AiReceiptRecognitionRequest request, List<OrderHeader> orders) {
    ApiResponse<Map<String, Object>> response = aiReceiptRecognitionClient.recognizeReceipt(request);
    if (response == null || response.getData() == null || response.getData().isEmpty()) {
      return buildFallback(orders, buildTrace(
          "remote-ai-service",
          "remote-ai-service",
          "receipt-recognizer",
          "REMOTE_OCR",
          0L,
          "EMPTY_REMOTE_RESPONSE",
          "Remote receipt recognition returned an empty payload.",
          "remote-empty-response",
          0,
          1,
          firstNonBlank(request.fileName(), "inline-receipt"),
          null,
          STATUS_FAILED_RECOGNITION,
          LocalDateTime.now(),
          LocalDateTime.now()));
    }
    return response.getData();
  }

  private ReceiptRecognitionAttempt extractWithAi(String base64Image, String fileName) {
    String prompt = """
        你是采购小票识别助手。请识别这张采购小票图片，并严格输出 JSON 对象，不要输出解释。
        JSON schema:
        {
          "merchantName": "string",
          "receiptDate": "string|null",
          "currency": "string|null",
          "summary": "string",
          "receiptItems": [
            {
              "name": "string",
              "quantity": number,
              "unitPrice": number|null,
              "totalPrice": number|null,
              "categoryHint": "string|null"
            }
          ]
        }
        要求：
        1. quantity 无法识别时填 1。
        2. unitPrice / totalPrice 无法识别时填 null。
        3. receiptItems 至少返回你能确认的商品行，不要编造不存在的条目。
        4. summary 用中文概括识别置信度和主要不确定点。
        附加文件名：%s
        """.formatted(fileName == null ? "unknown" : fileName);
    String inputRef = firstNonBlank(fileName, "inline-receipt");
    LocalDateTime startedAt = LocalDateTime.now();
    long startedNs = System.nanoTime();
    String provider = llmGateway.getEdgeProvider();
    String model = llmGateway.getEdgeModel();
    try {
      Optional<String> response = llmGateway.generate(prompt, "llava", List.of(base64Image));
      if (response.isEmpty() || response.get().isBlank()) {
        return new ReceiptRecognitionAttempt(
            Optional.empty(),
            buildTrace(provider, provider, model, "LLM_STRUCTURING", elapsedMs(startedNs), "UNCONFIGURED",
                "Receipt AI returned empty response.", "unconfigured-or-empty-response", 0, 1, inputRef, null,
                STATUS_FAILED_RECOGNITION, startedAt, LocalDateTime.now()));
      }
      String jsonPayload = extractJsonPayload(response.get());
      Map<String, Object> raw = objectMapper.readValue(jsonPayload, new TypeReference<>() {});
      List<Map<String, Object>> items = new ArrayList<>();
      if (raw.get("receiptItems") instanceof List<?> rawItems) {
        for (Object rawItem : rawItems) {
          if (rawItem instanceof Map<?, ?> map && map.get("name") != null) {
            items.add(sanitizeReceiptItem(map));
          }
        }
      }
      if (items.isEmpty()) {
        return new ReceiptRecognitionAttempt(
            Optional.empty(),
            buildTrace(provider, provider, model, "LLM_STRUCTURING", elapsedMs(startedNs), "EMPTY_RESULT",
                "Receipt AI did not return any usable line items.", "empty-result-fallback", 0, 1, inputRef, null,
                STATUS_FAILED_RECOGNITION, startedAt, LocalDateTime.now()));
      }
      return new ReceiptRecognitionAttempt(Optional.of(new AiReceiptExtraction(
          stringValue(raw.get("merchantName")),
          stringValue(raw.get("receiptDate")),
          stringValue(raw.get("currency")),
          firstNonBlank(stringValue(raw.get("summary")), "AI extracted receipt items."),
          items,
          response.get())),
          buildTrace(provider, provider, model, "LLM_STRUCTURING", elapsedMs(startedNs), null,
              null, null, 0, 1, inputRef, null, STATUS_RECOGNIZED, startedAt, LocalDateTime.now()));
    } catch (Exception ex) {
      log.warn("Receipt AI extraction failed for fileName={}: {}", fileName, ex.getMessage());
      return new ReceiptRecognitionAttempt(
          Optional.empty(),
          buildTrace(provider, provider, model, "LLM_STRUCTURING", elapsedMs(startedNs), "AI_RECOGNITION_FAILED",
              truncateTraceMessage(ex.getMessage()), "exception-fallback", 0, 1, inputRef, null,
              STATUS_FAILED_RECOGNITION, startedAt, LocalDateTime.now()));
    }
  }

  private Map<String, Object> reconcile(AiReceiptExtraction extraction, List<OrderHeader> orders, Map<String, Object> trace) {
    List<ReceiptLineCandidate> candidates = flattenOrders(orders);
    Map<String, Integer> matchedByKey = new HashMap<>();
    List<Map<String, Object>> matchedOrderLines = new ArrayList<>();
    List<Map<String, Object>> unmatchedReceiptItems = new ArrayList<>();
    List<Map<String, Object>> receiptItems = new ArrayList<>(extraction.receiptItems());

    for (Map<String, Object> receiptItem : extraction.receiptItems()) {
      String receiptName = stringValue(receiptItem.get("name"));
      int receiptQuantity = intValue(receiptItem.get("quantity"), 1);
      double receiptUnitPrice = doubleValue(receiptItem.get("unitPrice"));

      Optional<ReceiptLineCandidate> best = candidates.stream()
          .filter(candidate -> remainingQuantity(candidate, matchedByKey) > 0)
          .map(candidate -> Map.entry(candidate, matchScore(receiptName, receiptUnitPrice, candidate)))
          .filter(entry -> entry.getValue() >= MATCH_SCORE_THRESHOLD)
          .max(Comparator.comparingDouble(Map.Entry::getValue))
          .map(Map.Entry::getKey);

      if (best.isEmpty()) {
        unmatchedReceiptItems.add(new LinkedHashMap<>(Map.of(
            "name", receiptName,
            "quantity", receiptQuantity,
            "unitPrice", receiptItem.get("unitPrice"),
            "totalPrice", receiptItem.get("totalPrice"),
            "disposition", "UNREVIEWED"
        )));
        continue;
      }

      ReceiptLineCandidate matched = best.get();
      int allocatable = Math.min(receiptQuantity, remainingQuantity(matched, matchedByKey));
      if (allocatable <= 0) {
        unmatchedReceiptItems.add(new LinkedHashMap<>(Map.of(
            "name", receiptName,
            "quantity", receiptQuantity,
            "unitPrice", receiptItem.get("unitPrice"),
            "totalPrice", receiptItem.get("totalPrice"),
            "disposition", "UNREVIEWED"
        )));
        continue;
      }
      matchedByKey.merge(matched.key(), allocatable, Integer::sum);
      matchedOrderLines.add(new LinkedHashMap<>(Map.of(
          "businessId", matched.businessId(),
          "skuId", matched.skuId(),
          "itemName", matched.itemName(),
          "orderedQuantity", matched.orderedQuantity(),
          "purchasedQuantity", matched.purchasedQuantity(),
          "matchedQuantity", allocatable,
          "receiptName", receiptName,
          "unitPrice", receiptItem.get("unitPrice")
      )));
      if (receiptQuantity > allocatable) {
        unmatchedReceiptItems.add(new LinkedHashMap<>(Map.of(
            "name", receiptName,
            "quantity", receiptQuantity - allocatable,
            "unitPrice", receiptItem.get("unitPrice"),
            "totalPrice", receiptItem.get("totalPrice"),
            "disposition", "UNREVIEWED"
        )));
      }
    }

    List<Map<String, Object>> missingOrderedItems = new ArrayList<>();
    for (ReceiptLineCandidate candidate : candidates) {
      int matchedQuantity = matchedByKey.getOrDefault(candidate.key(), 0);
      int missingQuantity = Math.max(candidate.orderedQuantity() - matchedQuantity, 0);
      if (missingQuantity > 0) {
        missingOrderedItems.add(new LinkedHashMap<>(Map.of(
            "businessId", candidate.businessId(),
            "skuId", candidate.skuId(),
            "itemName", candidate.itemName(),
            "missingQuantity", missingQuantity,
            "disposition", "OUT_OF_STOCK"
        )));
      }
    }

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("recognitionMode", AI_MODE);
    result.put("merchantName", extraction.merchantName());
    result.put("receiptDate", extraction.receiptDate());
    result.put("currency", extraction.currency());
    result.put("summary", extraction.summary());
      result.put("rawAiResponse", extraction.rawAiResponse());
      result.put("receiptItems", receiptItems);
      result.put("matchedOrderLines", matchedOrderLines);
      result.put("unmatchedReceiptItems", unmatchedReceiptItems);
      result.put("missingOrderedItems", missingOrderedItems);
    result.put("selfUseItems", new ArrayList<>());
    result.put("confidence", DEFAULT_AI_CONFIDENCE);
    result.put("reviewStatus", REVIEW_STATUS_PENDING);
    result.put("recognitionStatus", STATUS_RECOGNIZED);
    result.put("reviewedBy", null);
    result.put("reviewedAt", null);
    result.put("trace", trace);
    result.put("traceHistory", new ArrayList<>());
    return result;
  }

  private Map<String, Object> buildFallback(List<OrderHeader> orders, Map<String, Object> trace) {
    List<Map<String, Object>> receiptItems = new ArrayList<>();
    List<Map<String, Object>> matchedOrderLines = new ArrayList<>();
    List<Map<String, Object>> missingOrderedItems = new ArrayList<>();
    for (OrderHeader order : orders) {
      for (OrderLine line : safeLines(order)) {
        int orderedQuantity = Math.max(line.getQuantity(), 0);
        int purchasedQuantity = Math.max(line.getPurchasedQuantity(), 0);
        receiptItems.add(Map.of(
            "name", line.getItemName(),
            "quantity", Math.max(purchasedQuantity, 1),
            "unitPrice", line.getUnitPrice(),
            "totalPrice", round2(Math.max(purchasedQuantity, 1) * line.getUnitPrice())
        ));
        matchedOrderLines.add(Map.of(
            "businessId", order.getBusinessId(),
            "skuId", line.getSkuId(),
            "itemName", line.getItemName(),
            "orderedQuantity", orderedQuantity,
            "purchasedQuantity", purchasedQuantity,
            "matchedQuantity", Math.min(Math.max(purchasedQuantity, 1), orderedQuantity)
        ));
        if (purchasedQuantity < orderedQuantity) {
          missingOrderedItems.add(Map.of(
              "businessId", order.getBusinessId(),
              "skuId", line.getSkuId(),
              "itemName", line.getItemName(),
              "missingQuantity", orderedQuantity - purchasedQuantity,
              "disposition", "OUT_OF_STOCK"
          ));
        }
      }
    }
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("recognitionMode", FALLBACK_MODE);
    result.put("summary", "AI service unavailable, using deterministic fallback from order data.");
    result.put("receiptItems", receiptItems);
    result.put("matchedOrderLines", matchedOrderLines);
    result.put("unmatchedReceiptItems", new ArrayList<>());
    result.put("missingOrderedItems", missingOrderedItems);
    result.put("selfUseItems", new ArrayList<>());
    result.put("confidence", DEFAULT_FALLBACK_CONFIDENCE);
    result.put("reviewStatus", REVIEW_STATUS_PENDING);
    result.put("recognitionStatus", STATUS_PENDING_MANUAL_REVIEW);
    result.put("reviewedBy", "");
    result.put("reviewedAt", "");
    result.put("trace", trace);
    result.put("traceHistory", new ArrayList<>());
    return result;
  }

  private List<ReceiptLineCandidate> flattenOrders(List<OrderHeader> orders) {
    List<ReceiptLineCandidate> result = new ArrayList<>();
    for (OrderHeader order : orders) {
      for (OrderLine line : safeLines(order)) {
        result.add(new ReceiptLineCandidate(
            order.getBusinessId(),
            line.getSkuId(),
            line.getItemName(),
            Math.max(line.getQuantity(), 0),
            Math.max(line.getPurchasedQuantity(), 0),
            round2(line.getUnitPrice())
        ));
      }
    }
    return result;
  }

  private int remainingQuantity(ReceiptLineCandidate candidate, Map<String, Integer> matchedByKey) {
    return Math.max(candidate.orderedQuantity() - matchedByKey.getOrDefault(candidate.key(), 0), 0);
  }

  private double matchScore(String receiptName, double receiptUnitPrice, ReceiptLineCandidate candidate) {
    Set<String> receiptTokens = normalizeTokens(receiptName);
    Set<String> candidateTokens = normalizeTokens(candidate.itemName());
    if (receiptTokens.isEmpty() || candidateTokens.isEmpty()) {
      return 0d;
    }
    long overlap = receiptTokens.stream().filter(candidateTokens::contains).count();
    if (overlap <= 0) {
      return 0d;
    }
    long union = receiptTokens.size() + candidateTokens.size() - overlap;
    double tokenScore = union <= 0 ? 0d : (double) overlap / (double) union;
    double priceScore = 0d;
    if (receiptUnitPrice > 0d && candidate.unitPrice() > 0d) {
      double diffRatio = Math.abs(receiptUnitPrice - candidate.unitPrice()) / candidate.unitPrice();
      priceScore = Math.max(0d, 1d - diffRatio);
    }
    return (tokenScore * TOKEN_SCORE_WEIGHT) + (priceScore * PRICE_SCORE_WEIGHT);
  }

  private Set<String> normalizeTokens(String raw) {
    if (raw == null || raw.isBlank()) {
      return Set.of();
    }
    String normalized = raw.toLowerCase(Locale.ROOT)
        .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}\\p{IsIdeographic}]+", " ")
        .trim();
    if (normalized.isBlank()) {
      return Set.of();
    }
    String[] split = normalized.split("\\s+");
    Set<String> tokens = new LinkedHashSet<>();
    for (String token : split) {
      if (!token.isBlank() && token.length() > 1) {
        tokens.add(token);
      }
    }
    return tokens;
  }

  private Map<String, Object> sanitizeReceiptItem(Map<?, ?> raw) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("name", stringValue(raw.get("name")));
    result.put("quantity", intValue(raw.get("quantity"), 1));
    result.put("unitPrice", nullableDouble(raw.get("unitPrice")));
    result.put("totalPrice", nullableDouble(raw.get("totalPrice")));
    result.put("categoryHint", stringValue(raw.get("categoryHint")));
    return result;
  }

  private String extractJsonPayload(String rawResponse) {
    String trimmed = rawResponse == null ? "" : rawResponse.trim();
    Matcher matcher = JSON_BLOCK_PATTERN.matcher(trimmed);
    if (matcher.find()) {
      return matcher.group(1).trim();
    }
    return trimmed;
  }

  private List<OrderLine> safeLines(OrderHeader order) {
    return order.getLines() == null ? List.of() : order.getLines();
  }

  private String stringValue(Object value) {
    return value == null ? "" : String.valueOf(value).trim();
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value.trim();
      }
    }
    return "";
  }

  private int intValue(Object value, int defaultValue) {
    if (value instanceof Number number) {
      return Math.max(number.intValue(), 0);
    }
    try {
      return Math.max(Integer.parseInt(String.valueOf(value)), 0);
    } catch (Exception ex) {
      return defaultValue;
    }
  }

  private double doubleValue(Object value) {
    Double nullable = nullableDouble(value);
    return nullable == null ? 0d : nullable;
  }

  private Double nullableDouble(Object value) {
    if (value == null || String.valueOf(value).isBlank() || "null".equalsIgnoreCase(String.valueOf(value))) {
      return null;
    }
    if (value instanceof Number number) {
      return round2(number.doubleValue());
    }
    try {
      return round2(Double.parseDouble(String.valueOf(value)));
    } catch (Exception ex) {
      return null;
    }
  }

  private double round2(double value) {
    return Math.round(value * 100d) / 100d;
  }

  private long elapsedMs(long startedNs) {
    return Math.max(0L, (System.nanoTime() - startedNs) / 1_000_000L);
  }

  private Map<String, Object> buildTrace(String provider,
                                         String activeProvider,
                                         String model,
                                         String stage,
                                         long latencyMs,
                                         String errorCode,
                                         String errorMessage,
                                         String fallbackReason,
                                         int retryCount,
                                         int attemptNo,
                                         String inputRef,
                                         String outputRef,
                                         String status,
                                         LocalDateTime createdAt,
                                         LocalDateTime updatedAt) {
    Map<String, Object> trace = new LinkedHashMap<>();
    trace.put("provider", firstNonBlank(provider, "unconfigured"));
    trace.put("activeProvider", firstNonBlank(activeProvider, "unconfigured"));
    trace.put("model", firstNonBlank(model, ""));
    trace.put("stage", firstNonBlank(stage, ""));
    trace.put("latencyMs", latencyMs);
    trace.put("errorCode", errorCode);
    trace.put("errorMessage", truncateTraceMessage(errorMessage));
    trace.put("fallbackReason", fallbackReason);
    trace.put("retryCount", retryCount);
    trace.put("attemptNo", attemptNo);
    trace.put("inputRef", firstNonBlank(inputRef, ""));
    trace.put("outputRef", outputRef);
    trace.put("status", firstNonBlank(status, STATUS_PENDING_MANUAL_REVIEW));
    trace.put("createdAt", createdAt);
    trace.put("updatedAt", updatedAt);
    return trace;
  }

  private String truncateTraceMessage(String errorMessage) {
    if (errorMessage == null || errorMessage.isBlank()) {
      return null;
    }
    String sanitized = errorMessage.replaceAll("\\s+", " ").trim();
    return sanitized.length() > 180 ? sanitized.substring(0, 180) : sanitized;
  }

  private record ReceiptLineCandidate(String businessId,
                                      String skuId,
                                      String itemName,
                                      int orderedQuantity,
                                      int purchasedQuantity,
                                      double unitPrice) {
    private String key() {
      return businessId + "::" + skuId + "::" + itemName;
    }
  }

  private record AiReceiptExtraction(String merchantName,
                                     String receiptDate,
                                     String currency,
                                     String summary,
                                     List<Map<String, Object>> receiptItems,
                                     String rawAiResponse) {
  }

  private record ReceiptRecognitionAttempt(Optional<AiReceiptExtraction> extraction,
                                           Map<String, Object> trace) {
  }
}
