package com.bobbuy.api;

public class ReceiptPreviewResponse {
  private final Long expenseId;
  private final String previewUrl;

  public ReceiptPreviewResponse(Long expenseId, String previewUrl) {
    this.expenseId = expenseId;
    this.previewUrl = previewUrl;
  }

  public Long getExpenseId() {
    return expenseId;
  }

  public String getPreviewUrl() {
    return previewUrl;
  }
}
