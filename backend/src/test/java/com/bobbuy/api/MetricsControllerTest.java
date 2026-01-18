package com.bobbuy.api;

import com.bobbuy.api.response.ApiResponse;
import com.bobbuy.model.Metrics;
import com.bobbuy.service.AuditLogService;
import com.bobbuy.service.BobbuyStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MetricsControllerTest {
  private MetricsController controller;
  private RequestMetricsService requestMetricsService;

  @BeforeEach
  void setUp() {
    BobbuyStore store = new BobbuyStore(new AuditLogService());
    store.seed();
    requestMetricsService = new RequestMetricsService();
    requestMetricsService.record("GET", "/api/orders", 120);
    requestMetricsService.record("GET", "/api/orders", 240);
    controller = new MetricsController(store, requestMetricsService);
  }

  @Test
  void metricsIncludesLatencyAndCounts() {
    ApiResponse<Metrics> response = controller.metrics();
    Metrics metrics = response.getData();
    assertThat(metrics.getUsers()).isEqualTo(2);
    assertThat(metrics.getTrips()).isEqualTo(1);
    assertThat(metrics.getOrders()).isEqualTo(1);
    assertThat(metrics.getOrderStatusCounts()).containsKey("NEW");
    assertThat(metrics.getLatencyP95Ms()).containsKey("GET /api/orders");
    assertThat(metrics.getLatencyP99Ms()).containsKey("GET /api/orders");
    assertThat(metrics.getSlowEndpoints()).isNotEmpty();
  }
}
