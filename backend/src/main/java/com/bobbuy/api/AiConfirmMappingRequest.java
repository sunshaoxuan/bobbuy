package com.bobbuy.api;

import jakarta.validation.constraints.NotBlank;

public class AiConfirmMappingRequest {
  @NotBlank(message = "{validation.common.not_blank}")
  private String originalName;

  @NotBlank(message = "{validation.common.not_blank}")
  private String matchedName;

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
}
