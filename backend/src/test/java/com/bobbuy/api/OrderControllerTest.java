package com.bobbuy.api;

import com.bobbuy.api.response.ApiException;
import com.bobbuy.api.response.ApiResponse;
import com.bobbuy.model.Order;
import com.bobbuy.model.OrderItem;
import com.bobbuy.model.OrderStatus;
import com.bobbuy.service.AuditLogService;
import com.bobbuy.service.BobbuyStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderControllerTest {
  private AuditLogService auditLogService;
  private BobbuyStore store;
  private OrderController controller;

  @BeforeEach
  void setUp() {
    auditLogService = new AuditLogService();
    store = new BobbuyStore(auditLogService);
    store.seed();
    controller = new OrderController(store);
  }

  @Test
  void listsOrders() {
    ApiResponse<java.util.List<Order>> response = controller.list();
    assertThat(response.getData()).hasSize(1);
  }

  @Test
  void getsOrder() {
    ResponseEntity<ApiResponse<Order>> response = controller.get(3000L);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getData().getId()).isEqualTo(3000L);
  }

  @Test
  void createsOrder() {
    List<OrderItem> items = new ArrayList<>();
    items.add(new OrderItem(null, "Camera", 1, 120.0, false));
    Order order = new Order(null, "C1002-E1", 1002L, 2000L, items, 10.0, 5.0, "CNY",
        OrderStatus.NEW, LocalDateTime.now());
    ResponseEntity<ApiResponse<Order>> response = controller.create(order);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getData().getId()).isNotNull();
    assertThat(store.listOrders()).hasSize(2);
  }

  @Test
  void updatesOrder() {
    List<OrderItem> items = new ArrayList<>();
    items.add(new OrderItem(null, "Updated Item", 2, 32.5, false));
    Order order = new Order(3000L, "3000-KEY", 1001L, 2000L, items, 6.0, 2.3, "CNY",
        OrderStatus.CONFIRMED, LocalDateTime.now());
    ResponseEntity<ApiResponse<Order>> response = controller.update(3000L, order);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getData().getItems().get(0).getItemName()).isEqualTo("Updated Item");
  }

  @Test
  void updatesStatusAndWritesAuditLog() {
    int before = auditLogService.listLogs().size();
    OrderStatusRequest request = new OrderStatusRequest();
    request.setStatus(OrderStatus.PURCHASED);
    ResponseEntity<ApiResponse<Order>> response = controller.updateStatus(3000L, request);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getData().getStatus()).isEqualTo(OrderStatus.PURCHASED);
    assertThat(auditLogService.listLogs().size()).isEqualTo(before + 1);
  }

  @Test
  void deletesOrder() {
    ResponseEntity<ApiResponse<Void>> response = controller.delete(3000L);
    assertThat(response.getBody()).isNotNull();
    assertThat(store.listOrders()).isEmpty();
  }

  @Test
  void throwsWhenOrderMissing() {
    assertThatThrownBy(() -> controller.get(9999L))
        .isInstanceOf(ApiException.class);
    assertThatThrownBy(() -> controller.update(9999L, new Order()))
        .isInstanceOf(ApiException.class);
    assertThatThrownBy(() -> controller.delete(9999L))
        .isInstanceOf(ApiException.class);
  }
}
