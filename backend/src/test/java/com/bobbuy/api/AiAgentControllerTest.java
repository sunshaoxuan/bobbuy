package com.bobbuy.api;

import com.bobbuy.api.response.ApiResponse;
import com.bobbuy.model.ExperienceMapping;
import com.bobbuy.repository.ExperienceMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AiAgentControllerTest {
  @Autowired
  private AiAgentController controller;

  @Autowired
  private ExperienceMappingRepository experienceMappingRepository;

  @BeforeEach
  void setUp() {
    experienceMappingRepository.deleteAll();
  }

  @Test
  void parseExtractsStructuredItemsFromFuzzyText() {
    AiParseRequest request = new AiParseRequest();
    request.setText("马粪蛋糕两个，还有 Tomato");

    ResponseEntity<ApiResponse<AiParseResponse>> response = controller.parse(request);

    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getData().getItems()).hasSize(2);
    AiExtractedItemResponse muffin = response.getBody().getData().getItems().stream()
        .filter(item -> "Muffin".equals(item.getMatchedName()))
        .findFirst()
        .orElseThrow();
    assertThat(muffin.getQuantity()).isEqualTo(2);
    assertThat(muffin.getConfidence()).isGreaterThanOrEqualTo(0.85);
  }

  @Test
  void confirmMappingPersistsExperienceAndEnhancesMatch() {
    AiConfirmMappingRequest confirm = new AiConfirmMappingRequest();
    confirm.setOriginalName("马粪杯糕");
    confirm.setMatchedName("Muffin");

    controller.confirmMapping(confirm);

    ExperienceMapping saved = experienceMappingRepository.findByFuzzyTerm("马粪杯糕").orElseThrow();
    assertThat(saved.getMappedName()).isEqualTo("Muffin");

    AiParseRequest request = new AiParseRequest();
    request.setText("马粪杯糕一个");
    ResponseEntity<ApiResponse<AiParseResponse>> parsed = controller.parse(request);
    assertThat(parsed.getBody()).isNotNull();
    assertThat(parsed.getBody().getData().getItems()).hasSize(1);
    assertThat(parsed.getBody().getData().getItems().get(0).getMatchedName()).isEqualTo("Muffin");
    assertThat(parsed.getBody().getData().getItems().get(0).getConfidence()).isGreaterThanOrEqualTo(0.85);
  }
}
