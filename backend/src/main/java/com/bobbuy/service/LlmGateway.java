package com.bobbuy.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(LlmGateway.class);
    private final ObjectMapper objectMapper;

    // Main Node (Cloud Core) - High performance reasoning
    private final String mainUrl;
    private final String mainModel;

    // Edge Node (Local) - Low latency, Vision/OCR
    private final String edgeUrl;
    private final String edgeModel;

    public LlmGateway(ObjectMapper objectMapper,
                      @Value("${bobbuy.ai.llm.main.url:}") String mainUrl,
                      @Value("${bobbuy.ai.llm.main.model:qwen3:14b}") String mainModel,
                      @Value("${bobbuy.ai.llm.edge.url:}") String edgeUrl,
                      @Value("${bobbuy.ai.llm.edge.model:llava}") String edgeModel) {
        this.objectMapper = objectMapper;
        this.mainUrl = mainUrl;
        this.mainModel = mainModel;
        this.edgeUrl = edgeUrl;
        this.edgeModel = edgeModel;
    }

    public Optional<List<Map<String, Object>>> parseItems(String text) {
        if (text == null || text.isBlank() || mainUrl.isBlank()) {
            return Optional.empty();
        }

        String prompt = "请将输入文本提取为JSON数组，每个对象包含name, quantity, note字段，只输出JSON数组。输入：" + text;
        return generate(prompt, mainModel, null).map(response -> {
            try {
                List<Map<String, Object>> parsed = objectMapper.readValue(response, new TypeReference<>() {});
                return parsed == null ? new ArrayList<>() : parsed;
            } catch (Exception e) {
                log.error("Failed to parse LLM response as JSON: {}", response, e);
                return new ArrayList<>();
            }
        });
    }

    public Optional<String> translate(String text, String targetLocale) {
        if (text == null || text.isBlank() || mainUrl.isBlank()) {
            return Optional.empty();
        }

        String prompt = String.format("请将以下文本翻译成%s，只输出翻译后的文本，不要有任何额外解释。文本：%s", targetLocale, text);
        return generate(prompt, mainModel, null).map(String::trim);
    }

    /**
     * Intelligent routing between Main and Edge nodes.
     * Vision tasks are pinned to the Edge node.
     * Complex reasoning tasks default to the Main node.
     */
    public Optional<String> generate(String prompt, String targetModel, List<String> base64Images) {
        boolean isVision = (base64Images != null && !base64Images.isEmpty()) || (targetModel != null && targetModel.contains("llava"));
        
        String url = isVision ? edgeUrl : mainUrl;
        String model = targetModel != null ? targetModel : (isVision ? edgeModel : mainModel);

        if (url == null || url.isBlank()) {
            log.warn("Target LLM URL is not configured (isVision: {})", isVision);
            // Attempt failover to main if edge is missing and it's not strictly vision
            if (isVision || mainUrl.isBlank()) return Optional.empty();
            url = mainUrl;
            model = mainModel;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("stream", false);
        payload.put("prompt", prompt);
        if (base64Images != null && !base64Images.isEmpty()) {
            payload.put("images", base64Images);
        }

        try {
            log.info("Dispatching LLM task to {} (model: {}, vision: {})", url, model, isVision);
            Map<String, Object> raw = RestClient.create(url)
                    .post()
                    .uri("/api/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
            
            if (raw == null || !(raw.get("response") instanceof String response) || response.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(response);
        } catch (Exception e) {
            log.error("LLM Gateway error at {}: {}", url, e.getMessage());
            // Failover: if edge failed, try main (if not vision). If main failed, try edge (as last resort).
            if (!isVision && url.equals(mainUrl) && !edgeUrl.isBlank()) {
                log.info("Attempting failover to Edge Node...");
                return generate(prompt, edgeModel, base64Images);
            }
            return Optional.empty();
        }
    }
}
