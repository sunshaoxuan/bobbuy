package com.bobbuy.service;

import com.bobbuy.api.BobbuyApplication;
import com.bobbuy.repository.ProductRepository;
import com.bobbuy.repository.TripRepository;
import com.bobbuy.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = BobbuyApplication.class, properties = "bobbuy.seed.enabled=true")
class BobbuyStoreSeedEnabledIntegrationTest {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private TripRepository tripRepository;

  @Autowired
  private ProductRepository productRepository;

  @Test
  void seedsDataWhenExplicitlyEnabled() {
    assertThat(userRepository.count()).isGreaterThanOrEqualTo(2);
    assertThat(tripRepository.count()).isGreaterThanOrEqualTo(1);
    assertThat(productRepository.count()).isGreaterThanOrEqualTo(1);
  }
}
