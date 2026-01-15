package com.bobbuy.api.response;

public class ApiError {
  private final String status;
  private final String errorCode;
  private final String message;

  public ApiError(String errorCode, String message) {
    this.status = "error";
    this.errorCode = errorCode;
    this.message = message;
  }

  public String getStatus() {
    return status;
  }

  public String getErrorCode() {
    return errorCode;
  }

  public String getMessage() {
    return message;
  }
}
