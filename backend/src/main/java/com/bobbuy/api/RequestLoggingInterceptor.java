package com.bobbuy.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {
  private static final Logger log = LoggerFactory.getLogger(RequestLoggingInterceptor.class);
  private static final String TRACE_HEADER = "X-Trace-Id";
  private static final String USER_HEADER = "X-User-Id";
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
    String userId = request.getHeader(USER_HEADER);
    if (userId == null || userId.isBlank()) {
      userId = "anonymous";
    }
    requestMetricsService.record(request.getMethod(), request.getRequestURI(), costMs);
    log.info("[INFO] {} {} status={} cost={}ms trace_id={} user={}",
        request.getMethod(),
        request.getRequestURI(),
        response.getStatus(),
        costMs,
        traceId,
        userId);
  }
}
