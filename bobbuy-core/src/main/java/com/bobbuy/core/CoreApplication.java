package com.bobbuy.core;

import com.bobbuy.api.AiAgentController;
import com.bobbuy.api.BobbuyApplication;
import com.bobbuy.api.ChatController;
import com.bobbuy.config.WebSocketConfig;
import com.bobbuy.service.client.AiReceiptRecognitionClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableFeignClients(basePackageClasses = AiReceiptRecognitionClient.class)
@EnableJpaRepositories(basePackages = "com.bobbuy.repository")
@EntityScan(basePackages = "com.bobbuy.model")
@ComponentScan(basePackages = "com.bobbuy",
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = AiAgentController.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ChatController.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = WebSocketConfig.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = BobbuyApplication.class)
    })
public class CoreApplication {
  public static void main(String[] args) {
    SpringApplication.run(CoreApplication.class, args);
  }
}
