package com.bobbuy.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallet_transactions")
public class WalletTransaction {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String partnerId;

  @Column(nullable = false)
  private Double amount;

  @Column(nullable = false)
  private String type; // e.g., "TRIP_PAYOUT", "WITHDRAWAL"

  @Column(nullable = false)
  private Long tripId;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  public WalletTransaction() {}

  public WalletTransaction(String partnerId, Double amount, String type, Long tripId, LocalDateTime createdAt) {
    this.partnerId = partnerId;
    this.amount = amount;
    this.type = type;
    this.tripId = tripId;
    this.createdAt = createdAt;
  }

  // Getters and Setters
  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getPartnerId() { return partnerId; }
  public void setPartnerId(String partnerId) { this.partnerId = partnerId; }
  public Double getAmount() { return amount; }
  public void setAmount(Double amount) { this.amount = amount; }
  public String getType() { return type; }
  public void setType(String type) { this.type = type; }
  public Long getTripId() { return tripId; }
  public void setTripId(Long tripId) { this.tripId = tripId; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
