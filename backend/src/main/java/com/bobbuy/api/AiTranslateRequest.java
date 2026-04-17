package com.bobbuy.api;

import jakarta.validation.constraints.NotBlank;

public class AiTranslateRequest {
  @NotBlank
  private String text;
  @NotBlank
  private String targetLocale;

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public String getTargetLocale() {
    return targetLocale;
  }

  public void setTargetLocale(String targetLocale) {
    this.targetLocale = targetLocale;
  }
}
