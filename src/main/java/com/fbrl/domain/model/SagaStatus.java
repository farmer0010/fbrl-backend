package com.fbrl.domain.model;

public enum SagaStatus {
  STARTED,
  WITHDRAWAL_COMPLETED,
  COMPLETED,
  COMPENSATING,
  COMPENSATED,
  FAILED;

  public boolean canTransitionTo(SagaStatus target) {
    return switch (this) {
      case STARTED -> target == WITHDRAWAL_COMPLETED || target == FAILED;
      case WITHDRAWAL_COMPLETED -> target == COMPLETED || target == COMPENSATING;
      case COMPENSATING -> target == COMPENSATED || target == FAILED;
      case COMPLETED, COMPENSATED, FAILED -> false;
    };
  }

  public boolean isTerminal() {
    return switch (this) {
      case COMPLETED, COMPENSATED, FAILED -> true;
      case STARTED, WITHDRAWAL_COMPLETED, COMPENSATING -> false;
    };
  }
}
