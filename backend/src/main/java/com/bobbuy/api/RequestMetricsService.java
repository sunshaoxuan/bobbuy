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

@Service
public class RequestMetricsService {
  private static final int MAX_SAMPLES = 200;
  private final Map<String, Deque<Long>> latencySamples = new ConcurrentHashMap<>();

  public void record(String method, String path, long costMs) {
    String key = method + " " + path;
    Deque<Long> samples = latencySamples.computeIfAbsent(key, ignored -> new ConcurrentLinkedDeque<>());
    samples.addLast(costMs);
    while (samples.size() > MAX_SAMPLES) {
      samples.pollFirst();
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
}
