package com.bobbuy.api;

public class DeliveryPreparationResponse {
  private final String businessId;
  private final Long customerId;
  private final String customerName;
  private final String deliveryStatus;
  private final String addressSummary;
  private final String contactName;
  private final String contactPhone;
  private final Double latitude;
  private final Double longitude;
  private final int totalPickItems;
  private final int pickedItems;

  public DeliveryPreparationResponse(String businessId,
                                     Long customerId,
                                     String customerName,
                                     String deliveryStatus,
                                     String addressSummary,
                                     String contactName,
                                     String contactPhone,
                                     Double latitude,
                                     Double longitude,
                                     int totalPickItems,
                                     int pickedItems) {
    this.businessId = businessId;
    this.customerId = customerId;
    this.customerName = customerName;
    this.deliveryStatus = deliveryStatus;
    this.addressSummary = addressSummary;
    this.contactName = contactName;
    this.contactPhone = contactPhone;
    this.latitude = latitude;
    this.longitude = longitude;
    this.totalPickItems = totalPickItems;
    this.pickedItems = pickedItems;
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

  public String getContactName() {
    return contactName;
  }

  public String getContactPhone() {
    return contactPhone;
  }

  public Double getLatitude() {
    return latitude;
  }

  public Double getLongitude() {
    return longitude;
  }

  public int getTotalPickItems() {
    return totalPickItems;
  }

  public int getPickedItems() {
    return pickedItems;
  }
}
