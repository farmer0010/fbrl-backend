package com.fbrl.domain.model;

import java.util.Objects;

public record FraudPolicy(Money threshold) {
  public FraudPolicy {
    Objects.requireNonNull(threshold, "이상거래 임계 금액은 필수입니다.");
  }

  public boolean isSuspicious(Money amount) {
    return amount.isGreaterThanOrEqual(threshold);
  }
}
