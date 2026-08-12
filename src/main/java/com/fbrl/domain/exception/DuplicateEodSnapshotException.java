package com.fbrl.domain.exception;

public class DuplicateEodSnapshotException extends RuntimeException {
  public DuplicateEodSnapshotException(String message) {
    super(message);
  }
}
