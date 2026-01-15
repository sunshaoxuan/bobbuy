package com.bobbuy.api;

import jakarta.validation.constraints.Min;

public class TripReserveRequest {
  @Min(value = 1, message = "预留数量必须大于 0")
  private int quantity;

  public int getQuantity() {
    return quantity;
  }

  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }
}
