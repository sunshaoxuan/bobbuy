package com.bobbuy.api;

import com.bobbuy.api.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
  @GetMapping("/api/health")
  public ApiResponse<String> health() {
    return ApiResponse.success("ok");
  }
}
