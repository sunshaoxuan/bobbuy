package com.bobbuy.service;

import com.bobbuy.model.AuditLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class AuditLogService {
  private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);
  private final AtomicLong auditId = new AtomicLong(1);
  private final List<AuditLog> logs = Collections.synchronizedList(new ArrayList<>());

  public void logStatusChange(String entityType, Long entityId, String beforeValue, String afterValue, Long userId) {
    AuditLog entry = new AuditLog(
        auditId.getAndIncrement(),
        entityType,
        entityId,
        "STATUS_CHANGE",
        beforeValue,
        afterValue,
        userId,
        LocalDateTime.now()
    );
    logs.add(entry);
    log.info("AuditLog entity={} id={} action={} from={} to={} byUser={}",
        entityType, entityId, entry.getAction(), beforeValue, afterValue, userId);
  }

  public List<AuditLog> listLogs() {
    return new ArrayList<>(logs);
  }
}
