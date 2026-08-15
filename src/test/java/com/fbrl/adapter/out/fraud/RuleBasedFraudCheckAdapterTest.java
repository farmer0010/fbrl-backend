package com.fbrl.adapter.out.fraud;

import static org.assertj.core.api.Assertions.assertThat;

import com.fbrl.domain.model.Money;
import com.fbrl.global.config.FraudPolicyProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@DisplayName("RuleBasedFraudCheckAdapter 단위 테스트")
class RuleBasedFraudCheckAdapterTest {

  @Autowired private FraudPolicyProperties fraudPolicyProperties;

  @Test
  @DisplayName("application.yaml의 fraud.threshold 값이 FraudPolicyProperties에 실제로 바인딩된다.")
  void fraudPolicyProperties_bindsThresholdFromYaml() {
    assertThat(fraudPolicyProperties.threshold()).isEqualByComparingTo("50000000");
  }

  @Test
  @DisplayName("임계치 이상 금액은 이상거래로 판정한다.")
  void isSuspicious_amountAtOrAboveThreshold_returnsTrue() {
    RuleBasedFraudCheckAdapter sut = new RuleBasedFraudCheckAdapter(Money.wons(50_000_000));

    assertThat(sut.isSuspicious("111-111", Money.wons(50_000_000))).isTrue();
    assertThat(sut.isSuspicious("111-111", Money.wons(60_000_000))).isTrue();
  }

  @Test
  @DisplayName("임계치 미만 금액은 이상거래로 판정하지 않는다.")
  void isSuspicious_amountBelowThreshold_returnsFalse() {
    RuleBasedFraudCheckAdapter sut = new RuleBasedFraudCheckAdapter(Money.wons(50_000_000));

    assertThat(sut.isSuspicious("111-111", Money.wons(49_999_999))).isFalse();
  }
}
