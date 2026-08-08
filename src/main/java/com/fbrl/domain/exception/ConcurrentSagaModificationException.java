package com.fbrl.domain.exception;

public class ConcurrentSagaModificationException extends RuntimeException {

  public ConcurrentSagaModificationException(String message, Throwable cause) {
    super(message, cause);
  }
}
