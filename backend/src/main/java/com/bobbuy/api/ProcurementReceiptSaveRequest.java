package com.bobbuy.api;

import java.util.Map;

public class ProcurementReceiptSaveRequest {
  private String processingStatus;
  private Map<String, Object> reconciliationResult;

  public String getProcessingStatus() {
    return processingStatus;
  }

  public void setProcessingStatus(String processingStatus) {
    this.processingStatus = processingStatus;
  }

  public Map<String, Object> getReconciliationResult() {
    return reconciliationResult;
  }

  public void setReconciliationResult(Map<String, Object> reconciliationResult) {
    this.reconciliationResult = reconciliationResult;
  }
}
