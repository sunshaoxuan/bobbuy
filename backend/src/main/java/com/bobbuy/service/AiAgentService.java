package com.bobbuy.service;

import com.bobbuy.model.ExperienceMapping;
import com.bobbuy.repository.ExperienceMappingRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AiAgentService {
  private static final Pattern QTY_PATTERN = Pattern.compile("(\\d+|[一二两三四五六七八九十])\\s*(个|袋|箱|盒|pack|packs|pcs|件|份|只|瓶|罐)?", Pattern.CASE_INSENSITIVE);
  private static final List<String> QUANTITY_UNITS = List.of("个", "袋", "箱", "盒", "pack", "packs", "pcs", "件", "份", "只", "瓶", "罐");
  private static final List<String> CHINESE_NUMBERS = List.of("一", "二", "两", "三", "四", "五", "六", "七", "八", "九", "十");

  private final LlmGateway llmGateway;
  private final ObjectProvider<ExperienceMappingRepository> experienceMappingRepositoryProvider;

  public AiAgentService(LlmGateway llmGateway, ObjectProvider<ExperienceMappingRepository> experienceMappingRepositoryProvider) {
    this.llmGateway = llmGateway;
    this.experienceMappingRepositoryProvider = experienceMappingRepositoryProvider;
  }

  public Optional<String> translate(String text, String targetLocale) {
    return llmGateway.translate(text, targetLocale);
  }

  public List<ExtractedItem> parse(String text) {
    List<CandidateItem> items = fromLlm(text).orElseGet(() -> parseHeuristically(text));
    if (items.isEmpty()) {
      return List.of();
    }

    Map<String, MatchCatalogItem> catalog = buildCatalogWithExperience();
    List<ExtractedItem> extracted = new ArrayList<>();
    int index = 1;
    for (CandidateItem candidate : items) {
      MatchCatalogItem bestMatch = null;
      double bestScore = 0.0;
      for (MatchCatalogItem target : catalog.values()) {
        double score = 0.0;
        for (String alias : target.aliases()) {
          score = Math.max(score, similarity(candidate.name(), alias));
        }
        if (score > bestScore) {
          bestScore = score;
          bestMatch = target;
        }
      }

      if (bestMatch != null && bestScore >= 0.60) {
        extracted.add(new ExtractedItem(
            "AI-" + index,
            candidate.name(),
            bestMatch.name(),
            Math.max(1, candidate.quantity()),
            candidate.note(),
            bestMatch.price(),
            bestScore
        ));
      } else {
        extracted.add(new ExtractedItem(
            "AI-" + index,
            candidate.name(),
            candidate.name(),
            Math.max(1, candidate.quantity()),
            candidate.note(),
            0.0,
            0.50
        ));
      }
      index++;
    }
    return extracted;
  }

  @Transactional
  public void confirmMapping(String originalName, String matchedName) {
    if (originalName == null || originalName.isBlank() || matchedName == null || matchedName.isBlank()) {
      return;
    }
    String normalized = normalize(originalName);
    ExperienceMappingRepository experienceMappingRepository = experienceMappingRepositoryProvider.getIfAvailable();
    if (experienceMappingRepository == null) {
      return;
    }
    ExperienceMapping mapping = experienceMappingRepository.findByFuzzyTerm(normalized)
        .orElseGet(() -> new ExperienceMapping(normalized, matchedName.trim()));
    mapping.setMappedName(matchedName.trim());
    mapping.setUpdatedAt(Instant.now());
    experienceMappingRepository.save(mapping);
  }

  private Optional<List<CandidateItem>> fromLlm(String text) {
    return llmGateway.parseItems(text).map(items -> {
      List<CandidateItem> parsed = new ArrayList<>();
      for (Map<String, Object> item : items) {
        String name = item.get("name") instanceof String value ? value.trim() : "";
        if (name.isBlank()) {
          continue;
        }
        int quantity = parseQuantity(item.get("quantity") == null ? "" : String.valueOf(item.get("quantity")));
        String note = item.get("note") instanceof String value ? value.trim() : "";
        parsed.add(new CandidateItem(name, quantity, note));
      }
      return parsed;
    });
  }

  private List<CandidateItem> parseHeuristically(String text) {
    if (text == null || text.isBlank()) {
      return List.of();
    }

    String normalized = text.replace('，', ',')
        .replace('。', ',')
        .replace('；', ',')
        .replace(';', ',')
        .replace('、', ',')
        .replace("还有", ",")
        .replace("另外", ",")
        .replace("以及", ",")
        .replace(" and ", ",");

    String[] parts = normalized.split(",");
    List<CandidateItem> results = new ArrayList<>();
    for (String rawPart : parts) {
      String part = rawPart.trim();
      if (part.isBlank()) {
        continue;
      }
      int quantity = parseQuantity(part);
      String note = extractNote(part);
      String name = cleanName(part);
      if (!name.isBlank()) {
        results.add(new CandidateItem(name, quantity, note));
      }
    }
    return results;
  }

  private int parseQuantity(String text) {
    Matcher matcher = QTY_PATTERN.matcher(text == null ? "" : text);
    if (!matcher.find()) {
      return 1;
    }
    String token = matcher.group(1);
    if (isDigits(token)) {
      return Integer.parseInt(token);
    }
    return switch (token) {
      case "一" -> 1;
      case "二", "两" -> 2;
      case "三" -> 3;
      case "四" -> 4;
      case "五" -> 5;
      case "六" -> 6;
      case "七" -> 7;
      case "八" -> 8;
      case "九" -> 9;
      case "十" -> 10;
      default -> 1;
    };
  }

  private String extractNote(String text) {
    if (text == null || text.isBlank()) {
      return "";
    }
    int index = text.indexOf("口味");
    if (index > 0) {
      return text.substring(0, index + 2).trim();
    }
    return "";
  }

  private String cleanName(String text) {
    String cleaned = text == null ? "" : text;
    cleaned = cleaned.replace("最少", "");
    cleaned = removeQuantityTokens(cleaned);
    cleaned = collapseSpaces(cleaned);
    if (cleaned.endsWith("的")) {
      cleaned = cleaned.substring(0, cleaned.length() - 1).trim();
    }
    return cleaned;
  }

  private Map<String, MatchCatalogItem> buildCatalogWithExperience() {
    Map<String, MatchCatalogItem> catalog = new LinkedHashMap<>();
    catalog.put("Muffin", new MatchCatalogItem("Muffin", "SKU-MUFFIN", 6.99,
        new HashSet<>(Set.of("muffin", "马粪蛋糕", "松饼", "杯子蛋糕"))));
    catalog.put("Tomato", new MatchCatalogItem("Tomato", "SKU-TOMATO", 2.99,
        new HashSet<>(Set.of("tomato", "番茄", "西红柿"))));
    catalog.put("Cheese Cake", new MatchCatalogItem("Cheese Cake", "SKU-CHEESE-CAKE", 8.99,
        new HashSet<>(Set.of("cheese cake", "芝士蛋糕", "起司蛋糕"))));
    catalog.put("Cherry", new MatchCatalogItem("Cherry", "SKU-CHERRY", 11.50,
        new HashSet<>(Set.of("cherry", "车厘子", "さくらんぼ"))));
    catalog.put("Spinach", new MatchCatalogItem("Spinach", "SKU-SPINACH", 3.49,
        new HashSet<>(Set.of("spinach", "菠菜", "ほれんそ", "ほうれん草"))));

    ExperienceMappingRepository experienceMappingRepository = experienceMappingRepositoryProvider.getIfAvailable();
    if (experienceMappingRepository != null) {
      for (ExperienceMapping mapping : experienceMappingRepository.findAll()) {
        String mapped = mapping.getMappedName();
        MatchCatalogItem existing = catalog.get(mapped);
        if (existing != null) {
          existing.aliases().add(mapping.getFuzzyTerm());
        } else {
          catalog.put(mapped, new MatchCatalogItem(mapped, "SKU-CUSTOM-" + normalize(mapped).toUpperCase(Locale.ROOT), 0.0,
              new HashSet<>(Set.of(mapping.getFuzzyTerm(), mapped))));
        }
      }
    }

    return catalog;
  }

  private double similarity(String left, String right) {
    String a = normalize(left);
    String b = normalize(right);
    if (a.isBlank() || b.isBlank()) {
      return 0.0;
    }
    if (a.equals(b)) {
      return 1.0;
    }
    if (a.contains(b) || b.contains(a)) {
      return 0.92;
    }

    Set<String> aShingles = shingles(a);
    Set<String> bShingles = shingles(b);
    if (aShingles.isEmpty() || bShingles.isEmpty()) {
      return 0.0;
    }
    int overlap = 0;
    for (String token : aShingles) {
      if (bShingles.contains(token)) {
        overlap++;
      }
    }
    return overlap / (double) (aShingles.size() + bShingles.size() - overlap);
  }

  private Set<String> shingles(String text) {
    String normalized = normalize(text);
    if (normalized.length() < 2) {
      return Set.of(normalized);
    }
    Set<String> tokens = new HashSet<>();
    for (int i = 0; i < normalized.length() - 1; i++) {
      tokens.add(normalized.substring(i, i + 2));
    }
    return tokens;
  }

  private String normalize(String value) {
    if (value == null) {
      return "";
    }
    return value.toLowerCase(Locale.ROOT)
        .replaceAll("[^\\p{IsHan}a-z0-9ぁ-んァ-ヶー]", "")
        .trim();
  }

  private String removeQuantityTokens(String value) {
    String result = value;
    for (String unit : QUANTITY_UNITS) {
      for (int i = 0; i <= 99; i++) {
        String number = String.valueOf(i);
        result = result.replace(number + unit, "");
        result = result.replace(number + " " + unit, "");
      }
      for (String number : CHINESE_NUMBERS) {
        result = result.replace(number + unit, "");
        result = result.replace(number + " " + unit, "");
      }
    }
    return result;
  }

  private String collapseSpaces(String value) {
    String result = value.trim().replace('\n', ' ').replace('\r', ' ').replace('\t', ' ');
    while (result.contains("  ")) {
      result = result.replace("  ", " ");
    }
    return result;
  }

  private boolean isDigits(String token) {
    if (token == null || token.isEmpty()) {
      return false;
    }
    for (int i = 0; i < token.length(); i++) {
      if (!Character.isDigit(token.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  private record CandidateItem(String name, int quantity, String note) {
  }

  private record MatchCatalogItem(String name, String skuId, double price, Set<String> aliases) {
  }

  public record ExtractedItem(String id, String originalName, String matchedName, int quantity, String note,
                              double price, double confidence) {
  }
}
