package com.bobbuy.api;

public class PickingChecklistUpdateRequest {
  private String skuId;
  private boolean checked;

  public String getSkuId() {
    return skuId;
  }

  public void setSkuId(String skuId) {
    this.skuId = skuId;
  }

  public boolean isChecked() {
    return checked;
  }

  public void setChecked(boolean checked) {
    this.checked = checked;
  }
}
