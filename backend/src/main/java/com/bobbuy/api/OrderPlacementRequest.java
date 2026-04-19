package com.bobbuy.api;

public class OrderPlacementRequest {
  private String skuId;
  private Integer quantity;
  private String businessId; // For Zen mode, we typically map to a default or selected customer account

  public OrderPlacementRequest() {}

  public OrderPlacementRequest(String skuId, Integer quantity, String businessId) {
    this.skuId = skuId;
    this.quantity = quantity;
    this.businessId = businessId;
  }

  public String getSkuId() { return skuId; }
  public void setSkuId(String skuId) { this.skuId = skuId; }

  public Integer getQuantity() { return quantity; }
  public void setQuantity(Integer quantity) { this.quantity = quantity; }

  public String getBusinessId() { return businessId; }
  public void setBusinessId(String businessId) { this.businessId = businessId; }
}
