package com.bobbuy.repository;

import com.bobbuy.model.TripExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TripExpenseRepository extends JpaRepository<TripExpense, Long> {
  List<TripExpense> findByTripIdOrderByCreatedAtDescIdDesc(Long tripId);

  @Query("select coalesce(sum(e.cost), 0) from TripExpense e where e.tripId = :tripId")
  double sumCostByTripId(@Param("tripId") Long tripId);
}
