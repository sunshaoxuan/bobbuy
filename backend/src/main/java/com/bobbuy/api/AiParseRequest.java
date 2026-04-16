package com.bobbuy.api;

import jakarta.validation.constraints.NotBlank;

public class AiParseRequest {
  @NotBlank(message = "{validation.common.not_blank}")
  private String text;

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }
}
