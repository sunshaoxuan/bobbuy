package com.bobbuy.service;

import com.bobbuy.model.Order;
import com.bobbuy.model.OrderStatus;
import com.bobbuy.model.Role;
import com.bobbuy.model.Trip;
import com.bobbuy.model.TripStatus;
import com.bobbuy.model.User;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
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

    Trip trip = new Trip(tripId.getAndIncrement(), agent.getId(), "Tokyo", "Shanghai", LocalDate.now().plusDays(5), 6, TripStatus.PUBLISHED);
    trips.put(trip.getId(), trip);

    Order order = new Order(orderId.getAndIncrement(), customer.getId(), trip.getId(), "Matcha Kit", 2, 32.5, OrderStatus.CONFIRMED);
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
    trips.put(trip.getId(), trip);
    return trip;
  }

  public Optional<Trip> updateTrip(Long id, Trip trip) {
    if (!trips.containsKey(id)) {
      return Optional.empty();
    }
    trip.setId(id);
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
    orders.put(order.getId(), order);
    return order;
  }

  public Optional<Order> updateOrder(Long id, Order order) {
    if (!orders.containsKey(id)) {
      return Optional.empty();
    }
    order.setId(id);
    orders.put(id, order);
    return Optional.of(order);
  }

  public boolean deleteOrder(Long id) {
    return orders.remove(id) != null;
  }

  public double calculateGmv() {
    return orders.values().stream()
        .mapToDouble(order -> order.getUnitPrice() * order.getQuantity())
        .sum();
  }
}
