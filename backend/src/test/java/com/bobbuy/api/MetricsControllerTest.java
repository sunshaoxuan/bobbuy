package com.bobbuy.api;

import com.bobbuy.api.response.ApiResponse;
import com.bobbuy.model.Metrics;
import com.bobbuy.service.BobbuyStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class MetricsControllerTest {
  @Autowired
  private BobbuyStore store;
  private MetricsController controller;
  private RequestMetricsService requestMetricsService;

  @BeforeEach
  void setUp() {
    store.seed();
    requestMetricsService = new RequestMetricsService();
    requestMetricsService.record("GET", "/api/orders", 120, 200);
    requestMetricsService.record("GET", "/api/orders", 240, 500);
    requestMetricsService.record("POST", "/api/auth/login", 80, 401);
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
    assertThat(metrics.getRequestCounts()).containsEntry("GET /api/orders", 2L);
    assertThat(metrics.getStatusCounts()).containsEntry("5xx", 1L);
    assertThat(metrics.getLatencyP95Ms()).containsKey("GET /api/orders");
    assertThat(metrics.getLatencyP99Ms()).containsKey("GET /api/orders");
    assertThat(metrics.getHttp4xxByEndpoint()).containsEntry("POST /api/auth/login", 1L);
    assertThat(metrics.getHttp5xxByEndpoint()).containsEntry("GET /api/orders", 1L);
    assertThat(metrics.getLoginFailureCount()).isEqualTo(1);
    assertThat(metrics.getOverall5xxRate()).isGreaterThan(0);
    assertThat(metrics.getSlowEndpoints()).isNotEmpty();
  }
}
