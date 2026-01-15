package com.bobbuy.api;

import com.bobbuy.model.TripStatus;
import jakarta.validation.constraints.NotNull;

public class TripStatusRequest {
  @NotNull(message = "{validation.trip_status.required}")
  private TripStatus status;

  public TripStatus getStatus() {
    return status;
  }

  public void setStatus(TripStatus status) {
    this.status = status;
  }
}
