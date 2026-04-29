package com.bobbuy.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class InternalServiceTokenFilter extends OncePerRequestFilter {
    public static final String SERVICE_TOKEN_HEADER = "X-BOBBUY-SERVICE-TOKEN";
    public static final String INTERNAL_SERVICE_HEADER = "X-BOBBUY-INTERNAL-SERVICE";

    private static final Logger log = LoggerFactory.getLogger(InternalServiceTokenFilter.class);

    private final String configuredToken;

    public InternalServiceTokenFilter(@Value("${bobbuy.security.service-token.secret:}") String configuredToken) {
        this.configuredToken = normalize(configuredToken);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        boolean internalEndpoint = isInternalEndpoint(request);
        String providedToken = normalize(request.getHeader(SERVICE_TOKEN_HEADER));
        String providedServiceName = normalize(request.getHeader(INTERNAL_SERVICE_HEADER));
        boolean attemptingInternalAuth = hasText(providedToken) || hasText(providedServiceName);

        if (!hasText(configuredToken)) {
            if (internalEndpoint) {
                log.debug("Rejecting internal endpoint access because service token trust is not configured");
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }

        if (!attemptingInternalAuth) {
            if (internalEndpoint) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }

        if (!tokensMatch(configuredToken, providedToken)) {
            log.debug("Rejecting internal service token for path={}", request.getRequestURI());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        request.setAttribute(
            InternalServiceIdentity.REQUEST_ATTRIBUTE,
            new InternalServiceIdentity(hasText(providedServiceName) ? providedServiceName : "internal-service")
        );
        filterChain.doFilter(request, response);
    }

    private boolean isInternalEndpoint(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        return requestUri != null && requestUri.startsWith("/internal/");
    }

    private boolean tokensMatch(String expected, String provided) {
        if (!hasText(provided)) {
            return false;
        }
        return MessageDigest.isEqual(
            expected.getBytes(StandardCharsets.UTF_8),
            provided.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
