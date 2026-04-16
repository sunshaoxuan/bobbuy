package com.bobbuy.repository;

import com.bobbuy.model.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, Long> {
    Optional<Trip> findTopByOrderByIdDesc();
}
