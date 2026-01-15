package com.bobbuy.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class Order {
  private Long id;

  @NotNull
  private Long customerId;

  @NotNull
  private Long tripId;

  @NotBlank
  private String itemName;

  @Min(1)
  private int quantity;

  @Min(0)
  private double unitPrice;

  @Min(0)
  private double serviceFee;

  @Min(0)
  private double estimatedTax;

  @NotBlank
  private String currency;

  @NotNull
  private OrderStatus status;

  private LocalDateTime statusUpdatedAt;

  public Order() {
  }

  public Order(Long id, Long customerId, Long tripId, String itemName, int quantity, double unitPrice, double serviceFee,
               double estimatedTax, String currency, OrderStatus status, LocalDateTime statusUpdatedAt) {
    this.id = id;
    this.customerId = customerId;
    this.tripId = tripId;
    this.itemName = itemName;
    this.quantity = quantity;
    this.unitPrice = unitPrice;
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

  public String getItemName() {
    return itemName;
  }

  public void setItemName(String itemName) {
    this.itemName = itemName;
  }

  public int getQuantity() {
    return quantity;
  }

  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }

  public double getUnitPrice() {
    return unitPrice;
  }

  public void setUnitPrice(double unitPrice) {
    this.unitPrice = unitPrice;
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
