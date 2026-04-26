package com.bobbuy.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.bobbuy")
@EnableJpaRepositories(basePackages = "com.bobbuy.repository")
@EntityScan(basePackages = "com.bobbuy.model")
@EnableAsync
public class BobbuyApplication {
  public static void main(String[] args) {
    SpringApplication.run(BobbuyApplication.class, args);
  }
}
