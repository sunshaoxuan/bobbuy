package com.bobbuy.repository;

import com.bobbuy.model.OrderHeader;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderHeaderRepository extends JpaRepository<OrderHeader, Long> {
    Optional<OrderHeader> findByBusinessId(String businessId);

    List<OrderHeader> findByTripId(Long tripId);

    Optional<OrderHeader> findTopByOrderByIdDesc();
}
