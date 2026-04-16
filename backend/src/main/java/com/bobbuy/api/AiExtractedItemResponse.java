package com.bobbuy.api;

public class AiExtractedItemResponse {
  private String id;
  private String originalName;
  private String matchedName;
  private int quantity;
  private String note;
  private double price;
  private double confidence;

  public AiExtractedItemResponse() {
  }

  public AiExtractedItemResponse(String id, String originalName, String matchedName, int quantity, String note,
      double price, double confidence) {
    this.id = id;
    this.originalName = originalName;
    this.matchedName = matchedName;
    this.quantity = quantity;
    this.note = note;
    this.price = price;
    this.confidence = confidence;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getOriginalName() {
    return originalName;
  }

  public void setOriginalName(String originalName) {
    this.originalName = originalName;
  }

  public String getMatchedName() {
    return matchedName;
  }

  public void setMatchedName(String matchedName) {
    this.matchedName = matchedName;
  }

  public int getQuantity() {
    return quantity;
  }

  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }

  public double getPrice() {
    return price;
  }

  public void setPrice(double price) {
    this.price = price;
  }

  public double getConfidence() {
    return confidence;
  }

  public void setConfidence(double confidence) {
    this.confidence = confidence;
  }
}
