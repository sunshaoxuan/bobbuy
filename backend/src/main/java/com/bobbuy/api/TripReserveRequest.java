package com.bobbuy.api;

import jakarta.validation.constraints.Min;

public class TripReserveRequest {
  @Min(value = 1, message = "{validation.trip.reserve_quantity.min}")
  private int quantity;

  public int getQuantity() {
    return quantity;
  }

  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }
}
