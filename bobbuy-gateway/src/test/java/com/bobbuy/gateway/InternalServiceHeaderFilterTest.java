package com.bobbuy.gateway;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

class InternalServiceHeaderFilterTest {

  @Test
  void configuredTokenReplacesForgedHeaders() {
    InternalServiceHeaderFilter filter = new InternalServiceHeaderFilter("service-token", "gateway-service");
    AtomicReference<HttpHeaders> capturedHeaders = new AtomicReference<>();
    ServerWebExchange exchange = MockServerWebExchange.from(
        MockServerHttpRequest.get("/api/orders")
            .header(InternalServiceHeaderFilter.SERVICE_TOKEN_HEADER, "forged")
            .header(InternalServiceHeaderFilter.INTERNAL_SERVICE_HEADER, "browser")
            .build());

    filter.filter(exchange, captureHeaders(capturedHeaders)).block();

    assertThat(capturedHeaders.get().getFirst(InternalServiceHeaderFilter.SERVICE_TOKEN_HEADER))
        .isEqualTo("service-token");
    assertThat(capturedHeaders.get().getFirst(InternalServiceHeaderFilter.INTERNAL_SERVICE_HEADER))
        .isEqualTo("gateway-service");
  }

  @Test
  void blankTokenOnlyStripsForgedHeaders() {
    InternalServiceHeaderFilter filter = new InternalServiceHeaderFilter("", "gateway-service");
    AtomicReference<HttpHeaders> capturedHeaders = new AtomicReference<>();
    ServerWebExchange exchange = MockServerWebExchange.from(
        MockServerHttpRequest.get("/api/orders")
            .header(InternalServiceHeaderFilter.SERVICE_TOKEN_HEADER, "forged")
            .header(InternalServiceHeaderFilter.INTERNAL_SERVICE_HEADER, "browser")
            .build());

    filter.filter(exchange, captureHeaders(capturedHeaders)).block();

    assertThat(capturedHeaders.get().containsKey(InternalServiceHeaderFilter.SERVICE_TOKEN_HEADER)).isFalse();
    assertThat(capturedHeaders.get().containsKey(InternalServiceHeaderFilter.INTERNAL_SERVICE_HEADER)).isFalse();
  }

  private GatewayFilterChain captureHeaders(AtomicReference<HttpHeaders> capturedHeaders) {
    return exchange -> {
      capturedHeaders.set(exchange.getRequest().getHeaders());
      return Mono.empty();
    };
  }
}
