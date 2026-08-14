package com.fbrl.domain.exception;

public class LedgerPersistenceException extends RuntimeException {

  public LedgerPersistenceException(String message, Throwable cause) {
    super(message, cause);
  }
}
