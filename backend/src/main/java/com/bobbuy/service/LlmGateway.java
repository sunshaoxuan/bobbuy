package com.bobbuy.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
public class LlmGateway {
    private static final Logger log = LoggerFactory.getLogger(LlmGateway.class);
    private final ObjectMapper objectMapper;

    // Main Node (Cloud Core) - High performance reasoning
    private final String mainUrl;
    private final String mainModel;
    private final String mainProvider;
    private final String codexCommand;
    private final Duration codexTimeout;
    private final Duration ollamaHealthTimeout;
    private volatile String activeMainProvider;

    // Edge Node (Local) - Low latency, Vision/OCR
    private final String edgeUrl;
    private final String edgeModel;
    private final String ocrUrl;

    public LlmGateway(ObjectMapper objectMapper,
                      @Value("${bobbuy.ai.llm.main.url:}") String mainUrl,
                      @Value("${bobbuy.ai.llm.main.model:qwen3:14b}") String mainModel,
                      @Value("${bobbuy.ai.llm.main.provider:auto}") String mainProvider,
                      @Value("${bobbuy.ai.llm.codex.command:codex}") String codexCommand,
                      @Value("${bobbuy.ai.llm.codex.timeout-seconds:120}") long codexTimeoutSeconds,
                      @Value("${bobbuy.ai.llm.ollama.health-timeout-seconds:3}") long ollamaHealthTimeoutSeconds,
                      @Value("${bobbuy.ai.llm.edge.url:}") String edgeUrl,
                      @Value("${bobbuy.ai.llm.edge.model:llava}") String edgeModel,
                      @Value("${bobbuy.ocr.url:}") String ocrUrl) {
        this.objectMapper = objectMapper;
        this.mainUrl = mainUrl;
        this.mainModel = mainModel;
        this.mainProvider = mainProvider == null || mainProvider.isBlank() ? "auto" : mainProvider.trim().toLowerCase();
        this.codexCommand = codexCommand;
        this.codexTimeout = Duration.ofSeconds(Math.max(1, codexTimeoutSeconds));
        this.ollamaHealthTimeout = Duration.ofSeconds(Math.max(1, ollamaHealthTimeoutSeconds));
        this.edgeUrl = edgeUrl;
        this.edgeModel = edgeModel;
        this.ocrUrl = ocrUrl;
    }

    @PostConstruct
    public void initializeMainProvider() {
        if ("codex".equals(mainProvider)) {
            activeMainProvider = isCodexConfigured() ? "codex" : "unconfigured";
        } else if (mainUrl == null || mainUrl.isBlank()) {
            activeMainProvider = isCodexConfigured() ? "codex" : "unconfigured";
        } else {
            activeMainProvider = isOllamaAvailable(mainUrl) ? "ollama" : (isCodexConfigured() ? "codex" : "unconfigured");
        }
        log.info("AI main LLM provider initialized: configured={}, active={}, ollamaUrl={}, codexCommand={}",
                mainProvider, activeMainProvider, blankToUnset(mainUrl), blankToUnset(codexCommand));
    }

    /**
     * Calls the dedicated Python OCR service.
     */
    public List<String> performOcr(String base64Image) {
        if (ocrUrl == null || ocrUrl.isBlank()) {
            log.warn("OCR service URL is not configured.");
            return List.of();
        }

        try {
            log.info("Calling OCR service at {}...", ocrUrl);
            Map<String, String> payload = Map.of("image", base64Image);
            
            // Use a fresh RestClient and explicit URI
            Map<String, Object> response = RestClient.builder()
                    .baseUrl(ocrUrl)
                    .build()
                    .post()
                    .uri("/ocr")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (response != null && response.get("results") instanceof List<?> results) {
                log.info("OCR service returned {} lines.", results.size());
                return results.stream()
                        .filter(Map.class::isInstance)
                        .map(m -> (Map<String, Object>) m)
                        .map(m -> (String) m.get("text"))
                        .toList();
            } else {
                log.warn("OCR service returned invalid response format: {}", response);
            }
        } catch (Exception e) {
            log.error("OCR Service call failed: {}", e.getMessage(), e);
        }
        return List.of();
    }

    public Optional<List<Map<String, Object>>> parseItems(String text) {
        if (text == null || text.isBlank() || !isMainLlmConfigured()) {
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
        if (text == null || text.isBlank() || !isMainLlmConfigured()) {
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

        if (!isVision && "codex".equals(resolveActiveMainProvider())) {
            return generateWithCodex(prompt);
        }
        
        String url = isVision ? edgeUrl : mainUrl;
        String model = targetModel != null && !targetModel.isBlank() ? targetModel : (isVision ? edgeModel : mainModel);

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
            if (!isVision && !"codex".equals(activeMainProvider) && isCodexConfigured()) {
                activeMainProvider = "codex";
                log.info("Ollama main node failed; switching active main LLM provider to Codex.");
                return generateWithCodex(prompt);
            }
            return Optional.empty();
        }
    }

    private boolean isMainLlmConfigured() {
        if ("codex".equals(resolveActiveMainProvider())) {
            return isCodexConfigured();
        }
        if ("unconfigured".equals(resolveActiveMainProvider())) {
            return false;
        }
        return mainUrl != null && !mainUrl.isBlank();
    }

    private String resolveActiveMainProvider() {
        if (activeMainProvider == null || activeMainProvider.isBlank()) {
            initializeMainProvider();
        }
        return activeMainProvider;
    }

    private boolean isOllamaAvailable(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        try {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(ollamaHealthTimeout);
            requestFactory.setReadTimeout(ollamaHealthTimeout);
            Map<String, Object> raw = RestClient.builder()
                    .baseUrl(url)
                    .requestFactory(requestFactory)
                    .build()
                    .get()
                    .uri("/api/tags")
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
            return raw != null;
        } catch (Exception e) {
            log.warn("Ollama main node is unavailable at {}: {}", url, e.getMessage());
            return false;
        }
    }

    private boolean isCodexConfigured() {
        return codexCommand != null && !codexCommand.isBlank();
    }

    private String blankToUnset(String value) {
        return value == null || value.isBlank() ? "<unset>" : value;
    }

    private Optional<String> generateWithCodex(String prompt) {
        if (codexCommand == null || codexCommand.isBlank()) {
            log.warn("Codex command is not configured.");
            return Optional.empty();
        }

        List<String> command = new ArrayList<>();
        command.add(codexCommand);
        command.add("exec");
        command.add("--skip-git-repo-check");
        command.add(prompt);

        Path outputFile = null;
        try {
            outputFile = Files.createTempFile("bobbuy-codex-", ".out");
            log.info("Dispatching LLM task to Codex subscription via {}", codexCommand);
            Process process = new ProcessBuilder(command)
                    .redirectOutput(outputFile.toFile())
                    .redirectErrorStream(true)
                    .start();

            boolean finished = process.waitFor(codexTimeout.toSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.error("Codex command timed out after {} seconds.", codexTimeout.toSeconds());
                return Optional.empty();
            }

            String output = Files.readString(outputFile, StandardCharsets.UTF_8).trim();

            if (process.exitValue() != 0) {
                log.error("Codex command failed with exit code {}: {}", process.exitValue(), output);
                return Optional.empty();
            }

            return output.isBlank() ? Optional.empty() : Optional.of(output);
        } catch (Exception e) {
            log.error("Codex Gateway error: {}", e.getMessage(), e);
            return Optional.empty();
        } finally {
            if (outputFile != null) {
                try {
                    Files.deleteIfExists(outputFile);
                } catch (Exception ignored) {
                    // Best-effort cleanup for transient command output.
                }
            }
        }
    }
}
