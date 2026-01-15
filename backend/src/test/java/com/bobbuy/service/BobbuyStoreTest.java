package com.bobbuy.service;

import com.bobbuy.api.response.ApiException;
import com.bobbuy.api.response.ErrorCode;
import com.bobbuy.model.OrderStatus;
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

  @BeforeEach
  void setUp() {
    store = new BobbuyStore(new AuditLogService());
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
}
