package com.bobbuy.api;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RequestMetricsServiceTest {

  @Test
  void calculatesPercentilesAndSlowEndpoints() {
    RequestMetricsService service = new RequestMetricsService();
    service.record("GET", "/api/orders", 100);
    service.record("GET", "/api/orders", 200);
    service.record("GET", "/api/orders", 300);
    service.record("POST", "/api/orders", 50);

    Map<String, Long> p95 = service.p95ByEndpoint();
    Map<String, Long> p99 = service.p99ByEndpoint();

    assertThat(p95).containsEntry("GET /api/orders", 300L);
    assertThat(p99).containsEntry("GET /api/orders", 300L);
    assertThat(service.topSlowEndpoints(1)).containsExactly("GET /api/orders");
  }

  @Test
  void handlesEmptySamples() {
    RequestMetricsService service = new RequestMetricsService();
    assertThat(service.p95ByEndpoint()).isEmpty();
    assertThat(service.p99ByEndpoint()).isEmpty();
    assertThat(service.topSlowEndpoints(5)).isEmpty();
  }
}
