package com.bobbuy.repository;

import com.bobbuy.model.TripLogisticsTracking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TripLogisticsTrackingRepository extends JpaRepository<TripLogisticsTracking, Long> {
  List<TripLogisticsTracking> findByTripIdOrderByUpdatedAtDescIdDesc(Long tripId);
}
