package com.bobbuy.security;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class InternalServiceFeignRequestInterceptor {
    @Bean
    RequestInterceptor internalServiceRequestInterceptor(
        @Value("${bobbuy.security.service-token.secret:}") String serviceToken,
        @Value("${spring.application.name:bobbuy-service}") String serviceName
    ) {
        return template -> {
            if (serviceToken == null || serviceToken.isBlank()) {
                return;
            }
            template.header(InternalServiceTokenFilter.SERVICE_TOKEN_HEADER, serviceToken);
            template.header(InternalServiceTokenFilter.INTERNAL_SERVICE_HEADER, serviceName);
        };
    }
}
