package com.bobbuy.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bb_trip")
public class Trip {
  @Id
  private Long id;

  @NotNull(message = "{validation.trip.agent_id.required}")
  private Long agentId;

  @NotBlank(message = "{validation.trip.origin.required}")
  private String origin;

  @NotBlank(message = "{validation.trip.destination.required}")
  private String destination;

  @NotNull(message = "{validation.trip.depart_date.required}")
  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate departDate;

  @Min(value = 1, message = "{validation.trip.capacity.min}")
  private int capacity;

  @Min(value = 0, message = "{validation.trip.reserved_capacity.min}")
  private int reservedCapacity;

  @NotNull(message = "{validation.trip.status.required}")
  @Enumerated(EnumType.STRING)
  private TripStatus status;

  private LocalDateTime statusUpdatedAt;

  @Transient
  private double currentWeight;

  @Transient
  private double currentVolume;

  public Trip() {
  }

  public Trip(Long id, Long agentId, String origin, String destination, LocalDate departDate, int capacity, int reservedCapacity,
              TripStatus status, LocalDateTime statusUpdatedAt) {
    this.id = id;
    this.agentId = agentId;
    this.origin = origin;
    this.destination = destination;
    this.departDate = departDate;
    this.capacity = capacity;
    this.reservedCapacity = reservedCapacity;
    this.status = status;
    this.statusUpdatedAt = statusUpdatedAt;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getAgentId() {
    return agentId;
  }

  public void setAgentId(Long agentId) {
    this.agentId = agentId;
  }

  public String getOrigin() {
    return origin;
  }

  public void setOrigin(String origin) {
    this.origin = origin;
  }

  public String getDestination() {
    return destination;
  }

  public void setDestination(String destination) {
    this.destination = destination;
  }

  public LocalDate getDepartDate() {
    return departDate;
  }

  public void setDepartDate(LocalDate departDate) {
    this.departDate = departDate;
  }

  public int getCapacity() {
    return capacity;
  }

  public void setCapacity(int capacity) {
    this.capacity = capacity;
  }

  public int getReservedCapacity() {
    return reservedCapacity;
  }

  public void setReservedCapacity(int reservedCapacity) {
    this.reservedCapacity = reservedCapacity;
  }

  public int getRemainingCapacity() {
    return Math.max(capacity - reservedCapacity, 0);
  }

  public TripStatus getStatus() {
    return status;
  }

  public void setStatus(TripStatus status) {
    this.status = status;
  }

  public LocalDateTime getStatusUpdatedAt() {
    return statusUpdatedAt;
  }

  public void setStatusUpdatedAt(LocalDateTime statusUpdatedAt) {
    this.statusUpdatedAt = statusUpdatedAt;
  }

  public double getCurrentWeight() {
    return currentWeight;
  }

  public void setCurrentWeight(double currentWeight) {
    this.currentWeight = Math.max(currentWeight, 0D);
  }

  public double getCurrentVolume() {
    return currentVolume;
  }

  public void setCurrentVolume(double currentVolume) {
    this.currentVolume = Math.max(currentVolume, 0D);
  }

  public void recalculateCurrentLoad(int purchasedQuantity, double weightPerUnit, double volumePerUnit) {
    setCurrentWeight(purchasedQuantity * Math.max(weightPerUnit, 0D));
    setCurrentVolume(purchasedQuantity * Math.max(volumePerUnit, 0D));
  }
}
