package com.bobbuy.service;

import com.bobbuy.model.AuditLog;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuditLogServiceTest {
  @Test
  void logStatusChangeStoresCompleteEntry() {
    AuditLogService service = new AuditLogService();
    service.logStatusChange("TRIP", 2000L, "PUBLISHED", "IN_PROGRESS", 0L);

    List<AuditLog> logs = service.listLogs();
    assertThat(logs).hasSize(1);
    AuditLog log = logs.get(0);
    assertThat(log.getId()).isNotNull();
    assertThat(log.getEntityType()).isEqualTo("TRIP");
    assertThat(log.getEntityId()).isEqualTo(2000L);
    assertThat(log.getAction()).isEqualTo("STATUS_CHANGE");
    assertThat(log.getBeforeValue()).isEqualTo("PUBLISHED");
    assertThat(log.getAfterValue()).isEqualTo("IN_PROGRESS");
    assertThat(log.getUserId()).isEqualTo(0L);
    assertThat(log.getCreatedAt()).isNotNull();
  }

  @Test
  void logEntriesHaveUniqueIdsAndListIsIsolated() {
    AuditLogService service = new AuditLogService();
    service.logStatusChange("ORDER", 3000L, "CONFIRMED", "PURCHASED", 0L);
    service.logStatusChange("ORDER", 3001L, "PURCHASED", "DELIVERED", 0L);

    List<AuditLog> logs = service.listLogs();
    assertThat(logs).hasSize(2);
    assertThat(logs.get(0).getId()).isNotEqualTo(logs.get(1).getId());

    logs.clear();
    assertThat(service.listLogs()).hasSize(2);
  }
}
