package com.bobbuy.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "bb_trip_logistics_tracking")
public class TripLogisticsTracking {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long tripId;
  private String trackingNumber;

  @Enumerated(EnumType.STRING)
  private LogisticsChannel channel;

  @Enumerated(EnumType.STRING)
  private LogisticsProvider provider;

  @Enumerated(EnumType.STRING)
  private LogisticsStatus status;

  private String lastMessage;
  private boolean settlementReminderTriggered;
  private LocalDateTime lastCheckedAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public TripLogisticsTracking() {
  }

  public TripLogisticsTracking(Long tripId,
                               String trackingNumber,
                               LogisticsChannel channel,
                               LogisticsProvider provider,
                               LogisticsStatus status,
                               String lastMessage,
                               boolean settlementReminderTriggered,
                               LocalDateTime lastCheckedAt,
                               LocalDateTime createdAt,
                               LocalDateTime updatedAt) {
    this.tripId = tripId;
    this.trackingNumber = trackingNumber;
    this.channel = channel;
    this.provider = provider;
    this.status = status;
    this.lastMessage = lastMessage;
    this.settlementReminderTriggered = settlementReminderTriggered;
    this.lastCheckedAt = lastCheckedAt;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public Long getId() {
    return id;
  }

  public Long getTripId() {
    return tripId;
  }

  public void setTripId(Long tripId) {
    this.tripId = tripId;
  }

  public String getTrackingNumber() {
    return trackingNumber;
  }

  public void setTrackingNumber(String trackingNumber) {
    this.trackingNumber = trackingNumber;
  }

  public LogisticsChannel getChannel() {
    return channel;
  }

  public void setChannel(LogisticsChannel channel) {
    this.channel = channel;
  }

  public LogisticsProvider getProvider() {
    return provider;
  }

  public void setProvider(LogisticsProvider provider) {
    this.provider = provider;
  }

  public LogisticsStatus getStatus() {
    return status;
  }

  public void setStatus(LogisticsStatus status) {
    this.status = status;
  }

  public String getLastMessage() {
    return lastMessage;
  }

  public void setLastMessage(String lastMessage) {
    this.lastMessage = lastMessage;
  }

  public boolean isSettlementReminderTriggered() {
    return settlementReminderTriggered;
  }

  public void setSettlementReminderTriggered(boolean settlementReminderTriggered) {
    this.settlementReminderTriggered = settlementReminderTriggered;
  }

  public LocalDateTime getLastCheckedAt() {
    return lastCheckedAt;
  }

  public void setLastCheckedAt(LocalDateTime lastCheckedAt) {
    this.lastCheckedAt = lastCheckedAt;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
