package com.bobbuy.service.client;

import com.bobbuy.api.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "ai-service", path = "/internal/ai")
public interface AiReceiptRecognitionClient {
  @PostMapping("/receipt-recognition")
  ApiResponse<Map<String, Object>> recognizeReceipt(@RequestBody AiReceiptRecognitionRequest request);
}
