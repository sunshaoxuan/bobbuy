package com.bobbuy.api.response;

public class ApiException extends RuntimeException {
  private final ErrorCode errorCode;
  private final String messageKey;
  private final Object[] messageArgs;

  public ApiException(ErrorCode errorCode, String messageKey, Object... messageArgs) {
    super(messageKey);
    this.errorCode = errorCode;
    this.messageKey = messageKey;
    this.messageArgs = messageArgs == null ? new Object[0] : messageArgs.clone();
  }

  public ErrorCode getErrorCode() {
    return errorCode;
  }

  public String getMessageKey() {
    return messageKey;
  }

  public Object[] getMessageArgs() {
    return messageArgs.clone();
  }
}
