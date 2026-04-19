package com.bobbuy.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "partner_wallets")
public class PartnerWallet {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String partnerId; // e.g., "PURCHASER", "PROMOTER"

  @Column(nullable = false)
  private Double balance;

  @Column(nullable = false)
  private String currency; // e.g., "CNY"

  @Column(nullable = false)
  private LocalDateTime updatedAt;

  public PartnerWallet() {}

  public PartnerWallet(String partnerId, Double balance, String currency, LocalDateTime updatedAt) {
    this.partnerId = partnerId;
    this.balance = balance;
    this.currency = currency;
    this.updatedAt = updatedAt;
  }

  // Getters and Setters
  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getPartnerId() { return partnerId; }
  public void setPartnerId(String partnerId) { this.partnerId = partnerId; }
  public Double getBalance() { return balance; }
  public void setBalance(Double balance) { this.balance = balance; }
  public String getCurrency() { return currency; }
  public void setCurrency(String currency) { this.currency = currency; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
