package com.bobbuy.api.response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
  private final MessageSource messageSource;

  public GlobalExceptionHandler(MessageSource messageSource) {
    this.messageSource = messageSource;
  }

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ApiError> handleApiException(ApiException ex) {
    log.warn("API error: {}", ex.getMessage());
    String message = messageSource.getMessage(ex.getMessageKey(), ex.getMessageArgs(), "Error",
        LocaleContextHolder.getLocale());
    return ResponseEntity.status(resolveStatus(ex.getErrorCode()))
        .body(new ApiError(ex.getErrorCode().name(), message));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidationException(MethodArgumentNotValidException ex) {
    String message = ex.getBindingResult().getFieldErrors().stream()
        .map(error -> messageSource.getMessage(error, LocaleContextHolder.getLocale()))
        .collect(Collectors.joining("; "));
    return ResponseEntity.badRequest().body(new ApiError(ErrorCode.INVALID_REQUEST.name(), message));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
    log.error("Unexpected error", ex);
    String message = messageSource.getMessage("error.internal", null, LocaleContextHolder.getLocale());
    return ResponseEntity.internalServerError()
        .body(new ApiError(ErrorCode.INTERNAL_ERROR.name(), message));
  }

  private int resolveStatus(ErrorCode errorCode) {
    return switch (errorCode) {
      case RESOURCE_NOT_FOUND -> 404;
      case CAPACITY_NOT_ENOUGH -> 409;
      case INVALID_REQUEST, INVALID_STATUS -> 400;
      default -> 500;
    };
  }
}
