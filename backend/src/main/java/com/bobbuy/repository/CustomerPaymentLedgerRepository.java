package com.bobbuy.repository;

import com.bobbuy.model.CustomerPaymentLedger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerPaymentLedgerRepository extends JpaRepository<CustomerPaymentLedger, Long> {
  List<CustomerPaymentLedger> findByTripIdOrderByCreatedAtAscIdAsc(Long tripId);

  List<CustomerPaymentLedger> findByTripIdAndBusinessIdOrderByCreatedAtAscIdAsc(Long tripId, String businessId);

  List<CustomerPaymentLedger> findByCustomerIdOrderByCreatedAtAscIdAsc(Long customerId);
}
