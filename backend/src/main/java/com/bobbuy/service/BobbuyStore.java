package com.bobbuy.service;

import com.bobbuy.api.response.ApiException;
import com.bobbuy.api.response.ErrorCode;
import com.bobbuy.model.Order;
import com.bobbuy.model.OrderItem;
import com.bobbuy.model.OrderStatus;
import com.bobbuy.model.Role;
import com.bobbuy.model.Trip;
import com.bobbuy.model.TripStatus;
import com.bobbuy.model.User;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class BobbuyStore {
  private static final Long SYSTEM_USER_ID = 0L;
  private final Map<Long, User> users = new ConcurrentHashMap<>();
  private final Map<Long, Trip> trips = new ConcurrentHashMap<>();
  private final Map<Long, Order> orders = new ConcurrentHashMap<>();
  private final AuditLogService auditLogService;

  private final AtomicLong userId = new AtomicLong(1000);
  private final AtomicLong tripId = new AtomicLong(2000);
  private final AtomicLong orderId = new AtomicLong(3000);
  private final AtomicLong itemId = new AtomicLong(4000);

  public BobbuyStore(AuditLogService auditLogService) {
    this.auditLogService = auditLogService;
  }

  @PostConstruct
  public void seed() {
    User agent = new User(userId.getAndIncrement(), "Aiko Tan", Role.AGENT, 4.8);
    User customer = new User(userId.getAndIncrement(), "Chen Li", Role.CUSTOMER, 4.6);
    users.put(agent.getId(), agent);
    users.put(customer.getId(), customer);

    Trip trip = new Trip(tripId.getAndIncrement(), agent.getId(), "Tokyo", "Shanghai", LocalDate.now().plusDays(5), 6,
        1,
        TripStatus.PUBLISHED, LocalDateTime.now());
    trips.put(trip.getId(), trip);

    List<OrderItem> items = new ArrayList<>();
    items.add(new OrderItem(itemId.getAndIncrement(), "Matcha Kit", 2, 32.5, false));
    Order order = new Order(orderId.getAndIncrement(), customer.getId() + "-20260117-001", customer.getId(),
        trip.getId(), items, 6.0, 2.3, "CNY",
        OrderStatus.CONFIRMED, LocalDateTime.now());
    orders.put(order.getId(), order);
  }

  public List<User> listUsers() {
    return new ArrayList<>(users.values());
  }

  public Optional<User> getUser(Long id) {
    return Optional.ofNullable(users.get(id));
  }

  public User createUser(User user) {
    user.setId(userId.getAndIncrement());
    users.put(user.getId(), user);
    return user;
  }

  public Optional<User> updateUser(Long id, User user) {
    if (!users.containsKey(id)) {
      return Optional.empty();
    }
    user.setId(id);
    users.put(id, user);
    return Optional.of(user);
  }

  public boolean deleteUser(Long id) {
    return users.remove(id) != null;
  }

  public List<Trip> listTrips() {
    return new ArrayList<>(trips.values());
  }

  public Optional<Trip> getTrip(Long id) {
    return Optional.ofNullable(trips.get(id));
  }

  public Trip createTrip(Trip trip) {
    trip.setId(tripId.getAndIncrement());
    trip.setReservedCapacity(Math.max(trip.getReservedCapacity(), 0));
    trip.setStatusUpdatedAt(LocalDateTime.now());
    trips.put(trip.getId(), trip);
    return trip;
  }

  public Optional<Trip> updateTrip(Long id, Trip trip) {
    if (!trips.containsKey(id)) {
      return Optional.empty();
    }
    trip.setId(id);
    trip.setStatusUpdatedAt(LocalDateTime.now());
    trips.put(id, trip);
    return Optional.of(trip);
  }

  public Trip updateTripStatus(Long id, TripStatus nextStatus) {
    Trip trip = getTrip(id)
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "error.trip.not_found"));
    String previousStatus = trip.getStatus().name();
    trip.setStatus(nextStatus);
    trip.setStatusUpdatedAt(LocalDateTime.now());
    trips.put(id, trip);
    auditLogService.logStatusChange("TRIP", id, previousStatus, nextStatus.name(), SYSTEM_USER_ID);
    return trip;
  }

  public boolean deleteTrip(Long id) {
    return trips.remove(id) != null;
  }

  public List<Order> listOrders() {
    return new ArrayList<>(orders.values());
  }

  public Optional<Order> getOrder(Long id) {
    return Optional.ofNullable(orders.get(id));
  }

  public synchronized Order createOrder(Order order) {
    // 幂等与合并逻辑
    Optional<Order> existing = orders.values().stream()
        .filter(o -> o.getBusinessKey().equals(order.getBusinessKey()))
        .findFirst();

    if (existing.isPresent()) {
      Order target = existing.get();
      // 合并项目
      for (OrderItem newItem : order.getItems()) {
        mergeItem(target, newItem);
      }
      target.setStatusUpdatedAt(LocalDateTime.now());
      return target;
    } else {
      order.setId(orderId.getAndIncrement());
      for (OrderItem item : order.getItems()) {
        item.setId(itemId.getAndIncrement());
      }
      order.setStatusUpdatedAt(LocalDateTime.now());
      orders.put(order.getId(), order);
      return order;
    }
  }

  private void mergeItem(Order target, OrderItem newItem) {
    if (newItem.isVariable()) {
      // 非标品直接追加
      newItem.setId(itemId.getAndIncrement());
      target.addItem(newItem);
    } else {
      // 标品尝试累加
      Optional<OrderItem> match = target.getItems().stream()
          .filter(i -> !i.isVariable() && i.getItemName().equals(newItem.getItemName())
              && i.getUnitPrice() == newItem.getUnitPrice())
          .findFirst();

      if (match.isPresent()) {
        OrderItem existingItem = match.get();
        existingItem.setQuantity(existingItem.getQuantity() + newItem.getQuantity());
      } else {
        newItem.setId(itemId.getAndIncrement());
        target.addItem(newItem);
      }
    }
  }

  public Optional<Order> updateOrder(Long id, Order order) {
    if (!orders.containsKey(id)) {
      return Optional.empty();
    }
    order.setId(id);
    order.setStatusUpdatedAt(LocalDateTime.now());
    orders.put(id, order);
    return Optional.of(order);
  }

  public Order updateOrderStatus(Long id, OrderStatus nextStatus) {
    Order order = getOrder(id)
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "error.order.not_found"));
    if (!isValidStatusTransition(order.getStatus(), nextStatus)) {
      throw new ApiException(ErrorCode.INVALID_STATUS, "error.order.invalid_status");
    }
    String previousStatus = order.getStatus().name();
    order.setStatus(nextStatus);
    order.setStatusUpdatedAt(LocalDateTime.now());
    orders.put(id, order);
    auditLogService.logStatusChange("ORDER", id, previousStatus, nextStatus.name(), SYSTEM_USER_ID);
    return order;
  }

  public boolean deleteOrder(Long id) {
    return orders.remove(id) != null;
  }

  public synchronized Trip reserveTripCapacity(Long id, int quantity) {
    Trip trip = getTrip(id)
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "error.trip.not_found"));
    int remaining = trip.getRemainingCapacity();
    if (quantity <= 0) {
      throw new ApiException(ErrorCode.INVALID_REQUEST, "error.trip.invalid_quantity");
    }
    if (remaining < quantity) {
      throw new ApiException(ErrorCode.CAPACITY_NOT_ENOUGH, "error.trip.capacity_not_enough");
    }
    trip.setReservedCapacity(trip.getReservedCapacity() + quantity);
    trip.setStatusUpdatedAt(LocalDateTime.now());
    trips.put(id, trip);
    return trip;
  }

  public double calculateGmv() {
    return orders.values().stream()
        .flatMap(order -> order.getItems().stream())
        .mapToDouble(item -> item.getUnitPrice() * item.getQuantity())
        .sum();
  }

  public Map<OrderStatus, Integer> orderStatusCounts() {
    Map<OrderStatus, Integer> counts = new EnumMap<>(OrderStatus.class);
    orders.values().forEach(order -> counts.merge(order.getStatus(), 1, Integer::sum));
    return counts;
  }

  private boolean isValidStatusTransition(OrderStatus current, OrderStatus next) {
    return switch (current) {
      case NEW -> next == OrderStatus.CONFIRMED;
      case CONFIRMED -> next == OrderStatus.PURCHASED;
      case PURCHASED -> next == OrderStatus.DELIVERED;
      case DELIVERED -> next == OrderStatus.SETTLED;
      case SETTLED -> false;
    };
  }
}
