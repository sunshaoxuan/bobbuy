package com.bobbuy.model;

import java.util.Locale;

public enum OfflinePaymentMethod {
  CASH,
  BANK_TRANSFER,
  OTHER;

  public static OfflinePaymentMethod parse(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return OfflinePaymentMethod.valueOf(value.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }
}
