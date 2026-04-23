package com.bobbuy.api;

import java.time.LocalDateTime;

public class CustomerPaymentRecordResponse {
  private final Long id;
  private final Long tripId;
  private final String businessId;
  private final Long customerId;
  private final double amount;
  private final String paymentMethod;
  private final String note;
  private final LocalDateTime createdAt;
  private final String operator;

  public CustomerPaymentRecordResponse(Long id,
                                       Long tripId,
                                       String businessId,
                                       Long customerId,
                                       double amount,
                                       String paymentMethod,
                                       String note,
                                       LocalDateTime createdAt,
                                       String operator) {
    this.id = id;
    this.tripId = tripId;
    this.businessId = businessId;
    this.customerId = customerId;
    this.amount = amount;
    this.paymentMethod = paymentMethod;
    this.note = note;
    this.createdAt = createdAt;
    this.operator = operator;
  }

  public Long getId() {
    return id;
  }

  public Long getTripId() {
    return tripId;
  }

  public String getBusinessId() {
    return businessId;
  }

  public Long getCustomerId() {
    return customerId;
  }

  public double getAmount() {
    return amount;
  }

  public String getPaymentMethod() {
    return paymentMethod;
  }

  public String getNote() {
    return note;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public String getOperator() {
    return operator;
  }
}
