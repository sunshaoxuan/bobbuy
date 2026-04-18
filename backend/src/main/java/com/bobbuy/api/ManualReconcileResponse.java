package com.bobbuy.api;

public class ManualReconcileResponse {
  private final String skuId;
  private final String fromBusinessId;
  private final String toBusinessId;
  private final int transferredQuantity;

  public ManualReconcileResponse(String skuId, String fromBusinessId, String toBusinessId, int transferredQuantity) {
    this.skuId = skuId;
    this.fromBusinessId = fromBusinessId;
    this.toBusinessId = toBusinessId;
    this.transferredQuantity = transferredQuantity;
  }

  public String getSkuId() {
    return skuId;
  }

  public String getFromBusinessId() {
    return fromBusinessId;
  }

  public String getToBusinessId() {
    return toBusinessId;
  }

  public int getTransferredQuantity() {
    return transferredQuantity;
  }
}
