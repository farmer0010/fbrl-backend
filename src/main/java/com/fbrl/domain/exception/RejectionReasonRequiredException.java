package com.fbrl.domain.exception;

public class RejectionReasonRequiredException extends RuntimeException {
  public RejectionReasonRequiredException() {
    super("거절 사유는 필수입니다.");
  }
}
