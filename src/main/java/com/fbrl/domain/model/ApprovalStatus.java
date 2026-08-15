package com.fbrl.domain.model;

public enum ApprovalStatus {
  PENDING,
  APPROVED,
  REJECTED;

  public boolean canTransitionTo(ApprovalStatus target) {
    return switch (this) {
      case PENDING -> target == APPROVED || target == REJECTED;
      case APPROVED, REJECTED -> false;
    };
  }
}
