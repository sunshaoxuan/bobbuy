package com.bobbuy.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class Order {
  private Long id;

  @NotBlank(message = "{validation.order.business_key.required}")
  private String businessKey; // customerId-eventId

  @NotNull(message = "{validation.order.customer_id.required}")
  private Long customerId;

  @NotNull(message = "{validation.order.trip_id.required}")
  private Long tripId;

  private java.util.List<OrderItem> items = new java.util.ArrayList<>();

  @Min(value = 0, message = "{validation.order.service_fee.min}")
  private double serviceFee;

  @Min(value = 0, message = "{validation.order.estimated_tax.min}")
  private double estimatedTax;

  @NotBlank(message = "{validation.order.currency.required}")
  private String currency;

  @NotNull(message = "{validation.order.status.required}")
  private OrderStatus status;

  private LocalDateTime statusUpdatedAt;

  public Order() {
  }

  public Order(Long id, String businessKey, Long customerId, Long tripId, java.util.List<OrderItem> items,
      double serviceFee, double estimatedTax, String currency, OrderStatus status, LocalDateTime statusUpdatedAt) {
    this.id = id;
    this.businessKey = businessKey;
    this.customerId = customerId;
    this.tripId = tripId;
    this.items = items != null ? items : new java.util.ArrayList<>();
    this.serviceFee = serviceFee;
    this.estimatedTax = estimatedTax;
    this.currency = currency;
    this.status = status;
    this.statusUpdatedAt = statusUpdatedAt;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getBusinessKey() {
    return businessKey;
  }

  public void setBusinessKey(String businessKey) {
    this.businessKey = businessKey;
  }

  public Long getCustomerId() {
    return customerId;
  }

  public void setCustomerId(Long customerId) {
    this.customerId = customerId;
  }

  public Long getTripId() {
    return tripId;
  }

  public void setTripId(Long tripId) {
    this.tripId = tripId;
  }

  public java.util.List<OrderItem> getItems() {
    return items;
  }

  public void setItems(java.util.List<OrderItem> items) {
    this.items = items;
  }

  public void addItem(OrderItem item) {
    if (this.items == null) {
      this.items = new java.util.ArrayList<>();
    }
    this.items.add(item);
  }

  public double getServiceFee() {
    return serviceFee;
  }

  public void setServiceFee(double serviceFee) {
    this.serviceFee = serviceFee;
  }

  public double getEstimatedTax() {
    return estimatedTax;
  }

  public void setEstimatedTax(double estimatedTax) {
    this.estimatedTax = estimatedTax;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public OrderStatus getStatus() {
    return status;
  }

  public void setStatus(OrderStatus status) {
    this.status = status;
  }

  public LocalDateTime getStatusUpdatedAt() {
    return statusUpdatedAt;
  }

  public void setStatusUpdatedAt(LocalDateTime statusUpdatedAt) {
    this.statusUpdatedAt = statusUpdatedAt;
  }
}
