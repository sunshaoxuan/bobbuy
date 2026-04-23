package com.bobbuy.service;

import com.bobbuy.model.OrderHeader;
import com.bobbuy.model.OrderLine;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcurementReceiptRecognitionServiceTest {
  @Mock
  private LlmGateway llmGateway;

  private ProcurementReceiptRecognitionService service;

  @BeforeEach
  void setUp() {
    service = new ProcurementReceiptRecognitionService(llmGateway, new ObjectMapper());
  }

  @Test
  void recognizeUsesAiExtractionWhenAvailable() {
    when(llmGateway.generate(anyString(), eq("llava"), anyList()))
        .thenReturn(Optional.of("""
            {
              "merchantName":"Tokyo Mart",
              "receiptDate":"2026-04-23",
              "currency":"JPY",
              "summary":"识别两条商品行，第二条价格有轻微模糊。",
              "receiptItems":[
                {"name":"Matcha Kit","quantity":2,"unitPrice":32.5,"totalPrice":65},
                {"name":"Rice Crackers","quantity":1,"unitPrice":15,"totalPrice":15}
              ]
            }
            """));

    OrderHeader order = new OrderHeader("BIZ-1", 1001L, 2000L);
    order.addLine(new OrderLine("SKU-1", "Matcha Kit", null, 2, 32.5));

    Map<String, Object> result = service.recognize("base64", "receipt.jpg", List.of(order));

    assertThat(result.get("recognitionMode")).isEqualTo("AI");
    assertThat((String) result.get("merchantName")).isEqualTo("Tokyo Mart");
    assertThat((List<?>) result.get("receiptItems")).hasSize(2);
    assertThat((List<?>) result.get("matchedOrderLines")).hasSize(1);
    assertThat((List<?>) result.get("unmatchedReceiptItems")).hasSize(1);
  }

  @Test
  void recognizeFallsBackWhenAiUnavailable() {
    when(llmGateway.generate(anyString(), eq("llava"), anyList()))
        .thenReturn(Optional.empty());

    OrderHeader order = new OrderHeader("BIZ-1", 1001L, 2000L);
    OrderLine line = new OrderLine("SKU-1", "Matcha Kit", null, 2, 32.5);
    line.setPurchasedQuantity(1);
    order.addLine(line);

    Map<String, Object> result = service.recognize("base64", "receipt.jpg", List.of(order));

    assertThat(result.get("recognitionMode")).isEqualTo("RULE_FALLBACK");
    assertThat((List<?>) result.get("receiptItems")).hasSize(1);
    assertThat((List<?>) result.get("missingOrderedItems")).hasSize(1);
  }
}
