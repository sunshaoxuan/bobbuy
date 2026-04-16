package com.bobbuy.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class LlmGateway {
  private final ObjectMapper objectMapper;
  private final String ollamaUrl;
  private final String model;

  public LlmGateway(ObjectMapper objectMapper,
      @Value("${bobbuy.ai.ollama.url:}") String ollamaUrl,
      @Value("${bobbuy.ai.ollama.model:qwen2.5:7b}") String model) {
    this.objectMapper = objectMapper;
    this.ollamaUrl = ollamaUrl;
    this.model = model;
  }

  public Optional<List<Map<String, Object>>> parseItems(String text) {
    if (text == null || text.isBlank() || ollamaUrl == null || ollamaUrl.isBlank()) {
      return Optional.empty();
    }

    String prompt = "请将输入文本提取为JSON数组，每个对象包含name, quantity, note字段，只输出JSON数组。输入：" + text;
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("model", model);
    payload.put("stream", false);
    payload.put("prompt", prompt);

    try {
      Map<String, Object> raw = RestClient.create(ollamaUrl)
          .post()
          .uri("/api/generate")
          .contentType(MediaType.APPLICATION_JSON)
          .body(payload)
          .retrieve()
          .body(new ParameterizedTypeReference<>() {
          });
      if (raw == null || !(raw.get("response") instanceof String response) || response.isBlank()) {
        return Optional.empty();
      }
      List<Map<String, Object>> parsed = objectMapper.readValue(response, new TypeReference<>() {
      });
      return Optional.of(parsed == null ? new ArrayList<>() : parsed);
    } catch (Exception ignored) {
      return Optional.empty();
    }
  }
}
