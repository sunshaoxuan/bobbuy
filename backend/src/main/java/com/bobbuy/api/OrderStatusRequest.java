package com.bobbuy.api;

import com.bobbuy.model.OrderStatus;
import jakarta.validation.constraints.NotNull;

public class OrderStatusRequest {
  @NotNull(message = "{validation.order_status.required}")
  private OrderStatus status;

  public OrderStatus getStatus() {
    return status;
  }

  public void setStatus(OrderStatus status) {
    this.status = status;
  }
}
