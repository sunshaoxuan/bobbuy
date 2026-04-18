package com.bobbuy.api;

import java.util.List;

public class ProfitSharingConfigResponse {
  private final Long tripId;
  private final double purchaserRatioPercent;
  private final double promoterRatioPercent;
  private final List<PartnerProfitShareResponse> shares;

  public ProfitSharingConfigResponse(Long tripId,
                                     double purchaserRatioPercent,
                                     double promoterRatioPercent,
                                     List<PartnerProfitShareResponse> shares) {
    this.tripId = tripId;
    this.purchaserRatioPercent = purchaserRatioPercent;
    this.promoterRatioPercent = promoterRatioPercent;
    this.shares = shares;
  }

  public Long getTripId() {
    return tripId;
  }

  public double getPurchaserRatioPercent() {
    return purchaserRatioPercent;
  }

  public double getPromoterRatioPercent() {
    return promoterRatioPercent;
  }

  public List<PartnerProfitShareResponse> getShares() {
    return shares;
  }
}
