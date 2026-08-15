package com.fbrl.domain.model;

import java.util.Objects;

public record ApprovalPolicy(Money threshold) {
  public ApprovalPolicy {
    Objects.requireNonNull(threshold, "승인 임계 금액은 필수입니다.");
  }

  public boolean requiresApproval(Money amount) {
    return amount.isGreaterThanOrEqual(threshold);
  }
}
