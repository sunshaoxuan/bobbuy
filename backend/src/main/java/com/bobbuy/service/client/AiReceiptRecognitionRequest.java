package com.bobbuy.service.client;

import com.bobbuy.model.OrderHeader;

import java.util.List;

public record AiReceiptRecognitionRequest(String base64Image, String fileName, List<OrderHeader> orders) {
}
