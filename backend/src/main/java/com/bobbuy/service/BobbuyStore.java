package com.bobbuy.service;

import com.bobbuy.api.response.ApiException;
import com.bobbuy.api.response.ErrorCode;
import com.bobbuy.api.ProcurementItemResponse;
import com.bobbuy.model.OrderHeader;
import com.bobbuy.model.OrderLine;
import com.bobbuy.model.OrderStatus;
import com.bobbuy.model.PaymentStatus;
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
import java.util.stream.Collectors;

@Service
public class BobbuyStore {
    private static final Long SYSTEM_USER_ID = 0L;
    private final Map<Long, User> users = new ConcurrentHashMap<>();
    private final Map<Long, Trip> trips = new ConcurrentHashMap<>();
    private final Map<String, OrderHeader> orders = new ConcurrentHashMap<>();
    private final Map<Long, OrderHeader> ordersById = new ConcurrentHashMap<>();
    private final AuditLogService auditLogService;

    private final AtomicLong userId = new AtomicLong(1000);
    private final AtomicLong tripId = new AtomicLong(2000);
    private final AtomicLong orderId = new AtomicLong(3000);

    public BobbuyStore(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @PostConstruct
    public void seed() {
        User agent = new User(userId.getAndIncrement(), "Aiko Tan", Role.AGENT, 4.8);
        User customer = new User(userId.getAndIncrement(), "Chen Li", Role.CUSTOMER, 4.6);
        users.put(agent.getId(), agent);
        users.put(customer.getId(), customer);

        Trip trip = new Trip(tripId.getAndIncrement(), agent.getId(), "Tokyo", "Shanghai", LocalDate.now().plusDays(5),
                6, 1,
                TripStatus.PUBLISHED, LocalDateTime.now());
        trips.put(trip.getId(), trip);

        OrderHeader header = new OrderHeader("20260117001", customer.getId(), trip.getId());
        header.setId(orderId.getAndIncrement());

        OrderLine line = new OrderLine("SKU001", "Matcha Kit", null, 2, 32.5);
        header.addLine(line);
        header.setTotalAmount(65.0);

        orders.put(header.getBusinessId(), header);
        ordersById.put(header.getId(), header);
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

    public List<OrderHeader> listOrders() {
        return listOrders(null);
    }

    public List<OrderHeader> listOrders(Long tripId) {
        if (tripId == null) {
            return new ArrayList<>(orders.values());
        }
        return orders.values().stream()
                .filter(order -> tripId.equals(order.getTripId()))
                .collect(Collectors.toList());
    }

    public Optional<OrderHeader> getOrder(Long id) {
        return Optional.ofNullable(ordersById.get(id));
    }

    public Optional<OrderHeader> getOrderByBusinessId(String businessId) {
        return Optional.ofNullable(orders.get(businessId));
    }

    /**
     * 核心幂等合并算法 (Upsert Logic)
     * 严格遵循 ARCH-11 第 3 节
     */
    public synchronized OrderHeader upsertOrder(OrderHeader headerInput) {
        if (headerInput.getPaymentStatus() == null) {
            headerInput.setPaymentStatus(PaymentStatus.UNPAID);
        }
        OrderHeader existing = orders.get(headerInput.getBusinessId());

        if (existing == null) {
            // 新建逻辑
            if (isAtLeastConfirmed(headerInput.getStatus())) {
                reserveTripCapacity(headerInput.getTripId(), calculateTotalQuantity(headerInput));
            }
            headerInput.setId(orderId.getAndIncrement());
            headerInput.setStatusUpdatedAt(LocalDateTime.now());
            // 处理行 ID
            for (OrderLine line : headerInput.getLines()) {
                line.setId(orderId.getAndIncrement()); // 借用同一计数器
                line.setHeaderId(headerInput.getId());
            }
            recalculateTotal(headerInput);
            orders.put(headerInput.getBusinessId(), headerInput);
            ordersById.put(headerInput.getId(), headerInput);
            return headerInput;
        } else {
            // 合并逻辑 - 业务规则: SKU + Spec 匹配则累加
            int additionalQuantity = calculateTotalQuantity(headerInput);
            if (additionalQuantity > 0 && isAtLeastConfirmed(existing.getStatus())) {
                reserveTripCapacity(existing.getTripId(), additionalQuantity);
            }
            for (OrderLine newLine : headerInput.getLines()) {
                Optional<OrderLine> match = existing.getLines().stream()
                        .filter(l -> l.canMergeWith(newLine))
                        .findFirst();

                if (match.isPresent()) {
                    OrderLine existingLine = match.get();
                    existingLine.setQuantity(existingLine.getQuantity() + newLine.getQuantity());
                } else {
                    newLine.setId(orderId.getAndIncrement());
                    existing.addLine(newLine);
                }
            }
            existing.setStatusUpdatedAt(LocalDateTime.now());
            recalculateTotal(existing);
            return existing;
        }
    }

    private void recalculateTotal(OrderHeader header) {
        double total = header.getLines().stream()
                .mapToDouble(l -> l.getUnitPrice() * l.getQuantity())
                .sum();
        header.setTotalAmount(total);
    }

    public Optional<OrderHeader> updateOrder(Long id, OrderHeader order) {
        if (!ordersById.containsKey(id)) {
            return Optional.empty();
        }
        if (order.getPaymentStatus() == null) {
            order.setPaymentStatus(PaymentStatus.UNPAID);
        }
        order.setId(id);
        order.setStatusUpdatedAt(LocalDateTime.now());
        ordersById.put(id, order);
        orders.put(order.getBusinessId(), order);
        return Optional.of(order);
    }

    public OrderHeader updateOrderStatus(Long id, OrderStatus nextStatus) {
        OrderHeader order = getOrder(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "error.order.not_found"));
        return applyOrderStatusTransition(order, nextStatus, true);
    }

    public boolean deleteOrder(Long id) {
        OrderHeader removed = ordersById.remove(id);
        if (removed != null) {
            orders.remove(removed.getBusinessId());
            return true;
        }
        return false;
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

    public synchronized Trip releaseTripCapacity(Long id, int quantity) {
        Trip trip = getTrip(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "error.trip.not_found"));
        if (quantity <= 0) {
            return trip;
        }
        int releaseQuantity = Math.min(quantity, trip.getReservedCapacity());
        if (releaseQuantity == 0) {
            return trip;
        }
        trip.setReservedCapacity(trip.getReservedCapacity() - releaseQuantity);
        trip.setStatusUpdatedAt(LocalDateTime.now());
        trips.put(id, trip);
        return trip;
    }

    public synchronized List<OrderHeader> bulkUpdateOrderStatus(Long tripId, OrderStatus targetStatus) {
        getTrip(tripId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "error.trip.not_found"));
        List<OrderHeader> eligible = listOrders(tripId).stream()
                .filter(order -> isValidStatusTransition(order.getStatus(), targetStatus))
                .collect(Collectors.toList());
        if (eligible.isEmpty()) {
            return new ArrayList<>();
        }
        if (targetStatus == OrderStatus.CONFIRMED) {
            int totalQuantity = eligible.stream()
                    .mapToInt(this::calculateTotalQuantity)
                    .sum();
            if (totalQuantity > 0) {
                reserveTripCapacity(tripId, totalQuantity);
            }
        }
        List<OrderHeader> updated = new ArrayList<>();
        boolean applyCapacity = targetStatus != OrderStatus.CONFIRMED;
        for (OrderHeader order : eligible) {
            updated.add(applyOrderStatusTransition(order, targetStatus, applyCapacity));
        }
        return updated;
    }

    public List<ProcurementItemResponse> getProcurementList(Long tripId) {
        getTrip(tripId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "error.trip.not_found"));
        Map<String, ProcurementItemResponse> aggregated = new ConcurrentHashMap<>();
        listOrders(tripId).forEach(order -> order.getLines().forEach(line -> {
            String key = line.getSkuId() + "||" + line.getItemName();
            aggregated.compute(key, (_key, existing) -> {
                if (existing == null) {
                    return new ProcurementItemResponse(
                            line.getSkuId(),
                            line.getItemName(),
                            line.getQuantity(),
                            List.of(order.getBusinessId()));
                }
                existing.addQuantity(line.getQuantity());
                existing.addBusinessId(order.getBusinessId());
                return existing;
            });
        }));
        return new ArrayList<>(aggregated.values());
    }

    public double calculateGmv() {
        return orders.values().stream()
                .mapToDouble(OrderHeader::getTotalAmount)
                .sum();
    }

    public Map<OrderStatus, Integer> orderStatusCounts() {
        Map<OrderStatus, Integer> counts = new EnumMap<>(OrderStatus.class);
        orders.values().forEach(order -> counts.merge(order.getStatus(), 1, Integer::sum));
        return counts;
    }

    private boolean isValidStatusTransition(OrderStatus current, OrderStatus next) {
        return switch (current) {
            case NEW -> next == OrderStatus.CONFIRMED || next == OrderStatus.CANCELLED;
            case CONFIRMED -> next == OrderStatus.PURCHASED || next == OrderStatus.CANCELLED;
            case PURCHASED -> next == OrderStatus.DELIVERED;
            case DELIVERED -> next == OrderStatus.SETTLED;
            case SETTLED, CANCELLED -> false;
        };
    }

    private OrderHeader applyOrderStatusTransition(OrderHeader order, OrderStatus nextStatus, boolean applyCapacity) {
        if (!isValidStatusTransition(order.getStatus(), nextStatus)) {
            throw new ApiException(ErrorCode.INVALID_STATUS, "error.order.invalid_status");
        }
        if (applyCapacity && shouldReserveCapacity(order.getStatus(), nextStatus)) {
            reserveTripCapacity(order.getTripId(), calculateTotalQuantity(order));
        }
        if (applyCapacity && shouldReleaseCapacity(order.getStatus(), nextStatus)) {
            releaseTripCapacity(order.getTripId(), calculateTotalQuantity(order));
        }
        String previousStatus = order.getStatus().name();
        order.setStatus(nextStatus);
        order.setStatusUpdatedAt(LocalDateTime.now());
        orders.put(order.getBusinessId(), order);
        ordersById.put(order.getId(), order);
        auditLogService.logStatusChange("ORDER", order.getId(), previousStatus, nextStatus.name(), SYSTEM_USER_ID);
        return order;
    }

    private boolean shouldReserveCapacity(OrderStatus current, OrderStatus nextStatus) {
        return !isAtLeastConfirmed(current) && isAtLeastConfirmed(nextStatus);
    }

    private boolean isAtLeastConfirmed(OrderStatus status) {
        return status == OrderStatus.CONFIRMED
                || status == OrderStatus.PURCHASED
                || status == OrderStatus.DELIVERED
                || status == OrderStatus.SETTLED;
    }

    private boolean shouldReleaseCapacity(OrderStatus current, OrderStatus nextStatus) {
        return nextStatus == OrderStatus.CANCELLED && isAtLeastConfirmed(current);
    }

    private int calculateTotalQuantity(OrderHeader header) {
        if (header == null || header.getLines() == null) {
            return 0;
        }
        return header.getLines().stream()
                .mapToInt(OrderLine::getQuantity)
                .sum();
    }
}
