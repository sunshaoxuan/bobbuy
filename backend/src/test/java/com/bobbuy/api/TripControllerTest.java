package com.bobbuy.api;

import com.bobbuy.model.Trip;
import com.bobbuy.model.TripStatus;
import com.bobbuy.service.AuditLogService;
import com.bobbuy.service.BobbuyStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class TripControllerTest {
  private AuditLogService auditLogService;
  private BobbuyStore store;
  private TripController controller;

  @BeforeEach
  void setUp() {
    auditLogService = new AuditLogService();
    store = new BobbuyStore(auditLogService);
    store.seed();
    controller = new TripController(store);
  }

  @Test
  void updateStatusCreatesAuditLogEntry() {
    int before = auditLogService.listLogs().size();
    TripStatusRequest request = new TripStatusRequest();
    request.setStatus(TripStatus.IN_PROGRESS);

    ResponseEntity<com.bobbuy.api.response.ApiResponse<Trip>> response = controller.updateStatus(2000L, request);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getData()).isNotNull();

    assertThat(auditLogService.listLogs().size()).isEqualTo(before + 1);
    assertThat(auditLogService.listLogs().get(before).getEntityType()).isEqualTo("TRIP");
  }
}
