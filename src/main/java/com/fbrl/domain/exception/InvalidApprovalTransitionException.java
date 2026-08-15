package com.fbrl.domain.exception;

import com.fbrl.domain.model.ApprovalStatus;

public class InvalidApprovalTransitionException extends RuntimeException {
  public InvalidApprovalTransitionException(ApprovalStatus from, ApprovalStatus to) {
    super("승인 요청 상태를 %s에서 %s(으)로 전이할 수 없습니다.".formatted(from, to));
  }
}
