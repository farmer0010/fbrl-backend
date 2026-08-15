package com.fbrl.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("FraudPolicy 도메인 정책 객체 단위 테스트")
class FraudPolicyTest {

  private final FraudPolicy policy = new FraudPolicy(Money.wons(50_000_000));

  @Test
  @DisplayName("threshold보다 큰 금액은 이상거래로 판정한다.")
  void isSuspicious_aboveThreshold_true() {
    assertThat(policy.isSuspicious(Money.wons(50_000_001))).isTrue();
  }

  @Test
  @DisplayName("threshold와 같은 금액도 이상거래로 판정한다.")
  void isSuspicious_equalToThreshold_true() {
    assertThat(policy.isSuspicious(Money.wons(50_000_000))).isTrue();
  }

  @Test
  @DisplayName("threshold보다 작은 금액은 이상거래로 판정하지 않는다.")
  void isSuspicious_belowThreshold_false() {
    assertThat(policy.isSuspicious(Money.wons(49_999_999))).isFalse();
  }
}
