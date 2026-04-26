package com.bobbuy.gateway;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class GatewayHealthController {
  @GetMapping("/api/health")
  public Map<String, String> health() {
    return Map.of("status", "ok", "service", "gateway-service");
  }
}
