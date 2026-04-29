package com.bobbuy.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
    classes = GatewayApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.cloud.nacos.config.enabled=false",
        "bobbuy.security.service-token.secret=test-service-token"
    }
)
class GatewayServiceSmokeTest {
  @Test
  void contextLoads() {
  }
}
