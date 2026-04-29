package com.bobbuy.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class TokenAuthenticationFilter extends OncePerRequestFilter {
    private static final String BEARER_PREFIX = "Bearer ";
    private static final Logger log = LoggerFactory.getLogger(TokenAuthenticationFilter.class);

    private final BearerTokenAuthenticationService bearerTokenAuthenticationService;

    public TokenAuthenticationFilter(BearerTokenAuthenticationService bearerTokenAuthenticationService) {
        this.bearerTokenAuthenticationService = bearerTokenAuthenticationService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        return "/api/auth/login".equals(requestUri)
            || "/api/auth/refresh".equals(requestUri)
            || "/api/auth/logout".equals(requestUri);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            try {
                SecurityContextHolder.getContext().setAuthentication(
                    bearerTokenAuthenticationService.authenticate(header.substring(BEARER_PREFIX.length()).trim())
                );
            } catch (IllegalArgumentException ex) {
                log.debug("Rejecting bearer token: {}", ex.getMessage());
                SecurityContextHolder.clearContext();
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
