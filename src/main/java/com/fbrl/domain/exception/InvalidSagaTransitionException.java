package com.fbrl.domain.exception;

import com.fbrl.domain.model.SagaStatus;

public class InvalidSagaTransitionException extends RuntimeException {
  public InvalidSagaTransitionException(SagaStatus from, SagaStatus to) {
    super("Saga 상태를 %s에서 %s(으)로 전이할 수 없습니다.".formatted(from, to));
  }
}
