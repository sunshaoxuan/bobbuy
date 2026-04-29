package com.bobbuy.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
    classes = AuthApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.cloud.nacos.config.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:auth-smoke;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "bobbuy.security.jwt.secret=test-jwt-secret-for-integration",
        "bobbuy.security.header-auth.enabled=false",
        "bobbuy.security.service-token.secret=test-service-token"
    }
)
class AuthServiceSmokeTest {
  @Test
  void contextLoads() {
  }
}
