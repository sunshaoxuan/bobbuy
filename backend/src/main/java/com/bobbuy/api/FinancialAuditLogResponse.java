package com.bobbuy.api;

import java.time.LocalDateTime;

public class FinancialAuditLogResponse {
  private final Long id;
  private final Long tripId;
  private final String actionType;
  private final String operatorName;
  private final String originalValue;
  private final String modifiedValue;
  private final String previousHash;
  private final String currentHash;
  private final LocalDateTime createdAt;

  public FinancialAuditLogResponse(Long id,
                                   Long tripId,
                                   String actionType,
                                   String operatorName,
                                   String originalValue,
                                   String modifiedValue,
                                   String previousHash,
                                   String currentHash,
                                   LocalDateTime createdAt) {
    this.id = id;
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
