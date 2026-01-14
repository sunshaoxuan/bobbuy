package com.bobbuy.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class Trip {
  private Long id;

  @NotNull
  private Long agentId;

  @NotBlank
  private String origin;

  @NotBlank
  private String destination;

  @NotNull
  private LocalDate departDate;

  @Min(1)
  private int capacity;

  @NotNull
  private TripStatus status;

  public Trip() {
  }

  public Trip(Long id, Long agentId, String origin, String destination, LocalDate departDate, int capacity, TripStatus status) {
    this.id = id;
    this.agentId = agentId;
    this.origin = origin;
    this.destination = destination;
    this.departDate = departDate;
    this.capacity = capacity;
    this.status = status;
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

  public TripStatus getStatus() {
    return status;
  }

  public void setStatus(TripStatus status) {
    this.status = status;
  }
}
