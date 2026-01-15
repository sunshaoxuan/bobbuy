package com.bobbuy.service;

import com.bobbuy.api.response.ApiException;
import com.bobbuy.api.response.ErrorCode;
import com.bobbuy.model.OrderStatus;
import com.bobbuy.model.Trip;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BobbuyStoreTest {
  private BobbuyStore store;
  private AuditLogService auditLogService;

  @BeforeEach
  void setUp() {
    auditLogService = new AuditLogService();
    store = new BobbuyStore(auditLogService);
    store.seed();
  }

  @Test
  void updatesOrderStatusAndLogsCounts() {
    store.updateOrderStatus(3000L, OrderStatus.PURCHASED);
    assertThat(store.orderStatusCounts().get(OrderStatus.PURCHASED)).isEqualTo(1);
  }

  @Test
  void reserveTripCapacityRejectsWhenCapacityInsufficient() {
    assertThatThrownBy(() -> store.reserveTripCapacity(2000L, 99))
        .isInstanceOf(ApiException.class)
        .satisfies(error -> {
          ApiException ex = (ApiException) error;
          assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CAPACITY_NOT_ENOUGH);
          assertThat(ex.getMessageKey()).isEqualTo("error.trip.capacity_not_enough");
        });
  }

  @Test
  void updateOrderStatusRejectsMissingOrder() {
    assertThatThrownBy(() -> store.updateOrderStatus(9999L, OrderStatus.CONFIRMED))
        .isInstanceOf(ApiException.class)
        .satisfies(error -> {
          ApiException ex = (ApiException) error;
          assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
          assertThat(ex.getMessageKey()).isEqualTo("error.order.not_found");
        });
  }

  @Test
  void reserveTripCapacityRejectsInvalidQuantity() {
    assertThatThrownBy(() -> store.reserveTripCapacity(2000L, 0))
        .isInstanceOf(ApiException.class)
        .satisfies(error -> {
          ApiException ex = (ApiException) error;
          assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
          assertThat(ex.getMessageKey()).isEqualTo("error.trip.invalid_quantity");
        });

    assertThatThrownBy(() -> store.reserveTripCapacity(2000L, -2))
        .isInstanceOf(ApiException.class)
        .satisfies(error -> {
          ApiException ex = (ApiException) error;
          assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
          assertThat(ex.getMessageKey()).isEqualTo("error.trip.invalid_quantity");
        });
  }

  @Test
  void reserveTripCapacityAllowsExactRemainingCapacity() {
    Trip trip = store.reserveTripCapacity(2000L, 5);
    assertThat(trip.getRemainingCapacity()).isEqualTo(0);
  }

  @Test
  void reserveTripCapacityPreventsOverbookingUnderConcurrency() throws InterruptedException {
    ExecutorService executor = Executors.newFixedThreadPool(10);
    CountDownLatch latch = new CountDownLatch(10);
    AtomicInteger successCount = new AtomicInteger(0);

    try {
      for (int i = 0; i < 10; i++) {
        executor.submit(() -> {
          try {
            store.reserveTripCapacity(2000L, 1);
            successCount.incrementAndGet();
          } catch (ApiException ignored) {
            // Expected for some threads due to capacity limit.
          } finally {
            latch.countDown();
          }
        });
      }
      assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    } finally {
      executor.shutdownNow();
    }

    assertThat(successCount.get()).isLessThanOrEqualTo(5);
    assertThat(store.getTrip(2000L).orElseThrow().getRemainingCapacity()).isGreaterThanOrEqualTo(0);
  }

  @Test
  void updateTripStatusCreatesAuditLogEntry() {
    int before = auditLogService.listLogs().size();
    store.updateTripStatus(2000L, com.bobbuy.model.TripStatus.IN_PROGRESS);
    assertThat(auditLogService.listLogs().size()).isEqualTo(before + 1);
    assertThat(auditLogService.listLogs().get(before).getEntityType()).isEqualTo("TRIP");
  }
}
