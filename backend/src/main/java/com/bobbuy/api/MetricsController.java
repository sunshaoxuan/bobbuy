package com.bobbuy.api;

import com.bobbuy.model.Metrics;
import com.bobbuy.service.BobbuyStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MetricsController {
  private final BobbuyStore store;

  public MetricsController(BobbuyStore store) {
    this.store = store;
  }

  @GetMapping("/api/metrics")
  public Metrics metrics() {
    return new Metrics(
        store.listUsers().size(),
        store.listTrips().size(),
        store.listOrders().size(),
        store.calculateGmv());
  }
}
