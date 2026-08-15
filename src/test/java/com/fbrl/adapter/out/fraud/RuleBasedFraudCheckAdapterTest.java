package com.fbrl.adapter.out.fraud;

import static org.assertj.core.api.Assertions.assertThat;

import com.fbrl.domain.model.FraudPolicy;
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
  @DisplayName("판정을 주입받은 FraudPolicy에 위임한다.")
  void isSuspicious_delegatesToFraudPolicy() {
    RuleBasedFraudCheckAdapter sut =
        new RuleBasedFraudCheckAdapter(new FraudPolicy(Money.wons(50_000_000)));

    assertThat(sut.isSuspicious("111-111", Money.wons(60_000_000))).isTrue();
    assertThat(sut.isSuspicious("111-111", Money.wons(49_999_999))).isFalse();
  }
}
