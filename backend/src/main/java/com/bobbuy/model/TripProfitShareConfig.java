package com.bobbuy.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "bb_trip_profit_share_config")
public class TripProfitShareConfig {
  @Id
  private Long tripId;

  private double purchaserRatioPercent;
  private double promoterRatioPercent;
  private LocalDateTime updatedAt;

  public TripProfitShareConfig() {
  }

  public TripProfitShareConfig(Long tripId, double purchaserRatioPercent, double promoterRatioPercent, LocalDateTime updatedAt) {
    this.tripId = tripId;
    this.purchaserRatioPercent = purchaserRatioPercent;
    this.promoterRatioPercent = promoterRatioPercent;
    this.updatedAt = updatedAt;
  }

  public Long getTripId() {
    return tripId;
  }

  public void setTripId(Long tripId) {
    this.tripId = tripId;
  }

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

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
