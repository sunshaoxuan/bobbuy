package com.bobbuy.model;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Locale;

public enum OrderStatus {
  NEW,
  CONFIRMED,
  PURCHASED,
  DELIVERED,
  SETTLED,
  CANCELLED;

  @JsonCreator
  public static OrderStatus fromJson(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return OrderStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
  }
}
