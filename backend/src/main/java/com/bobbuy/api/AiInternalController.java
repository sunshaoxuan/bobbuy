package com.bobbuy.api;

import com.bobbuy.api.response.ApiResponse;
import com.bobbuy.service.ProcurementReceiptRecognitionService;
import com.bobbuy.service.client.AiReceiptRecognitionRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/internal/ai")
public class AiInternalController {
  private final ProcurementReceiptRecognitionService procurementReceiptRecognitionService;

  public AiInternalController(ProcurementReceiptRecognitionService procurementReceiptRecognitionService) {
    this.procurementReceiptRecognitionService = procurementReceiptRecognitionService;
  }

  @PostMapping("/receipt-recognition")
  public ApiResponse<Map<String, Object>> recognizeReceipt(@RequestBody AiReceiptRecognitionRequest request) {
    return ApiResponse.success(procurementReceiptRecognitionService
        .recognizeLocally(request.base64Image(), request.fileName(), request.orders()));
  }
}
