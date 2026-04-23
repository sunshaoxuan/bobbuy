package com.bobbuy.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "bb_customer_payment_ledger")
public class CustomerPaymentLedger {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long tripId;
  private String businessId;
  private Long customerId;
  private double amount;
  private String paymentMethod;
  private String note;
  private LocalDateTime createdAt;
  private String operator;

  public CustomerPaymentLedger() {
  }

  public CustomerPaymentLedger(Long tripId,
                               String businessId,
                               Long customerId,
                               double amount,
                               String paymentMethod,
                               String note,
                               LocalDateTime createdAt,
                               String operator) {
    this.tripId = tripId;
    this.businessId = businessId;
    this.customerId = customerId;
    this.amount = amount;
    this.paymentMethod = paymentMethod;
    this.note = note;
    this.createdAt = createdAt;
    this.operator = operator;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getTripId() {
    return tripId;
  }

  public void setTripId(Long tripId) {
    this.tripId = tripId;
  }

  public String getBusinessId() {
    return businessId;
  }

  public void setBusinessId(String businessId) {
    this.businessId = businessId;
  }

  public Long getCustomerId() {
    return customerId;
  }

  public void setCustomerId(Long customerId) {
    this.customerId = customerId;
  }

  public double getAmount() {
    return amount;
  }

  public void setAmount(double amount) {
    this.amount = amount;
  }

  public String getPaymentMethod() {
    return paymentMethod;
  }

  public void setPaymentMethod(String paymentMethod) {
    this.paymentMethod = paymentMethod;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public String getOperator() {
    return operator;
  }

  public void setOperator(String operator) {
    this.operator = operator;
  }
}
