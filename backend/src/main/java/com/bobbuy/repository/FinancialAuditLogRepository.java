package com.bobbuy.repository;

import com.bobbuy.model.FinancialAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FinancialAuditLogRepository extends JpaRepository<FinancialAuditLog, Long> {
  List<FinancialAuditLog> findByTripIdOrderByCreatedAtDescIdDesc(Long tripId);

  Optional<FinancialAuditLog> findTopByTripIdOrderByCreatedAtDescIdDesc(Long tripId);
}
