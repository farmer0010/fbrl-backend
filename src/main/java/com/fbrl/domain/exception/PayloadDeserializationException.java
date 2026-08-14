package com.fbrl.domain.exception;

public class PayloadDeserializationException extends NonRetryableEventProcessingException {
  public PayloadDeserializationException(String message, Throwable cause) {
    super(message, cause);
  }
}
