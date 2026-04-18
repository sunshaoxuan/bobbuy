package com.bobbuy.api;

public class ProcurementDeficitItemResponse {
  private final String skuId;
  private final String itemName;
  private final int deficitQuantity;
  private final double completionPercent;
  private final String priority;

  public ProcurementDeficitItemResponse(String skuId, String itemName, int deficitQuantity, double completionPercent, String priority) {
    this.skuId = skuId;
    this.itemName = itemName;
    this.deficitQuantity = deficitQuantity;
    this.completionPercent = completionPercent;
    this.priority = priority;
  }

  public String getSkuId() {
    return skuId;
  }

  public String getItemName() {
    return itemName;
  }

  public int getDeficitQuantity() {
    return deficitQuantity;
  }

  public double getCompletionPercent() {
    return completionPercent;
  }

  public String getPriority() {
    return priority;
  }
}
