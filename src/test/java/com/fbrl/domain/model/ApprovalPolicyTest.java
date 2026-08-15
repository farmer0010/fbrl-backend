package com.fbrl.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ApprovalPolicy 도메인 정책 객체 단위 테스트")
class ApprovalPolicyTest {

  private final ApprovalPolicy policy = new ApprovalPolicy(Money.wons(10_000_000));

  @Test
  @DisplayName("threshold보다 큰 금액은 승인이 필요하다.")
  void requiresApproval_aboveThreshold_true() {
    assertThat(policy.requiresApproval(Money.wons(10_000_001))).isTrue();
  }

  @Test
  @DisplayName("threshold와 같은 금액도 승인이 필요하다.")
  void requiresApproval_equalToThreshold_true() {
    assertThat(policy.requiresApproval(Money.wons(10_000_000))).isTrue();
  }

  @Test
  @DisplayName("threshold보다 작은 금액은 승인이 필요하지 않다.")
  void requiresApproval_belowThreshold_false() {
    assertThat(policy.requiresApproval(Money.wons(9_999_999))).isFalse();
  }
}
