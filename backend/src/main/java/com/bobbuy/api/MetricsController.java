package com.bobbuy.api;

import com.bobbuy.api.response.ApiResponse;
import com.bobbuy.model.Metrics;
import com.bobbuy.service.BobbuyStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class MetricsController {
  private final BobbuyStore store;
  private final RequestMetricsService requestMetricsService;

  public MetricsController(BobbuyStore store, RequestMetricsService requestMetricsService) {
    this.store = store;
    this.requestMetricsService = requestMetricsService;
  }

  @GetMapping("/api/metrics")
  public ApiResponse<Metrics> metrics() {
    Map<String, Integer> statusCounts = store.orderStatusCounts().entrySet().stream()
        .collect(Collectors.toMap(entry -> entry.getKey().name(), Map.Entry::getValue));
    Metrics metrics = new Metrics(
        store.listUsers().size(),
        store.listTrips().size(),
        store.listOrders().size(),
        store.calculateGmv(),
        statusCounts,
        requestMetricsService.p95ByEndpoint(),
        requestMetricsService.p99ByEndpoint(),
        requestMetricsService.topSlowEndpoints(3));
    return ApiResponse.success(metrics);
  }
}
