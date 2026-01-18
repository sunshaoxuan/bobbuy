package com.bobbuy.service;

import com.bobbuy.api.response.ApiException;
import com.bobbuy.api.response.ErrorCode;
import com.bobbuy.model.User;
import com.bobbuy.model.Role;
import com.bobbuy.model.OrderHeader;
import com.bobbuy.model.OrderLine;
import com.bobbuy.model.OrderStatus;
import com.bobbuy.model.Trip;
import com.bobbuy.model.TripStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
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
    // List & GMV (Seeded 1 order with 65.0 amount)
    assertThat(store.listOrders()).hasSize(1);
    assertThat(store.calculateGmv()).isEqualTo(65.0);

    // Create via upsert
    OrderHeader newHeader = new OrderHeader("BUS-X", 1001L, 2000L);
    newHeader.addLine(new OrderLine("SKU-X", "Tea", null, 1, 10.0));
    OrderHeader created = store.upsertOrder(newHeader);
    assertThat(created.getId()).isNotNull();
    assertThat(created.getTotalAmount()).isEqualTo(10.0);

    // Update status
    store.updateOrderStatus(created.getId(), OrderStatus.CONFIRMED);
    assertThat(store.getOrder(created.getId()).get().getStatus()).isEqualTo(OrderStatus.CONFIRMED);

    // Delete
    assertThat(store.deleteOrder(created.getId())).isTrue();
    assertThat(store.listOrders()).hasSize(1);
    assertThat(store.deleteOrder(created.getId())).isFalse();
  }

  @Test
  void updateOrderStatusReservesCapacityWhenConfirmed() {
    Trip tripBefore = store.getTrip(2000L).orElseThrow();
    int reservedBefore = tripBefore.getReservedCapacity();

    OrderHeader newHeader = new OrderHeader("BUS-CAP-1", 1001L, 2000L);
    newHeader.addLine(new OrderLine("SKU-CAP", "Capacity", null, 2, 10.0));
    OrderHeader created = store.upsertOrder(newHeader);

    store.updateOrderStatus(created.getId(), OrderStatus.CONFIRMED);

    Trip tripAfter = store.getTrip(2000L).orElseThrow();
    assertThat(tripAfter.getReservedCapacity()).isEqualTo(reservedBefore + 2);
  }

  @Test
  void upsertOrderReservesCapacityForConfirmedMerges() {
    Trip tripBefore = store.getTrip(2000L).orElseThrow();
    int reservedBefore = tripBefore.getReservedCapacity();

    OrderHeader confirmed = new OrderHeader("BUS-CAP-2", 1001L, 2000L);
    confirmed.setStatus(OrderStatus.CONFIRMED);
    confirmed.addLine(new OrderLine("SKU-CAP", "Capacity", null, 1, 10.0));
    store.upsertOrder(confirmed);

    Trip tripAfterCreate = store.getTrip(2000L).orElseThrow();
    assertThat(tripAfterCreate.getReservedCapacity()).isEqualTo(reservedBefore + 1);

    OrderHeader merged = new OrderHeader("BUS-CAP-2", 1001L, 2000L);
    merged.addLine(new OrderLine("SKU-CAP", "Capacity", null, 3, 10.0));
    store.upsertOrder(merged);

    Trip tripAfterMerge = store.getTrip(2000L).orElseThrow();
    assertThat(tripAfterMerge.getReservedCapacity()).isEqualTo(reservedBefore + 4);
  }

  @Test
  void idempotencyMergeTest() {
    String bizId = "BIZ-999";
    Long custId = 1001L;
    Long tripId = 2000L;

    // 1. First Submission: SKU-A x 2
    OrderHeader h1 = new OrderHeader(bizId, custId, tripId);
    h1.addLine(new OrderLine("SKU-A", "Item A", "Red", 2, 50.0));
    store.upsertOrder(h1);

    // 2. Second Submission (Idempotent): Repeat SKU-A x 1 -> Should merge to 3
    OrderHeader h2 = new OrderHeader(bizId, custId, tripId);
    h2.addLine(new OrderLine("SKU-A", "Item A", "Red", 1, 50.0));
    OrderHeader merged = store.upsertOrder(h2);

    assertThat(merged.getLines()).hasSize(1);
    assertThat(merged.getLines().get(0).getQuantity()).isEqualTo(3);
    assertThat(merged.getTotalAmount()).isEqualTo(150.0);

    // 3. Third Submission: Different SKU -> Should add new line
    OrderHeader h3 = new OrderHeader(bizId, custId, tripId);
    h3.addLine(new OrderLine("SKU-B", "Item B", null, 1, 20.0));
    OrderHeader withB = store.upsertOrder(h3);

    assertThat(withB.getLines()).hasSize(2);
    assertThat(withB.getTotalAmount()).isEqualTo(170.0);

    // 4. Fourth Submission: Same SKU but Different Spec (Green vs Red) -> Should
    // isolate
    OrderHeader h4 = new OrderHeader(bizId, custId, tripId);
    h4.addLine(new OrderLine("SKU-A", "Item A", "Green", 1, 50.0));
    OrderHeader withGreen = store.upsertOrder(h4);

    assertThat(withGreen.getLines()).hasSize(3); // A-Red(3), B(1), A-Green(1)
    assertThat(withGreen.getTotalAmount()).isEqualTo(220.0);
  }

  @Test
  void orderStatusCountsWithEmpty() {
    store.deleteOrder(3000L);
    assertThat(store.orderStatusCounts()).isEmpty();
  }

  @Test
  void listOrdersFiltersByTripId() {
    OrderHeader otherTrip = new OrderHeader("BIZ-TRIP", 1001L, 9999L);
    otherTrip.addLine(new OrderLine("SKU-T", "Tea", null, 1, 10.0));
    store.upsertOrder(otherTrip);

    assertThat(store.listOrders(2000L)).allMatch(order -> order.getTripId().equals(2000L));
    assertThat(store.listOrders(9999L)).allMatch(order -> order.getTripId().equals(9999L));
  }

  @Test
  void updateOrderUpdatesExistingAndRejectsMissing() {
    OrderHeader header = new OrderHeader("BIZ-UPDATE", 1001L, 2000L);
    header.addLine(new OrderLine("SKU-U", "Update", null, 1, 8.0));
    OrderHeader created = store.upsertOrder(header);

    OrderHeader updated = new OrderHeader(created.getBusinessId(), 1001L, 2000L);
    updated.setStatus(OrderStatus.CONFIRMED);
    assertThat(store.updateOrder(created.getId(), updated)).isPresent();
    assertThat(store.getOrder(created.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.CONFIRMED);

    assertThat(store.updateOrder(9999L, updated)).isEmpty();
  }

  @Test
  void getOrderByBusinessIdReturnsOptional() {
    assertThat(store.getOrderByBusinessId("20260117001")).isPresent();
    assertThat(store.getOrderByBusinessId("MISSING")).isEmpty();
  }

  @Test
  void updateOrderStatusRejectsInvalidTransitions() {
    OrderHeader header = new OrderHeader("BIZ-INVALID", 1001L, 2000L);
    header.addLine(new OrderLine("SKU-I", "Invalid", null, 1, 8.0));
    OrderHeader created = store.upsertOrder(header);

    assertThatThrownBy(() -> store.updateOrderStatus(created.getId(), OrderStatus.PURCHASED))
        .isInstanceOf(ApiException.class)
        .satisfies(error -> {
          ApiException ex = (ApiException) error;
          assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATUS);
        });

    created.setStatus(OrderStatus.CONFIRMED);
    store.updateOrder(created.getId(), created);
    assertThatThrownBy(() -> store.updateOrderStatus(created.getId(), OrderStatus.DELIVERED))
        .isInstanceOf(ApiException.class);

    created.setStatus(OrderStatus.PURCHASED);
    store.updateOrder(created.getId(), created);
    assertThatThrownBy(() -> store.updateOrderStatus(created.getId(), OrderStatus.SETTLED))
        .isInstanceOf(ApiException.class);

    created.setStatus(OrderStatus.DELIVERED);
    store.updateOrder(created.getId(), created);
    assertThatThrownBy(() -> store.updateOrderStatus(created.getId(), OrderStatus.CONFIRMED))
        .isInstanceOf(ApiException.class);

    created.setStatus(OrderStatus.SETTLED);
    store.updateOrder(created.getId(), created);
    assertThatThrownBy(() -> store.updateOrderStatus(created.getId(), OrderStatus.NEW))
        .isInstanceOf(ApiException.class);
  }

  @Test
  void reserveTripCapacityNotFound() {
    assertThatThrownBy(() -> store.reserveTripCapacity(9999L, 1))
        .isInstanceOf(ApiException.class);
  }

  @Test
  void updatesOrderStatusAndLogsCounts() {
    store.updateOrderStatus(3000L, OrderStatus.CONFIRMED);
    assertThat(store.orderStatusCounts().get(OrderStatus.CONFIRMED)).isEqualTo(1);
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
