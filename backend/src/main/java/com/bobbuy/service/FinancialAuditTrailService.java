package com.bobbuy.service;

import com.bobbuy.api.FinancialAuditLogResponse;
import com.bobbuy.model.FinancialAuditLog;
import com.bobbuy.repository.FinancialAuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

@Service
public class FinancialAuditTrailService {
  private static final String GENESIS_HASH = "GENESIS";
  private static final String OPERATOR_SYSTEM = "SYSTEM";

  private final FinancialAuditLogRepository financialAuditLogRepository;

  public FinancialAuditTrailService(FinancialAuditLogRepository financialAuditLogRepository) {
    this.financialAuditLogRepository = financialAuditLogRepository;
  }

  @Transactional
  public void logManualReconcile(Long tripId, String originalValue, String modifiedValue) {
    append(tripId, "MANUAL_RECONCILE", originalValue, modifiedValue);
  }

  @Transactional
  public void logExpenseCreate(Long tripId, String originalValue, String modifiedValue) {
    append(tripId, "EXPENSE_CREATE", originalValue, modifiedValue);
  }

  @Transactional
  public void logProfitShareRatioUpdate(Long tripId, String originalValue, String modifiedValue) {
    append(tripId, "PROFIT_SHARE_RATIO_UPDATE", originalValue, modifiedValue);
  }

  @Transactional
  public void logSettlementReminderTriggered(Long tripId, String originalValue, String modifiedValue) {
    append(tripId, "SETTLEMENT_REMINDER_TRIGGERED", originalValue, modifiedValue);
  }

  @Transactional
  public void logSettlementFreeze(Long tripId, String operatorName, String originalValue, String modifiedValue) {
    append(tripId, "SETTLEMENT_FREEZE", operatorName, originalValue, modifiedValue);
  }

  @Transactional
  public void logCustomerReceiptConfirmation(Long tripId, String operatorName, String originalValue, String modifiedValue) {
    append(tripId, "CUSTOMER_RECEIPT_CONFIRMED", operatorName, originalValue, modifiedValue);
  }

  @Transactional
  public void logCustomerBillingConfirmation(Long tripId, String operatorName, String originalValue, String modifiedValue) {
    append(tripId, "CUSTOMER_BILLING_CONFIRMED", operatorName, originalValue, modifiedValue);
  }

  @Transactional
  public void logProcurementReceiptUpload(Long tripId, String operatorName, String originalValue, String modifiedValue) {
    append(tripId, "PROCUREMENT_RECEIPT_UPLOAD", operatorName, originalValue, modifiedValue);
  }

  @Transactional
  public void logProcurementReceiptReconciliation(Long tripId, String operatorName, String originalValue, String modifiedValue) {
    append(tripId, "PROCUREMENT_RECEIPT_RECONCILED", operatorName, originalValue, modifiedValue);
  }

  @Transactional(readOnly = true)
  public List<FinancialAuditLogResponse> listByTripId(Long tripId) {
    List<FinancialAuditLog> logs = financialAuditLogRepository.findByTripIdOrderByCreatedAtDescIdDesc(tripId);
    return logs.stream()
        .map(item -> new FinancialAuditLogResponse(
            item.getId(),
            item.getTripId(),
            item.getActionType(),
            item.getOperatorName(),
            item.getOriginalValue(),
            item.getModifiedValue(),
            item.getPreviousHash(),
            item.getCurrentHash(),
            item.getCreatedAt()))
        .toList();
  }

  @Transactional(readOnly = true)
  public boolean checkChainIntegrity(Long tripId) {
    List<FinancialAuditLog> logs = financialAuditLogRepository.findByTripIdOrderByCreatedAtAscIdAsc(tripId);
    String expectedPreviousHash = GENESIS_HASH;

    for (FinancialAuditLog log : logs) {
      // 1. Check if previousHash matches the expected one
      if (!expectedPreviousHash.equals(log.getPreviousHash())) {
        return false;
      }

      // 2. Re-calculate currentHash and compare
      String payload = String.join("|",
          String.valueOf(log.getTripId()),
          log.getActionType(),
          log.getOperatorName(),
          nullSafe(log.getOriginalValue()),
          nullSafe(log.getModifiedValue()),
          log.getPreviousHash(),
          log.getCreatedAt().toString());
      
      String calculatedHash = sha256Hex(payload);
      if (!calculatedHash.equals(log.getCurrentHash())) {
        return false;
      }

      expectedPreviousHash = log.getCurrentHash();
    }
    return true;
  }

  private void append(Long tripId, String actionType, String originalValue, String modifiedValue) {
    append(tripId, actionType, OPERATOR_SYSTEM, originalValue, modifiedValue);
  }

  private void append(Long tripId, String actionType, String operatorName, String originalValue, String modifiedValue) {
    String previousHash = financialAuditLogRepository.findTopByTripIdOrderByCreatedAtDescIdDesc(tripId)
        .map(FinancialAuditLog::getCurrentHash)
        .orElse(GENESIS_HASH);
    LocalDateTime createdAt = LocalDateTime.now();
    String payload = String.join("|",
        String.valueOf(tripId),
        actionType,
        nullSafe(operatorName),
        nullSafe(originalValue),
        nullSafe(modifiedValue),
        previousHash,
        createdAt.toString());
    String currentHash = sha256Hex(payload);
    financialAuditLogRepository.save(new FinancialAuditLog(
        tripId,
        actionType,
        nullSafe(operatorName),
        nullSafe(originalValue),
        nullSafe(modifiedValue),
        previousHash,
        currentHash,
        createdAt));
  }

  private String nullSafe(String value) {
    return value == null ? "" : value;
  }

  private String sha256Hex(String payload) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 unavailable", ex);
    }
  }
}
