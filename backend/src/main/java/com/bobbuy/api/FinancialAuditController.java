package com.bobbuy.api;

import com.bobbuy.api.response.ApiMeta;
import com.bobbuy.api.response.ApiResponse;
import com.bobbuy.service.FinancialAuditTrailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/financial/audit")
public class FinancialAuditController {
  private final FinancialAuditTrailService auditService;

  public FinancialAuditController(FinancialAuditTrailService auditService) {
    this.auditService = auditService;
  }

  @GetMapping("/{tripId}")
  public ResponseEntity<ApiResponse<List<FinancialAuditLogResponse>>> listByTrip(@PathVariable Long tripId) {
    List<FinancialAuditLogResponse> logs = auditService.listByTripId(tripId);
    return ResponseEntity.ok(ApiResponse.success(logs, new ApiMeta(logs.size())));
  }

  @GetMapping("/{tripId}/check-integrity")
  public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkIntegrity(@PathVariable Long tripId) {
    boolean isValid = auditService.checkChainIntegrity(tripId);
    return ResponseEntity.ok(ApiResponse.success(Map.of("isValid", isValid)));
  }
}
