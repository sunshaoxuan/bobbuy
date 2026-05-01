package com.bobbuy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class LlmGatewayTest {

  private HttpServer mainServer;
  private HttpServer bridgeServer;

  @AfterEach
  void tearDown() {
    if (mainServer != null) {
      mainServer.stop(0);
    }
    if (bridgeServer != null) {
      bridgeServer.stop(0);
    }
  }

  @Test
  void generateFallsBackToCodexBridgeWhenOllamaReturnsEmptyResponse() throws Exception {
    mainServer = HttpServer.create(new InetSocketAddress(0), 0);
    mainServer.createContext("/api/tags", exchange -> writeJson(exchange, 200, "{\"models\":[]}"));
    mainServer.createContext("/api/generate", exchange -> writeJson(exchange, 200, "{\"response\":\"\"}"));
    mainServer.start();

    bridgeServer = HttpServer.create(new InetSocketAddress(0), 0);
    bridgeServer.createContext("/v1/models", exchange -> writeJson(exchange, 200, "{\"data\":[]}"));
    bridgeServer.createContext("/v1/chat/completions", exchange -> writeJson(exchange, 200, """
        {
          "choices": [
            {
              "message": {
                "content": [
                  { "type": "text", "text": "{\\"name\\":\\"Mixed Seafood\\",\\"basePrice\\":2698}" }
                ]
              }
            }
          ]
        }
        """));
    bridgeServer.start();

    LlmGateway gateway = new LlmGateway(
        new ObjectMapper(),
        "http://127.0.0.1:" + mainServer.getAddress().getPort(),
        "qwen2.5:7b",
        "auto",
        "",
        1,
        "http://127.0.0.1:" + bridgeServer.getAddress().getPort() + "/v1",
        "gpt-5.3-codex",
        "test-key",
        5,
        "",
        "",
        "",
        "",
        200000,
        "",
        1,
        "",
        "llava",
        ""
    );
    gateway.initializeMainProvider();

    Optional<String> response = gateway.generate("Return JSON", null, null);

    assertThat(response).contains("{\"name\":\"Mixed Seafood\",\"basePrice\":2698}");
    assertThat(gateway.getActiveMainProvider()).isEqualTo("codex-bridge");
  }

  @Test
  void unavailableCodexCommandIsNotSelectedAsMainProvider() {
    LlmGateway gateway = new LlmGateway(
        new ObjectMapper(),
        "",
        "qwen2.5:7b",
        "codex",
        "definitely-not-installed-bobbuy-codex",
        1,
        "",
        "gpt-5.3-codex",
        "",
        5,
        "",
        "",
        "",
        "",
        200000,
        "",
        1,
        "",
        "llava",
        ""
    );

    gateway.initializeMainProvider();

    assertThat(gateway.getActiveMainProvider()).isEqualTo("unconfigured");
    assertThat(gateway.generate("Return JSON", null, null)).isEmpty();
  }

  private void writeJson(HttpExchange exchange, int status, String body) throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "application/json");
    exchange.sendResponseHeaders(status, bytes.length);
    try (OutputStream output = exchange.getResponseBody()) {
      output.write(bytes);
    }
  }
}
