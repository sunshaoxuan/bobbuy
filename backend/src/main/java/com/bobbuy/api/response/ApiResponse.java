package com.bobbuy.api.response;

public class ApiResponse<T> {
  private final String status;
  private final T data;
  private final ApiMeta meta;

  private ApiResponse(String status, T data, ApiMeta meta) {
    this.status = status;
    this.data = data;
    this.meta = meta;
  }

  public static <T> ApiResponse<T> success(T data) {
    return new ApiResponse<>("success", data, null);
  }

  public static <T> ApiResponse<T> success(T data, ApiMeta meta) {
    return new ApiResponse<>("success", data, meta);
  }

  public String getStatus() {
    return status;
  }

  public T getData() {
    return data;
  }

  public ApiMeta getMeta() {
    return meta;
  }
}
