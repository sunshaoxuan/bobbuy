package com.bobbuy.api.response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ApiError> handleApiException(ApiException ex) {
    log.warn("API error: {}", ex.getMessage());
    return ResponseEntity.status(resolveStatus(ex.getErrorCode()))
        .body(new ApiError(ex.getErrorCode().name(), ex.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidationException(MethodArgumentNotValidException ex) {
    String message = ex.getBindingResult().getFieldErrors().stream()
        .map(FieldError::getDefaultMessage)
        .collect(Collectors.joining("; "));
    return ResponseEntity.badRequest().body(new ApiError(ErrorCode.INVALID_REQUEST.name(), message));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
    log.error("Unexpected error", ex);
    return ResponseEntity.internalServerError()
        .body(new ApiError(ErrorCode.INTERNAL_ERROR.name(), "服务器繁忙，请稍后再试"));
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
