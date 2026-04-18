package com.bobbuy.api;

public class ProfitSharingConfigRequest {
  private double purchaserRatioPercent;
  private double promoterRatioPercent;

  public double getPurchaserRatioPercent() {
    return purchaserRatioPercent;
  }

  public void setPurchaserRatioPercent(double purchaserRatioPercent) {
    this.purchaserRatioPercent = purchaserRatioPercent;
  }

  public double getPromoterRatioPercent() {
    return promoterRatioPercent;
  }

  public void setPromoterRatioPercent(double promoterRatioPercent) {
    this.promoterRatioPercent = promoterRatioPercent;
  }
}
