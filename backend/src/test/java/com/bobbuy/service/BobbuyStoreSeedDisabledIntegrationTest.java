package com.bobbuy.service;

import com.bobbuy.api.BobbuyApplication;
import com.bobbuy.repository.OrderHeaderRepository;
import com.bobbuy.repository.ProductRepository;
import com.bobbuy.repository.TripRepository;
import com.bobbuy.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = BobbuyApplication.class, properties = "bobbuy.seed.enabled=false")
class BobbuyStoreSeedDisabledIntegrationTest {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private TripRepository tripRepository;

  @Autowired
  private OrderHeaderRepository orderHeaderRepository;

  @Autowired
  private ProductRepository productRepository;

  @Test
  void doesNotSeedDataByDefault() {
    assertThat(userRepository.count()).isZero();
    assertThat(tripRepository.count()).isZero();
    assertThat(orderHeaderRepository.count()).isZero();
    assertThat(productRepository.count()).isZero();
  }
}
