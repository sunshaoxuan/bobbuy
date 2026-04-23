package com.bobbuy.repository;

import com.bobbuy.model.ProcurementReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProcurementReceiptRepository extends JpaRepository<ProcurementReceipt, Long> {
  List<ProcurementReceipt> findByTripIdOrderByUploadedAtDescIdDesc(Long tripId);

  Optional<ProcurementReceipt> findByIdAndTripId(Long id, Long tripId);
}
