package com.bobbuy.api;

import com.bobbuy.model.Trip;
import com.bobbuy.model.TripStatus;
import com.bobbuy.service.AuditLogService;
import com.bobbuy.service.BobbuyStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;

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

  @Test
  void tripCrudFlow() {
    assertThat(controller.list().getData()).hasSize(1);

    Trip created = controller.create(new Trip(null, 1000L, "HK", "NY", LocalDate.now(), 5, 0, TripStatus.DRAFT, null))
        .getBody().getData();
    assertThat(created.getId()).isNotNull();

    Trip updatedPayload = new Trip(null, 1000L, "LA", "SF", LocalDate.now(), 6, 1, TripStatus.PUBLISHED, null);
    Trip updated = controller.update(created.getId(), updatedPayload).getBody().getData();
    assertThat(updated.getOrigin()).isEqualTo("LA");

    Trip fetched = controller.get(created.getId()).getBody().getData();
    assertThat(fetched.getDestination()).isEqualTo("SF");

    TripReserveRequest reserveRequest = new TripReserveRequest();
    reserveRequest.setQuantity(1);
    Trip reserved = controller.reserve(created.getId(), reserveRequest).getBody().getData();
    assertThat(reserved.getReservedCapacity()).isEqualTo(2);

    TripOrderBulkStatusRequest bulkRequest = new TripOrderBulkStatusRequest();
    bulkRequest.setTargetStatus(com.bobbuy.model.OrderStatus.CONFIRMED);
    assertThat(controller.bulkUpdateOrderStatus(2000L, bulkRequest).getBody().getData()).hasSize(1);

    assertThat(controller.delete(created.getId()).getBody()).isNotNull();
  }
}
