package com.bobbuy.api;

import com.bobbuy.model.OrderStatus;
import jakarta.validation.constraints.NotNull;

public class TripOrderBulkStatusRequest {
  @NotNull(message = "{validation.order_status.required}")
  private OrderStatus targetStatus;

  public OrderStatus getTargetStatus() {
    return targetStatus;
  }

  public void setTargetStatus(OrderStatus targetStatus) {
    this.targetStatus = targetStatus;
  }
}
