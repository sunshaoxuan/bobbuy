package com.bobbuy.service;

import com.bobbuy.api.response.ApiException;
import com.bobbuy.api.response.ErrorCode;
import com.bobbuy.model.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
}
