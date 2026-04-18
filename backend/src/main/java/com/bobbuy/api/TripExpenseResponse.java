package com.bobbuy.api;

import java.time.LocalDateTime;

public class TripExpenseResponse {
  private final Long id;
  private final Long tripId;
  private final double cost;
  private final String category;
  private final LocalDateTime createdAt;

  public TripExpenseResponse(Long id, Long tripId, double cost, String category, LocalDateTime createdAt) {
    this.id = id;
    this.tripId = tripId;
    this.cost = cost;
    this.category = category;
    this.createdAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public Long getTripId() {
    return tripId;
  }

  public double getCost() {
    return cost;
  }

  public String getCategory() {
    return category;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }
}
