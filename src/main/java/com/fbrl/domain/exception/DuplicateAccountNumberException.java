package com.fbrl.domain.exception;

public class DuplicateAccountNumberException extends RuntimeException {
  public DuplicateAccountNumberException(String message) {
    super(message);
  }
}
