package com.fbrl.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fbrl.domain.exception.InvalidInterestRateException;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("InterestPolicy 단위 테스트")
class InterestPolicyTest {

  @Nested
  @DisplayName("생성 검증")
  class CreationTest {

    @Test
    @DisplayName("0 이상의 이자율로 정상 생성된다.")
    void createWithValidRate() {
      InterestPolicy policy = InterestPolicy.ofAnnualRate(BigDecimal.valueOf(0.05));

      assertThat(policy.annualRate()).isEqualByComparingTo(BigDecimal.valueOf(0.05));
    }

    @Test
    @DisplayName("이자율이 null이면 NullPointerException이 발생한다.")
    void createWithNullRateThrowsException() {
      assertThatThrownBy(() -> InterestPolicy.ofAnnualRate(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("연 이자율은 null일 수 없습니다");
    }

    @Test
    @DisplayName("이자율이 음수이면 InvalidInterestRateException이 발생한다.")
    void createWithNegativeRateThrowsException() {
      assertThatThrownBy(() -> InterestPolicy.ofAnnualRate(BigDecimal.valueOf(-0.01)))
          .isInstanceOf(InvalidInterestRateException.class)
          .hasMessageContaining("연 이자율은 0 이상이어야 합니다");
    }
  }

  @Nested
  @DisplayName("일일 이자 계산 검증")
  class CalculationTest {

    @Test
    @DisplayName("연 3.65% 이자율, 잔액 100만원이면 일일 이자는 100원이다.")
    void calculateDailyInterestWithCleanRate() {
      InterestPolicy policy = InterestPolicy.ofAnnualRate(BigDecimal.valueOf(0.0365));

      Money interest = policy.calculateDailyInterest(Money.wons(1_000_000));

      assertThat(interest).isEqualTo(Money.wons(100));
    }

    @Test
    @DisplayName("순환소수가 나오는 이자율도 원 단위로 반올림되어 계산된다.")
    void calculateDailyInterestWithRounding() {
      InterestPolicy policy = InterestPolicy.ofAnnualRate(BigDecimal.valueOf(0.05));

      Money interest = policy.calculateDailyInterest(Money.wons(1_000_000));

      // 0.05 / 365 = 0.0001369863(scale 10) → 1,000,000 × 0.0001369863 = 136.9863 → HALF_UP 시 137원
      assertThat(interest).isEqualTo(Money.wons(137));
    }

    @Test
    @DisplayName("잔액이 0원이면 이자도 0원이다.")
    void calculateDailyInterestWithZeroBalance() {
      InterestPolicy policy = InterestPolicy.ofAnnualRate(BigDecimal.valueOf(0.05));

      Money interest = policy.calculateDailyInterest(Money.ZERO);

      assertThat(interest).isEqualTo(Money.ZERO);
    }

    @Test
    @DisplayName("이자율이 0이면 이자도 0원이다.")
    void calculateDailyInterestWithZeroRate() {
      InterestPolicy policy = InterestPolicy.ofAnnualRate(BigDecimal.ZERO);

      Money interest = policy.calculateDailyInterest(Money.wons(1_000_000));

      assertThat(interest).isEqualTo(Money.ZERO);
    }

    @Test
    @DisplayName("잔액이 null이면 NullPointerException이 발생한다.")
    void calculateDailyInterestWithNullBalanceThrowsException() {
      InterestPolicy policy = InterestPolicy.ofAnnualRate(BigDecimal.valueOf(0.05));

      assertThatThrownBy(() -> policy.calculateDailyInterest(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("잔액은 null 일 수 없습니다");
    }
  }
}
