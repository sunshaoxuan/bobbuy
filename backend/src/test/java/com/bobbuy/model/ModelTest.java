package com.bobbuy.model;

import com.bobbuy.model.User;
import com.bobbuy.model.Trip;
import com.bobbuy.model.Order;
import com.bobbuy.model.AuditLog;
import com.bobbuy.model.Role;
import com.bobbuy.model.TripStatus;
import com.bobbuy.model.OrderStatus;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;

class ModelTest {
    @Test
    void userTest() {
        User user = new User(1L, "Name", Role.CUSTOMER, 4.5);
        assertThat(user.getId()).isEqualTo(1L);
        assertThat(user.getName()).isEqualTo("Name");
        assertThat(user.getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(user.getRating()).isEqualTo(4.5);

        user.setName("New");
        user.setRole(Role.AGENT);
        user.setRating(3.0);
        assertThat(user.getName()).isEqualTo("New");
        assertThat(user.getRole()).isEqualTo(Role.AGENT);
        assertThat(user.getRating()).isEqualTo(3.0);
    }

    @Test
    void tripTest() {
        LocalDateTime now = LocalDateTime.now();
        Trip trip = new Trip(1L, 100L, "O", "D", LocalDate.MAX, 10, 2, TripStatus.PUBLISHED, now);
        assertThat(trip.getOrigin()).isEqualTo("O");
        assertThat(trip.getCapacity()).isEqualTo(10);
        assertThat(trip.getReservedCapacity()).isEqualTo(2);
        assertThat(trip.getRemainingCapacity()).isEqualTo(8);

        trip.setDestination("D2");
        trip.setStatus(TripStatus.COMPLETED);
        assertThat(trip.getDestination()).isEqualTo("D2");
        assertThat(trip.getStatus()).isEqualTo(TripStatus.COMPLETED);
        assertThat(trip.getStatusUpdatedAt()).isEqualTo(now);
    }

    @Test
    void orderTest() {
        LocalDateTime now = LocalDateTime.now();
        List<OrderItem> items = new ArrayList<>();
        items.add(new OrderItem(10L, "Item", 2, 10.0, false));
        Order order = new Order(1L, "KEY-1", 100L, 200L, items, 1.0, 0.5, "USD", OrderStatus.NEW, now);
        assertThat(order.getBusinessKey()).isEqualTo("KEY-1");
        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getItems().get(0).getItemName()).isEqualTo("Item");
        assertThat(order.getServiceFee()).isEqualTo(1.0);
        assertThat(order.getEstimatedTax()).isEqualTo(0.5);
        assertThat(order.getCurrency()).isEqualTo("USD");

        order.setStatus(OrderStatus.CONFIRMED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void auditLogTest() {
        LocalDateTime now = LocalDateTime.now();
        AuditLog log = new AuditLog(1L, "T", 100L, "A", "B", "A", 0L, now);
        assertThat(log.getEntityType()).isEqualTo("T");
        assertThat(log.getAfterValue()).isEqualTo("A");

        log.setAction("UPDATED");
        assertThat(log.getAction()).isEqualTo("UPDATED");
    }
}
