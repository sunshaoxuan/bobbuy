package com.bobbuy.service;

import com.bobbuy.api.response.ApiException;
import com.bobbuy.api.response.ErrorCode;
import com.bobbuy.model.Order;
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
  private final Map<Long, User> users = new ConcurrentHashMap<>();
  private final Map<Long, Trip> trips = new ConcurrentHashMap<>();
  private final Map<Long, Order> orders = new ConcurrentHashMap<>();

  private final AtomicLong userId = new AtomicLong(1000);
  private final AtomicLong tripId = new AtomicLong(2000);
  private final AtomicLong orderId = new AtomicLong(3000);

  @PostConstruct
  public void seed() {
    User agent = new User(userId.getAndIncrement(), "Aiko Tan", Role.AGENT, 4.8);
    User customer = new User(userId.getAndIncrement(), "Chen Li", Role.CUSTOMER, 4.6);
    users.put(agent.getId(), agent);
    users.put(customer.getId(), customer);

    Trip trip = new Trip(tripId.getAndIncrement(), agent.getId(), "Tokyo", "Shanghai", LocalDate.now().plusDays(5), 6, 1,
        TripStatus.PUBLISHED, LocalDateTime.now());
    trips.put(trip.getId(), trip);

    Order order = new Order(orderId.getAndIncrement(), customer.getId(), trip.getId(), "Matcha Kit", 2, 32.5, 6.0, 2.3, "CNY",
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

  public boolean deleteTrip(Long id) {
    return trips.remove(id) != null;
  }

  public List<Order> listOrders() {
    return new ArrayList<>(orders.values());
  }

  public Optional<Order> getOrder(Long id) {
    return Optional.ofNullable(orders.get(id));
  }

  public Order createOrder(Order order) {
    order.setId(orderId.getAndIncrement());
    order.setStatusUpdatedAt(LocalDateTime.now());
    orders.put(order.getId(), order);
    return order;
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
    Order order = getOrder(id).orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "订单不存在"));
    if (!isValidStatusTransition(order.getStatus(), nextStatus)) {
      throw new ApiException(ErrorCode.INVALID_STATUS, "非法的订单状态流转");
    }
    order.setStatus(nextStatus);
    order.setStatusUpdatedAt(LocalDateTime.now());
    orders.put(id, order);
    return order;
  }

  public boolean deleteOrder(Long id) {
    return orders.remove(id) != null;
  }

  public Trip reserveTripCapacity(Long id, int quantity) {
    Trip trip = getTrip(id).orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "行程不存在"));
    int remaining = trip.getRemainingCapacity();
    if (quantity <= 0) {
      throw new ApiException(ErrorCode.INVALID_REQUEST, "预留数量必须大于 0");
    }
    if (remaining < quantity) {
      throw new ApiException(ErrorCode.CAPACITY_NOT_ENOUGH, "行程容量不足");
    }
    trip.setReservedCapacity(trip.getReservedCapacity() + quantity);
    trip.setStatusUpdatedAt(LocalDateTime.now());
    trips.put(id, trip);
    return trip;
  }

  public double calculateGmv() {
    return orders.values().stream()
        .mapToDouble(order -> order.getUnitPrice() * order.getQuantity())
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
