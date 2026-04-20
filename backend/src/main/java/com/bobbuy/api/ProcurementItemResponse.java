package com.bobbuy.api;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ProcurementItemResponse {
  private String skuId;
  private String itemName;
  private int totalQuantity;
  private int purchasedQuantity;
  private double unitPrice;
  private List<String> businessIds;

  public ProcurementItemResponse(
      String skuId,
      String itemName,
      int totalQuantity,
      int purchasedQuantity,
      double unitPrice,
      List<String> businessIds) {
    this.skuId = skuId;
    this.itemName = itemName;
    this.totalQuantity = totalQuantity;
    this.purchasedQuantity = purchasedQuantity;
    this.unitPrice = unitPrice;
    this.businessIds = new ArrayList<>(businessIds);
  }

  public String getSkuId() {
    return skuId;
  }

  public String getItemName() {
    return itemName;
  }

  public int getTotalQuantity() {
    return totalQuantity;
  }

  public int getPurchasedQuantity() {
    return purchasedQuantity;
  }

  public double getUnitPrice() {
    return unitPrice;
  }

  public List<String> getBusinessIds() {
    return businessIds;
  }

  public void addQuantity(int quantity) {
    this.totalQuantity += quantity;
  }

  public void addPurchasedQuantity(int quantity) {
    this.purchasedQuantity += quantity;
  }

  public void addBusinessId(String businessId) {
    Set<String> unique = new LinkedHashSet<>(this.businessIds);
    unique.add(businessId);
    this.businessIds = new ArrayList<>(unique);
  }
}
