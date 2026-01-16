package com.bobbuy.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.bobbuy")
public class BobbuyApplication {
  public static void main(String[] args) {
    SpringApplication.run(BobbuyApplication.class, args);
  }
}
