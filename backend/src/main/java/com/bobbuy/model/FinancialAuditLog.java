package com.bobbuy.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "bb_financial_audit_log")
public class FinancialAuditLog {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, updatable = false)
  private Long tripId;

  @Column(nullable = false, updatable = false, length = 64)
  private String actionType;

  @Column(nullable = false, updatable = false, length = 64)
  private String operatorName;

  @Column(nullable = false, updatable = false, columnDefinition = "text")
  private String originalValue;

  @Column(nullable = false, updatable = false, columnDefinition = "text")
  private String modifiedValue;

  @Column(nullable = false, updatable = false, length = 128)
  private String previousHash;

  @Column(nullable = false, updatable = false, length = 128)
  private String currentHash;

  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  public FinancialAuditLog() {
  }

  public FinancialAuditLog(Long tripId,
                           String actionType,
                           String operatorName,
                           String originalValue,
                           String modifiedValue,
                           String previousHash,
                           String currentHash,
                           LocalDateTime createdAt) {
    this.tripId = tripId;
    this.actionType = actionType;
    this.operatorName = operatorName;
    this.originalValue = originalValue;
    this.modifiedValue = modifiedValue;
    this.previousHash = previousHash;
    this.currentHash = currentHash;
    this.createdAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public Long getTripId() {
    return tripId;
  }

  public String getActionType() {
    return actionType;
  }

  public String getOperatorName() {
    return operatorName;
  }

  public String getOriginalValue() {
    return originalValue;
  }

  public String getModifiedValue() {
    return modifiedValue;
  }

  public String getPreviousHash() {
    return previousHash;
  }

  public String getCurrentHash() {
    return currentHash;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }
}
