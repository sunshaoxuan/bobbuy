package com.bobbuy.api;

public class CustomerBalanceSummaryResponse {
  private final Long customerId;
  private final double currentBalance;

  public CustomerBalanceSummaryResponse(Long customerId, double currentBalance) {
    this.customerId = customerId;
    this.currentBalance = currentBalance;
  }

  public Long getCustomerId() {
    return customerId;
  }

  public double getCurrentBalance() {
    return currentBalance;
  }
}
