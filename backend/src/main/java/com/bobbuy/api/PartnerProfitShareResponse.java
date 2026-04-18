package com.bobbuy.api;

public class PartnerProfitShareResponse {
  private final String partnerRole;
  private final double ratioPercent;
  private final double amount;

  public PartnerProfitShareResponse(String partnerRole, double ratioPercent, double amount) {
    this.partnerRole = partnerRole;
    this.ratioPercent = ratioPercent;
    this.amount = amount;
  }

  public String getPartnerRole() {
    return partnerRole;
  }

  public double getRatioPercent() {
    return ratioPercent;
  }

  public double getAmount() {
    return amount;
  }
}
