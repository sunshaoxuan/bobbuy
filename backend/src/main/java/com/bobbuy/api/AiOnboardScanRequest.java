package com.bobbuy.api;

import jakarta.validation.constraints.NotBlank;

public class AiOnboardScanRequest {
  @NotBlank
  private String base64Image;
  private String sampleId;

  public String getBase64Image() {
    return base64Image;
  }

  public void setBase64Image(String base64Image) {
    this.base64Image = base64Image;
  }

  public String getSampleId() {
    return sampleId;
  }

  public void setSampleId(String sampleId) {
    this.sampleId = sampleId;
  }
}
