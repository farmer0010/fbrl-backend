package com.fbrl.domain.exception;

public class DuplicateAdminUsernameException extends RuntimeException {
  public DuplicateAdminUsernameException(String message) {
    super(message);
  }
}
