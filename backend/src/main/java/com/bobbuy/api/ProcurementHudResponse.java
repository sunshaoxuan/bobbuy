package com.bobbuy.api;

import java.util.Map;

public class ProcurementHudResponse {
  private final Long tripId;
  private final double totalEstimatedProfit;
  private final double currentPurchasedAmount;
  private final double currentFxRate;
  private final double referenceFxRate;
  private final double totalTripExpenses;
  private final double currentWeight;
  private final double currentVolume;
  private final Map<String, Double> categoryCompletionPercent;

  public ProcurementHudResponse(Long tripId,
                                double totalEstimatedProfit,
                                double currentPurchasedAmount,
                                double currentFxRate,
                                double referenceFxRate,
                                double totalTripExpenses,
                                double currentWeight,
                                double currentVolume,
                                Map<String, Double> categoryCompletionPercent) {
    this.tripId = tripId;
    this.totalEstimatedProfit = totalEstimatedProfit;
    this.currentPurchasedAmount = currentPurchasedAmount;
    this.currentFxRate = currentFxRate;
    this.referenceFxRate = referenceFxRate;
    this.totalTripExpenses = totalTripExpenses;
    this.currentWeight = currentWeight;
    this.currentVolume = currentVolume;
    this.categoryCompletionPercent = categoryCompletionPercent;
  }

  public Long getTripId() {
    return tripId;
  }

  public double getTotalEstimatedProfit() {
    return totalEstimatedProfit;
  }

  public double getCurrentPurchasedAmount() {
    return currentPurchasedAmount;
  }

  public double getCurrentFxRate() {
    return currentFxRate;
  }

  public double getReferenceFxRate() {
    return referenceFxRate;
  }

  public double getTotalTripExpenses() {
    return totalTripExpenses;
  }

  public double getCurrentWeight() {
    return currentWeight;
  }

  public double getCurrentVolume() {
    return currentVolume;
  }

  public Map<String, Double> getCategoryCompletionPercent() {
    return categoryCompletionPercent;
  }
}
