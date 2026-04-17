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
    return generate(prompt, model, null).map(response -> {
      try {
        List<Map<String, Object>> parsed = objectMapper.readValue(response, new TypeReference<>() {
        });
        return parsed == null ? new ArrayList<>() : parsed;
      } catch (Exception e) {
        return new ArrayList<>();
      }
    });
  }

  public Optional<String> translate(String text, String targetLocale) {
    if (text == null || text.isBlank() || ollamaUrl == null || ollamaUrl.isBlank()) {
      return Optional.empty();
    }

    String prompt = String.format("请将以下文本翻译成%s，只输出翻译后的文本，不要有任何额外解释。文本：%s", targetLocale, text);
    return generate(prompt, model, null).map(String::trim);
  }

  public Optional<String> generate(String prompt, String targetModel, List<String> base64Images) {
    if (prompt == null || prompt.isBlank() || ollamaUrl == null || ollamaUrl.isBlank()) {
      return Optional.empty();
    }

    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("model", targetModel != null ? targetModel : model);
    payload.put("stream", false);
    payload.put("prompt", prompt);
    if (base64Images != null && !base64Images.isEmpty()) {
      payload.put("images", base64Images);
    }

    try {
      Map<String, Object> raw = RestClient.create(ollamaUrl)
          .post()
          .uri("/api/generate")
          .contentType(MediaType.APPLICATION_JSON)
          .body(payload)
          .retrieve()
          .body(new ParameterizedTypeReference<Map<String, Object>>() {
          });
      if (raw == null || !(raw.get("response") instanceof String response) || response.isBlank()) {
        return Optional.empty();
      }
      return Optional.of(response);
    } catch (Exception ignored) {
      return Optional.empty();
    }
  }
}
