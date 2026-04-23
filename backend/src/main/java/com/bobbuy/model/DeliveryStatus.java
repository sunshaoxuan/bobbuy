package com.bobbuy.model;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Locale;

public enum DeliveryStatus {
  PENDING_DELIVERY,
  READY_FOR_DELIVERY,
  DELIVERING,
  DELIVERED;

  @JsonCreator
  public static DeliveryStatus fromJson(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return DeliveryStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
  }
}
