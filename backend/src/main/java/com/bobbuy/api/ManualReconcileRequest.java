package com.bobbuy.api;

public class ManualReconcileRequest {
  private String skuId;
  private String fromBusinessId;
  private String toBusinessId;
  private int quantity;

  public String getSkuId() {
    return skuId;
  }

  public void setSkuId(String skuId) {
    this.skuId = skuId;
  }

  public String getFromBusinessId() {
    return fromBusinessId;
  }

  public void setFromBusinessId(String fromBusinessId) {
    this.fromBusinessId = fromBusinessId;
  }

  public String getToBusinessId() {
    return toBusinessId;
  }

  public void setToBusinessId(String toBusinessId) {
    this.toBusinessId = toBusinessId;
  }

  public int getQuantity() {
    return quantity;
  }

  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }
}
