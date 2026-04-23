package com.bobbuy.api;

import java.time.LocalDateTime;
import java.util.Map;

public class ProcurementReceiptResponse {
  private final Long id;
  private final Long tripId;
  private final String fileName;
  private final String originalImageUrl;
  private final String thumbnailUrl;
  private final String processingStatus;
  private final LocalDateTime uploadedAt;
  private final LocalDateTime updatedAt;
  private final Map<String, Object> reconciliationResult;
  private final Map<String, Object> rawRecognitionResult;
  private final Map<String, Object> manualReconciliationResult;

  public ProcurementReceiptResponse(Long id,
                                    Long tripId,
                                    String fileName,
                                    String originalImageUrl,
                                    String thumbnailUrl,
                                    String processingStatus,
                                    LocalDateTime uploadedAt,
                                    LocalDateTime updatedAt,
                                    Map<String, Object> reconciliationResult,
                                    Map<String, Object> rawRecognitionResult,
                                    Map<String, Object> manualReconciliationResult) {
    this.id = id;
    this.tripId = tripId;
    this.fileName = fileName;
    this.originalImageUrl = originalImageUrl;
    this.thumbnailUrl = thumbnailUrl;
    this.processingStatus = processingStatus;
    this.uploadedAt = uploadedAt;
    this.updatedAt = updatedAt;
    this.reconciliationResult = reconciliationResult;
    this.rawRecognitionResult = rawRecognitionResult;
    this.manualReconciliationResult = manualReconciliationResult;
  }

  public Long getId() {
    return id;
  }

  public Long getTripId() {
    return tripId;
  }

  public String getFileName() {
    return fileName;
  }

  public String getOriginalImageUrl() {
    return originalImageUrl;
  }

  public String getThumbnailUrl() {
    return thumbnailUrl;
  }

  public String getProcessingStatus() {
    return processingStatus;
  }

  public LocalDateTime getUploadedAt() {
    return uploadedAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public Map<String, Object> getReconciliationResult() {
    return reconciliationResult;
  }

  public Map<String, Object> getRawRecognitionResult() {
    return rawRecognitionResult;
  }

  public Map<String, Object> getManualReconciliationResult() {
    return manualReconciliationResult;
  }
}
