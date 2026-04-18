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

  @Transactional(readOnly = true)
  public List<FinancialAuditLogResponse> listByTripId(Long tripId) {
    return financialAuditLogRepository.findByTripIdOrderByCreatedAtDescIdDesc(tripId).stream()
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

  private void append(Long tripId, String actionType, String originalValue, String modifiedValue) {
    String previousHash = financialAuditLogRepository.findTopByTripIdOrderByCreatedAtDescIdDesc(tripId)
        .map(FinancialAuditLog::getCurrentHash)
        .orElse(GENESIS_HASH);
    LocalDateTime createdAt = LocalDateTime.now();
    String payload = String.join("|",
        String.valueOf(tripId),
        actionType,
        OPERATOR_SYSTEM,
        nullSafe(originalValue),
        nullSafe(modifiedValue),
        previousHash,
        createdAt.toString());
    String currentHash = sha256Hex(payload);
    financialAuditLogRepository.save(new FinancialAuditLog(
        tripId,
        actionType,
        OPERATOR_SYSTEM,
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
