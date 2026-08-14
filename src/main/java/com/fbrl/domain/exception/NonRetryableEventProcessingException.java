package com.fbrl.domain.exception;

public abstract class NonRetryableEventProcessingException extends RuntimeException {

  protected NonRetryableEventProcessingException(String message, Throwable cause) {
    super(message, cause);
  }

  protected NonRetryableEventProcessingException(String message) {
    super(message);
  }
}
