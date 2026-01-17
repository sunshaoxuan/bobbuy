package com.bobbuy.api;

import com.bobbuy.api.response.ApiMeta;
import com.bobbuy.api.response.ApiResponse;
import com.bobbuy.model.AuditLog;
import com.bobbuy.service.AuditLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {
  private final AuditLogService auditLogService;

  public AuditLogController(AuditLogService auditLogService) {
    this.auditLogService = auditLogService;
  }

  @GetMapping
  public ApiResponse<List<AuditLog>> list() {
    List<AuditLog> logs = auditLogService.listLogs();
    return ApiResponse.success(logs, new ApiMeta(logs.size()));
  }
}
