package com.bobbuy.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Entity
@Table(name = "bb_procurement_receipt")
public class ProcurementReceipt {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long tripId;
  private String fileName;
  private String originalObjectKey;
  private String originalImageUrl;
  private String thumbnailObjectKey;
  private String thumbnailUrl;
  private String processingStatus;
  private LocalDateTime uploadedAt;
  private LocalDateTime updatedAt;
  @Column(length = 200000)
  private String sourceImageBase64;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private Map<String, Object> reconciliationResult = new LinkedHashMap<>();

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private Map<String, Object> rawRecognitionResult = new LinkedHashMap<>();

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private Map<String, Object> manualReconciliationResult = new LinkedHashMap<>();

  public ProcurementReceipt() {
  }

  public ProcurementReceipt(Long tripId,
                            String fileName,
                            String originalObjectKey,
                            String originalImageUrl,
                            String thumbnailObjectKey,
                            String thumbnailUrl,
                            String processingStatus,
                            LocalDateTime uploadedAt,
                            LocalDateTime updatedAt,
                            String sourceImageBase64,
                            Map<String, Object> reconciliationResult) {
    this.tripId = tripId;
    this.fileName = fileName;
    this.originalObjectKey = originalObjectKey;
    this.originalImageUrl = originalImageUrl;
    this.thumbnailObjectKey = thumbnailObjectKey;
    this.thumbnailUrl = thumbnailUrl;
    this.processingStatus = processingStatus;
    this.uploadedAt = uploadedAt;
    this.updatedAt = updatedAt;
    this.sourceImageBase64 = sourceImageBase64;
    this.reconciliationResult = reconciliationResult == null ? new LinkedHashMap<>() : new LinkedHashMap<>(reconciliationResult);
    this.rawRecognitionResult = new LinkedHashMap<>(this.reconciliationResult);
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

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public String getOriginalObjectKey() {
    return originalObjectKey;
  }

  public void setOriginalObjectKey(String originalObjectKey) {
    this.originalObjectKey = originalObjectKey;
  }

  public String getOriginalImageUrl() {
    return originalImageUrl;
  }

  public void setOriginalImageUrl(String originalImageUrl) {
    this.originalImageUrl = originalImageUrl;
  }

  public String getThumbnailObjectKey() {
    return thumbnailObjectKey;
  }

  public void setThumbnailObjectKey(String thumbnailObjectKey) {
    this.thumbnailObjectKey = thumbnailObjectKey;
  }

  public String getThumbnailUrl() {
    return thumbnailUrl;
  }

  public void setThumbnailUrl(String thumbnailUrl) {
    this.thumbnailUrl = thumbnailUrl;
  }

  public String getProcessingStatus() {
    return processingStatus;
  }

  public void setProcessingStatus(String processingStatus) {
    this.processingStatus = processingStatus;
  }

  public LocalDateTime getUploadedAt() {
    return uploadedAt;
  }

  public void setUploadedAt(LocalDateTime uploadedAt) {
    this.uploadedAt = uploadedAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  public String getSourceImageBase64() {
    return sourceImageBase64;
  }

  public void setSourceImageBase64(String sourceImageBase64) {
    this.sourceImageBase64 = sourceImageBase64;
  }

  public Map<String, Object> getReconciliationResult() {
    return reconciliationResult;
  }

  public void setReconciliationResult(Map<String, Object> reconciliationResult) {
    this.reconciliationResult = reconciliationResult == null ? new LinkedHashMap<>() : reconciliationResult;
  }

  public Map<String, Object> getRawRecognitionResult() {
    return rawRecognitionResult;
  }

  public void setRawRecognitionResult(Map<String, Object> rawRecognitionResult) {
    this.rawRecognitionResult = rawRecognitionResult == null ? new LinkedHashMap<>() : rawRecognitionResult;
  }

  public Map<String, Object> getManualReconciliationResult() {
    return manualReconciliationResult;
  }

  public void setManualReconciliationResult(Map<String, Object> manualReconciliationResult) {
    this.manualReconciliationResult = manualReconciliationResult == null ? new LinkedHashMap<>() : manualReconciliationResult;
  }
}
