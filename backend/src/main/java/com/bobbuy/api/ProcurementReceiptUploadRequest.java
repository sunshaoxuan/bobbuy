package com.bobbuy.api;

import java.util.List;

public class ProcurementReceiptUploadRequest {
  private List<ReceiptImagePayload> receipts;

  public List<ReceiptImagePayload> getReceipts() {
    return receipts;
  }

  public void setReceipts(List<ReceiptImagePayload> receipts) {
    this.receipts = receipts;
  }

  public static class ReceiptImagePayload {
    private String imageBase64;
    private String fileName;

    public String getImageBase64() {
      return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
      this.imageBase64 = imageBase64;
    }

    public String getFileName() {
      return fileName;
    }

    public void setFileName(String fileName) {
      this.fileName = fileName;
    }
  }
}
