package com.bobbuy.gateway;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class GatewayHealthController {
  @GetMapping("/api/health")
  public Map<String, String> health() {
    return Map.of("status", "ok", "service", "gateway-service");
  }

  @GetMapping("/api/actuator/health")
  public Map<String, Object> actuatorHealth() {
    return actuatorResponse("UP");
  }

  @GetMapping("/api/actuator/health/readiness")
  public Map<String, Object> actuatorReadiness() {
    return actuatorResponse("UP");
  }

  private Map<String, Object> actuatorResponse(String status) {
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("status", status);
    response.put("service", "gateway-service");
    return response;
  }
}
