package com.bobbuy.gateway;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class InternalServiceHeaderFilter implements GlobalFilter, Ordered {
  static final String SERVICE_TOKEN_HEADER = "X-BOBBUY-SERVICE-TOKEN";
  static final String INTERNAL_SERVICE_HEADER = "X-BOBBUY-INTERNAL-SERVICE";

  private final String serviceToken;
  private final String serviceName;

  public InternalServiceHeaderFilter(
      @Value("${bobbuy.security.service-token.secret:}") String serviceToken,
      @Value("${spring.application.name:gateway-service}") String serviceName) {
    this.serviceToken = serviceToken;
    this.serviceName = serviceName;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
    ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
        .headers(headers -> {
          headers.remove(SERVICE_TOKEN_HEADER);
          headers.remove(INTERNAL_SERVICE_HEADER);
          if (serviceToken != null && !serviceToken.isBlank()) {
            headers.put(SERVICE_TOKEN_HEADER, List.of(serviceToken));
            headers.put(INTERNAL_SERVICE_HEADER, List.of(serviceName));
          }
        })
        .build();
    return chain.filter(exchange.mutate().request(mutatedRequest).build());
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }
}
