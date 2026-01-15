package com.bobbuy.api.response;

public class ApiMeta {
  private final int total;

  public ApiMeta(int total) {
    this.total = total;
  }

  public int getTotal() {
    return total;
  }
}
