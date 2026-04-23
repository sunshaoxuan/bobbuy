package com.bobbuy.service;

import com.bobbuy.model.OrderHeader;
import com.bobbuy.model.OrderLine;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ProcurementReceiptRecognitionService {
  private static final Logger log = LoggerFactory.getLogger(ProcurementReceiptRecognitionService.class);
  private static final Pattern JSON_BLOCK_PATTERN = Pattern.compile("```(?:json)?\\s*(\\{.*}|\\[.*])\\s*```", Pattern.DOTALL);
  private static final String AI_MODE = "AI";
  private static final String FALLBACK_MODE = "RULE_FALLBACK";
  private static final String REVIEW_STATUS_PENDING = "PENDING_REVIEW";
  private static final double DEFAULT_AI_CONFIDENCE = 0.86d;
  private static final double DEFAULT_FALLBACK_CONFIDENCE = 0.42d;
  // Keep name overlap as the primary signal while still letting price help disambiguate very similar receipt lines.
  private static final double MATCH_SCORE_THRESHOLD = 0.45d;
  private static final double TOKEN_SCORE_WEIGHT = 0.8d;
  private static final double PRICE_SCORE_WEIGHT = 0.2d;

  private final LlmGateway llmGateway;
  private final ObjectMapper objectMapper;

  public ProcurementReceiptRecognitionService(LlmGateway llmGateway, ObjectMapper objectMapper) {
    this.llmGateway = llmGateway;
    this.objectMapper = objectMapper;
  }

  public Map<String, Object> recognize(String base64Image, String fileName, List<OrderHeader> orders) {
    Optional<AiReceiptExtraction> extracted = extractWithAi(base64Image, fileName);
    if (extracted.isPresent()) {
      return reconcile(extracted.get(), orders);
    }
    return buildFallback(orders);
  }

  private Optional<AiReceiptExtraction> extractWithAi(String base64Image, String fileName) {
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
    try {
      Optional<String> response = llmGateway.generate(prompt, "llava", List.of(base64Image));
      if (response.isEmpty() || response.get().isBlank()) {
        return Optional.empty();
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
        return Optional.empty();
      }
      return Optional.of(new AiReceiptExtraction(
          stringValue(raw.get("merchantName")),
          stringValue(raw.get("receiptDate")),
          stringValue(raw.get("currency")),
          firstNonBlank(stringValue(raw.get("summary")), "AI extracted receipt items."),
          items,
          response.get()));
    } catch (Exception ex) {
      log.warn("Receipt AI extraction failed for fileName={}: {}", fileName, ex.getMessage());
      return Optional.empty();
    }
  }

  private Map<String, Object> reconcile(AiReceiptExtraction extraction, List<OrderHeader> orders) {
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
      result.put("reviewedBy", null);
      result.put("reviewedAt", null);
    return result;
  }

  private Map<String, Object> buildFallback(List<OrderHeader> orders) {
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
    result.put("reviewedBy", "");
    result.put("reviewedAt", "");
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
}
