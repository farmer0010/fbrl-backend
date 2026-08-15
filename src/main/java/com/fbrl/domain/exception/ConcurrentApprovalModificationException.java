package com.fbrl.domain.exception;

public class ConcurrentApprovalModificationException extends RuntimeException {

  public ConcurrentApprovalModificationException(String message, Throwable cause) {
    super(message, cause);
  }
}
