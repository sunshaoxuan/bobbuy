package com.bobbuy.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "bb_trip_expense")
public class TripExpense {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long tripId;
  private double cost;
  private String category;
  private LocalDateTime createdAt;

  public TripExpense() {
  }

  public TripExpense(Long tripId, double cost, String category, LocalDateTime createdAt) {
    this.tripId = tripId;
    this.cost = Math.max(cost, 0D);
    this.category = category;
    this.createdAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getTripId() {
    return tripId;
  }

  public void setTripId(Long tripId) {
    this.tripId = tripId;
  }

  public double getCost() {
    return cost;
  }

  public void setCost(double cost) {
    this.cost = Math.max(cost, 0D);
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
