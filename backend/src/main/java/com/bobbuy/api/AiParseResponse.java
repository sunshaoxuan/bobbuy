package com.bobbuy.api;

import java.util.ArrayList;
import java.util.List;

public class AiParseResponse {
  private List<AiExtractedItemResponse> items = new ArrayList<>();

  public AiParseResponse() {
  }

  public AiParseResponse(List<AiExtractedItemResponse> items) {
    this.items = items;
  }

  public List<AiExtractedItemResponse> getItems() {
    return items;
  }

  public void setItems(List<AiExtractedItemResponse> items) {
    this.items = items;
  }
}
