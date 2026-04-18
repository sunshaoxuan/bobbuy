package com.bobbuy.api;

public class CustomerBalanceLedgerResponse {
  private final String businessId;
  private final Long customerId;
  private final double totalReceivable;
  private final double paidDeposit;
  private final double outstandingBalance;

  public CustomerBalanceLedgerResponse(String businessId,
                                       Long customerId,
                                       double totalReceivable,
                                       double paidDeposit,
                                       double outstandingBalance) {
    this.businessId = businessId;
    this.customerId = customerId;
    this.totalReceivable = totalReceivable;
    this.paidDeposit = paidDeposit;
    this.outstandingBalance = outstandingBalance;
  }

  public String getBusinessId() {
    return businessId;
  }

  public Long getCustomerId() {
    return customerId;
  }

  public double getTotalReceivable() {
    return totalReceivable;
  }

  public double getPaidDeposit() {
    return paidDeposit;
  }

  public double getOutstandingBalance() {
    return outstandingBalance;
  }
}
