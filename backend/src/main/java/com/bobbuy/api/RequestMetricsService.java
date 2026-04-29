package com.bobbuy.api;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.LongAdder;

@Service
public class RequestMetricsService {
  private static final int MAX_SAMPLES = 200;
  private static final String LOGIN_ENDPOINT = "POST /api/auth/login";
  private final Map<String, Deque<Long>> latencySamples = new ConcurrentHashMap<>();
  private final Map<String, LongAdder> requestCounts = new ConcurrentHashMap<>();
  private final Map<String, LongAdder> statusCounts = new ConcurrentHashMap<>();
  private final Map<String, LongAdder> fourHundredsByEndpoint = new ConcurrentHashMap<>();
  private final Map<String, LongAdder> fiveHundredsByEndpoint = new ConcurrentHashMap<>();

  public void record(String method, String path, long costMs) {
    record(method, path, costMs, 200);
  }

  public void record(String method, String path, long costMs, int status) {
    String key = method + " " + path;
    Deque<Long> samples = latencySamples.computeIfAbsent(key, ignored -> new ConcurrentLinkedDeque<>());
    samples.addLast(costMs);
    while (samples.size() > MAX_SAMPLES) {
      samples.pollFirst();
    }
    requestCounts.computeIfAbsent(key, ignored -> new LongAdder()).increment();
    statusCounts.computeIfAbsent(statusBucket(status), ignored -> new LongAdder()).increment();
    if (status >= 400 && status < 500) {
      fourHundredsByEndpoint.computeIfAbsent(key, ignored -> new LongAdder()).increment();
    }
    if (status >= 500) {
      fiveHundredsByEndpoint.computeIfAbsent(key, ignored -> new LongAdder()).increment();
    }
  }

  public Map<String, Long> p95ByEndpoint() {
    return percentileByEndpoint(0.95);
  }

  public Map<String, Long> p99ByEndpoint() {
    return percentileByEndpoint(0.99);
  }

  public List<String> topSlowEndpoints(int limit) {
    Map<String, Long> p95 = p95ByEndpoint();
    List<Map.Entry<String, Long>> entries = new ArrayList<>(p95.entrySet());
    entries.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
    List<String> result = new ArrayList<>();
    for (int i = 0; i < Math.min(limit, entries.size()); i++) {
      result.add(entries.get(i).getKey());
    }
    return result;
  }

  public Map<String, Long> requestCountsByEndpoint() {
    return longMap(requestCounts);
  }

  public Map<String, Long> statusCounts() {
    return longMap(statusCounts);
  }

  public Map<String, Long> http4xxByEndpoint() {
    return longMap(fourHundredsByEndpoint);
  }

  public Map<String, Long> http5xxByEndpoint() {
    return longMap(fiveHundredsByEndpoint);
  }

  public double overall5xxRate() {
    long totalRequests = requestCounts.values().stream().mapToLong(LongAdder::sum).sum();
    if (totalRequests == 0) {
      return 0D;
    }
    long totalFiveHundreds = fiveHundredsByEndpoint.values().stream().mapToLong(LongAdder::sum).sum();
    return (double) totalFiveHundreds / totalRequests;
  }

  public long loginFailureCount() {
    return fourHundredsByEndpoint.entrySet().stream()
        .filter(entry -> entry.getKey().startsWith(LOGIN_ENDPOINT))
        .mapToLong(entry -> entry.getValue().sum())
        .sum();
  }

  private Map<String, Long> percentileByEndpoint(double percentile) {
    Map<String, Long> result = new ConcurrentHashMap<>();
    latencySamples.forEach((key, samples) -> {
      if (samples.isEmpty()) {
        return;
      }
      List<Long> sorted = new ArrayList<>(samples);
      Collections.sort(sorted);
      int index = Math.max((int) Math.ceil(percentile * sorted.size()) - 1, 0);
      result.put(key, sorted.get(index));
    });
    return result;
  }

  private Map<String, Long> longMap(Map<String, LongAdder> counters) {
    Map<String, Long> result = new ConcurrentHashMap<>();
    counters.forEach((key, counter) -> result.put(key, counter.sum()));
    return result;
  }

  private String statusBucket(int status) {
    if (status >= 500) {
      return "5xx";
    }
    if (status >= 400) {
      return "4xx";
    }
    if (status >= 300) {
      return "3xx";
    }
    if (status >= 200) {
      return "2xx";
    }
    return "1xx";
  }
}
