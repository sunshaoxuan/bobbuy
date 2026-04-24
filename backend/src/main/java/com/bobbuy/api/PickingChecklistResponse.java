package com.bobbuy.api;

import java.util.List;

public class PickingChecklistResponse {
  private final String businessId;
  private final Long customerId;
  private final String customerName;
  private final String deliveryStatus;
  private final String addressSummary;
  private final boolean readyForDelivery;
  private final List<PickingChecklistItemResponse> items;

  public PickingChecklistResponse(String businessId,
                                  Long customerId,
                                  String customerName,
                                  String deliveryStatus,
                                  String addressSummary,
                                  boolean readyForDelivery,
                                  List<PickingChecklistItemResponse> items) {
    this.businessId = businessId;
    this.customerId = customerId;
    this.customerName = customerName;
    this.deliveryStatus = deliveryStatus;
    this.addressSummary = addressSummary;
    this.readyForDelivery = readyForDelivery;
    this.items = items;
  }

  public String getBusinessId() {
    return businessId;
  }

  public Long getCustomerId() {
    return customerId;
  }

  public String getCustomerName() {
    return customerName;
  }

  public String getDeliveryStatus() {
    return deliveryStatus;
  }

  public String getAddressSummary() {
    return addressSummary;
  }

  public boolean isReadyForDelivery() {
    return readyForDelivery;
  }

  public List<PickingChecklistItemResponse> getItems() {
    return items;
  }
}
