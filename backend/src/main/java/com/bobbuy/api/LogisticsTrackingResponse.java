package com.bobbuy.api;

import java.time.LocalDateTime;

public class LogisticsTrackingResponse {
  private final Long id;
  private final Long tripId;
  private final String trackingNumber;
  private final String channel;
  private final String provider;
  private final String status;
  private final String lastMessage;
  private final boolean settlementReminderTriggered;
  private final LocalDateTime lastCheckedAt;

  public LogisticsTrackingResponse(Long id,
                                   Long tripId,
                                   String trackingNumber,
                                   String channel,
                                   String provider,
                                   String status,
                                   String lastMessage,
                                   boolean settlementReminderTriggered,
                                   LocalDateTime lastCheckedAt) {
    this.id = id;
    this.tripId = tripId;
    this.trackingNumber = trackingNumber;
    this.channel = channel;
    this.provider = provider;
    this.status = status;
    this.lastMessage = lastMessage;
    this.settlementReminderTriggered = settlementReminderTriggered;
    this.lastCheckedAt = lastCheckedAt;
  }

  public Long getId() {
    return id;
  }

  public Long getTripId() {
    return tripId;
  }

  public String getTrackingNumber() {
    return trackingNumber;
  }

  public String getChannel() {
    return channel;
  }

  public String getProvider() {
    return provider;
  }

  public String getStatus() {
    return status;
  }

  public String getLastMessage() {
    return lastMessage;
  }

  public boolean isSettlementReminderTriggered() {
    return settlementReminderTriggered;
  }

  public LocalDateTime getLastCheckedAt() {
    return lastCheckedAt;
  }
}
