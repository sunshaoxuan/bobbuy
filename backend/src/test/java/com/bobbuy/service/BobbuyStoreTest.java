package com.bobbuy.service;

import com.bobbuy.api.response.ApiException;
import com.bobbuy.api.response.ErrorCode;
import com.bobbuy.model.User;
import com.bobbuy.model.Role;
import com.bobbuy.model.Order;
import com.bobbuy.model.OrderStatus;
import com.bobbuy.model.Trip;
import com.bobbuy.model.TripStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.bobbuy.model.OrderItem;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
  void userLifecycleTests() {
    // List
    assertThat(store.listUsers()).hasSize(2);

    // Create
    User newUser = new User(null, "Test User", Role.CUSTOMER, 5.0);
    User created = store.createUser(newUser);
    assertThat(created.getId()).isEqualTo(1002L);
    assertThat(store.getUser(1002L)).isPresent();

    // Update
    created.setName("Updated Name");
    Optional<User> updated = store.updateUser(1002L, created);
    assertThat(updated).isPresent();
    assertThat(updated.get().getName()).isEqualTo("Updated Name");

    // Update missing
    assertThat(store.updateUser(9999L, created)).isEmpty();

    // Delete
    assertThat(store.deleteUser(1002L)).isTrue();
    assertThat(store.deleteUser(1002L)).isFalse();
  }

  @Test
  void tripLifecycleTests() {
    // List
    assertThat(store.listTrips()).hasSize(1);

    // Create
    Trip newTrip = new Trip(null, 1000L, "HK", "NY", LocalDate.now(), 5, -1, TripStatus.DRAFT, null);
    Trip created = store.createTrip(newTrip);
    assertThat(created.getId()).isEqualTo(2001L);
    assertThat(created.getReservedCapacity()).isEqualTo(0); // Test Math.max logic

    // Update
    created.setOrigin("Macau");
    Optional<Trip> updated = store.updateTrip(2001L, created);
    assertThat(updated).isPresent();
    assertThat(updated.get().getOrigin()).isEqualTo("Macau");

    // Update missing
    assertThat(store.updateTrip(9999L, created)).isEmpty();

    // Update status missing
    assertThatThrownBy(() -> store.updateTripStatus(9999L, TripStatus.COMPLETED))
        .isInstanceOf(ApiException.class);

    // Delete
    assertThat(store.deleteTrip(2001L)).isTrue();
    assertThat(store.deleteTrip(2001L)).isFalse();
  }

  @Test
  void orderLifecycleAndCalculations() {
    // List & GMV
    assertThat(store.listOrders()).hasSize(1);
    assertThat(store.calculateGmv()).isEqualTo(32.5 * 2);

    // Create
    OrderItem item = new OrderItem(null, "Tea", 1, 10.0, false);
    Order newOrder = new Order(null, "1001-EVENT-001", 1001L, 2000L, List.of(item), 1.0, 0.5, "USD", OrderStatus.NEW,
        null);
    Order created = store.createOrder(newOrder);
    assertThat(created.getId()).isEqualTo(3001L);
    assertThat(created.getItems()).hasSize(1);
    assertThat(created.getItems().get(0).getId()).isNotNull();

    // Update
    created.setBusinessKey("1001-EVENT-UPDATED");
    Optional<Order> updated = store.updateOrder(3001L, created);
    assertThat(updated).isPresent();

    // Update missing
    assertThat(store.updateOrder(9999L, created)).isEmpty();

    // Valid transitions
    store.updateOrderStatus(3001L, OrderStatus.CONFIRMED);
    store.updateOrderStatus(3001L, OrderStatus.PURCHASED);
    store.updateOrderStatus(3001L, OrderStatus.DELIVERED);
    store.updateOrderStatus(3001L, OrderStatus.SETTLED);

    // Invalid transitions
    assertThatThrownBy(() -> store.updateOrderStatus(3001L, OrderStatus.NEW))
        .isInstanceOf(ApiException.class);

    // Delete
    assertThat(store.deleteOrder(3001L)).isTrue();
    assertThat(store.deleteOrder(3001L)).isFalse();
  }

  @Test
  void orderBusinessKeyMergeLogic() {
    // 1. Create initial order
    OrderItem item1 = new OrderItem(null, "Milk", 1, 5.0, false); // Standard
    OrderItem item2 = new OrderItem(null, "Meat", 1, 12.0, true); // Variable
    Order order1 = new Order(null, "C1-E1", 1001L, 2000L, new ArrayList<>(List.of(item1, item2)), 1.0, 0.5, "CNY",
        OrderStatus.NEW, null);
    store.createOrder(order1);

    // 2. Submit same business key with same standard item and new variable item
    OrderItem item3 = new OrderItem(null, "Milk", 2, 5.0, false); // Same standard
    OrderItem item4 = new OrderItem(null, "Meat", 1, 15.0, true); // New variable
    Order order2 = new Order(null, "C1-E1", 1001L, 2000L, new ArrayList<>(List.of(item3, item4)), 1.0, 0.5, "CNY",
        OrderStatus.NEW, null);
    Order merged = store.createOrder(order2);

    assertThat(store.listOrders()).hasSize(2); // Seed(1) + Created(1) = 2
    assertThat(merged.getItems()).hasSize(3); // Milk(merged) + Meat1 + Meat2

    OrderItem milk = merged.getItems().stream().filter(i -> i.getItemName().equals("Milk")).findFirst().get();
    assertThat(milk.getQuantity()).isEqualTo(3); // 1 + 2

    long meatCount = merged.getItems().stream().filter(i -> i.getItemName().equals("Meat")).count();
    assertThat(meatCount).isEqualTo(2); // Variable items don't merge
  }

  @Test
  void orderStatusCountsWithEmpty() {
    store.deleteOrder(3000L);
    assertThat(store.orderStatusCounts()).isEmpty();
  }

  @Test
  void reserveTripCapacityNotFound() {
    assertThatThrownBy(() -> store.reserveTripCapacity(9999L, 1))
        .isInstanceOf(ApiException.class);
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
