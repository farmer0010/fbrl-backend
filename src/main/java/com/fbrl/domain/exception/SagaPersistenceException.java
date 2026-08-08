package com.fbrl.domain.exception;

public class SagaPersistenceException extends RuntimeException {

  public SagaPersistenceException(String message, Throwable cause) {
    super(message, cause);
  }
}
