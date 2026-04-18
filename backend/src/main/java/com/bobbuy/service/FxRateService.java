package com.bobbuy.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FxRateService {
  private static final Logger log = LoggerFactory.getLogger(FxRateService.class);
  private static final Pattern RATE_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)");
  private static final double DEFAULT_RATE = 1D;
  private static final double MIN_REASONABLE_EXCHANGE_RATE = 0.001D;
  private static final double MAX_REASONABLE_EXCHANGE_RATE = 1D;
  private static final String DEFAULT_BRAVE_ENDPOINT = "https://api.search.brave.com/res/v1/web/search";
  private static final String QUERY = "JPY to CNY exchange rate";

  private final HttpClient httpClient = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(2))
      .build();

  public double resolveCurrentRate() {
    Optional<Double> fromEnv = readRateFromEnv();
    if (fromEnv.isPresent()) {
      return fromEnv.get();
    }

    Optional<Double> fromBrave = fetchRateFromBraveSearch();
    if (fromBrave.isPresent()) {
      return fromBrave.get();
    }

    log.warn("Unable to resolve JPY/CNY FX rate, fallback to {}", DEFAULT_RATE);
    return DEFAULT_RATE;
  }

  private Optional<Double> readRateFromEnv() {
    String value = System.getenv("BOBBUY_FX_JPY_CNY_RATE");
    if (value == null || value.isBlank()) {
      value = System.getenv("BOBBUY_FX_CURRENT_RATE");
    }
    return parsePositiveDouble(value, "environment variable");
  }

  private Optional<Double> fetchRateFromBraveSearch() {
    String apiKey = System.getenv("BRAVE_SEARCH_API_KEY");
    if (apiKey == null || apiKey.isBlank()) {
      log.warn("BRAVE_SEARCH_API_KEY is not configured, skip Brave Search rate lookup");
      return Optional.empty();
    }

    String endpoint = System.getenv("BRAVE_SEARCH_ENDPOINT");
    if (endpoint == null || endpoint.isBlank()) {
      endpoint = DEFAULT_BRAVE_ENDPOINT;
    }

    String query = URLEncoder.encode(QUERY, StandardCharsets.UTF_8);
    URI uri = URI.create(endpoint + "?q=" + query + "&count=5");
    HttpRequest request = HttpRequest.newBuilder(uri)
        .timeout(Duration.ofSeconds(3))
        .header("Accept", "application/json")
        .header("X-Subscription-Token", apiKey)
        .GET()
        .build();
    try {
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        log.warn("Brave Search returned non-2xx status: {}", response.statusCode());
        return Optional.empty();
      }
      return extractRateFromText(response.body());
    } catch (IOException | InterruptedException ex) {
      if (ex instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      log.warn("Failed to query Brave Search for FX rate: {}", ex.getMessage());
      return Optional.empty();
    }
  }

  private Optional<Double> extractRateFromText(String payload) {
    if (payload == null || payload.isBlank()) {
      return Optional.empty();
    }
    Matcher matcher = RATE_PATTERN.matcher(payload);
    while (matcher.find()) {
      Optional<Double> parsed = parsePositiveDouble(matcher.group(1), "Brave Search response");
      if (parsed.isPresent()
          && parsed.get() > MIN_REASONABLE_EXCHANGE_RATE
          && parsed.get() < MAX_REASONABLE_EXCHANGE_RATE) {
        return parsed;
      }
    }
    log.warn("Unable to parse FX rate from Brave Search payload");
    return Optional.empty();
  }

  private Optional<Double> parsePositiveDouble(String value, String source) {
    if (value == null || value.isBlank()) {
      return Optional.empty();
    }
    try {
      double parsed = Double.parseDouble(value.trim());
      if (parsed > 0D) {
        return Optional.of(parsed);
      }
      log.warn("Invalid FX rate '{}' from {}, expected positive number", value, source);
    } catch (NumberFormatException ex) {
      log.warn("Failed to parse FX rate '{}' from {}", value, source);
    }
    return Optional.empty();
  }
}
