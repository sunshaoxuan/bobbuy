package com.bobbuy.api;

public class TripExpenseRequest {
  private double cost;
  private String category;
  private String receiptImageBase64;

  public double getCost() {
    return cost;
  }

  public void setCost(double cost) {
    this.cost = cost;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public String getReceiptImageBase64() {
    return receiptImageBase64;
  }

  public void setReceiptImageBase64(String receiptImageBase64) {
    this.receiptImageBase64 = receiptImageBase64;
  }
}
