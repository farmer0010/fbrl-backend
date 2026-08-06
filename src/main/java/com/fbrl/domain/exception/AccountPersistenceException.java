package com.fbrl.domain.exception;

public class AccountPersistenceException extends RuntimeException {

  public AccountPersistenceException(String message, Throwable cause) {
    super(message, cause);
  }
}
