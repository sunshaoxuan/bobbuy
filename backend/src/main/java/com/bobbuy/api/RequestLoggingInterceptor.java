package com.bobbuy.api;

import com.bobbuy.security.BobbuyAuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.security.Principal;
import java.util.UUID;

@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {
  private static final Logger log = LoggerFactory.getLogger(RequestLoggingInterceptor.class);
  private static final String TRACE_HEADER = "X-Trace-Id";
  private static final String USER_HEADER = "X-BOBBUY-USER";
  private static final String START_TIME = "requestStartTimeMs";
  private final RequestMetricsService requestMetricsService;

  public RequestLoggingInterceptor(RequestMetricsService requestMetricsService) {
    this.requestMetricsService = requestMetricsService;
  }

  @Override
  public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
      @NonNull Object handler) {
    String traceId = request.getHeader(TRACE_HEADER);
    if (traceId == null || traceId.isBlank()) {
      traceId = UUID.randomUUID().toString();
    }
    request.setAttribute(TRACE_HEADER, traceId);
    response.setHeader(TRACE_HEADER, traceId);
    request.setAttribute(START_TIME, System.currentTimeMillis());
    return true;
  }

  @Override
  public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
      @NonNull Object handler,
      @Nullable Exception ex) {
    Object start = request.getAttribute(START_TIME);
    long startTime = start instanceof Long ? (Long) start : System.currentTimeMillis();
    long costMs = System.currentTimeMillis() - startTime;
    String traceId = String.valueOf(request.getAttribute(TRACE_HEADER));
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String userId = resolveUser(request, authentication);
    String role = resolveRole(authentication);
    requestMetricsService.record(request.getMethod(), request.getRequestURI(), costMs, response.getStatus());
    log.info("[INFO] {} {} status={} cost={}ms trace_id={} user={} role={}",
        request.getMethod(),
        request.getRequestURI(),
        response.getStatus(),
        costMs,
        traceId,
        userId,
        role);
  }

  private String resolveUser(HttpServletRequest request, Authentication authentication) {
    String userId = request.getHeader(USER_HEADER);
    if (userId != null && !userId.isBlank()) {
      return userId.trim();
    }
    if (authentication == null) {
      return "anonymous";
    }
    Object principal = authentication.getPrincipal();
    if (principal instanceof BobbuyAuthenticatedUser authenticatedUser) {
      if (authenticatedUser.username() != null && !authenticatedUser.username().isBlank()) {
        return authenticatedUser.username();
      }
      return fallbackPrincipalName(authenticatedUser);
    }
    if (principal instanceof Principal namedPrincipal && namedPrincipal.getName() != null && !namedPrincipal.getName().isBlank()) {
      return namedPrincipal.getName();
    }
    return authentication.getName() != null && !authentication.getName().isBlank() ? authentication.getName() : "anonymous";
  }

  private String resolveRole(Authentication authentication) {
    if (authentication == null || authentication.getAuthorities() == null) {
      return "ANONYMOUS";
    }
    return authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .filter(authority -> authority != null && authority.startsWith("ROLE_"))
        .map(authority -> authority.substring("ROLE_".length()))
        .findFirst()
        .orElse("ANONYMOUS");
  }

  private String fallbackPrincipalName(BobbuyAuthenticatedUser authenticatedUser) {
    if (authenticatedUser.getName() != null && !authenticatedUser.getName().isBlank()) {
      return authenticatedUser.getName();
    }
    return "anonymous";
  }
}
