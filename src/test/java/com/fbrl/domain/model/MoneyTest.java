package com.fbrl.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fbrl.domain.exception.InvalidMoneyException;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Money VO 단위 테스트")
class MoneyTest {

  @Nested
  @DisplayName("금액 생성 검증")
  class CreationTest {

    @Test
    @DisplayName("정수 원화 금액으로 Money 객체를 생성한다.")
    void createWithWons() {
      Money money = Money.wons(10000);

      assertThat(money.getAmount()).isEqualTo(BigDecimal.valueOf(10000));
    }

    @Test
    @DisplayName("금액이 음수인 경우 InvalidMoneyException이 발생한다.")
    void createWithNegativeAmountThrowsException() {
      assertThatThrownBy(() -> Money.wons(-1000))
          .isInstanceOf(InvalidMoneyException.class)
          .hasMessageContaining("금액은 0원 이상이어야 합니다");
    }

    @Test
    @DisplayName("금액이 null인 경우 InvalidMoneyException이 발생한다.")
    void createWithNullAmountThrowsException() {
      assertThatThrownBy(() -> Money.of(null))
          .isInstanceOf(InvalidMoneyException.class)
          .hasMessageContaining("금액은 null일 수 없습니다");
    }
  }

  @Nested
  @DisplayName("금액 연산 검증")
  class OperationTest {

    @Test
    @DisplayName("두 Money 객체를 더한 새로운 Money 객체를 반환한다.")
    void add() {
      Money money1 = Money.wons(5000);
      Money money2 = Money.wons(3000);

      Money result = money1.add(money2);

      assertThat(result).isEqualTo(Money.wons(8000));
      assertThat(money1).isEqualTo(Money.wons(5000));
    }

    @Test
    @DisplayName("금액을 차감한 새로운 Money 객체를 반환한다.")
    void subtract() {
      Money money1 = Money.wons(10000);
      Money money2 = Money.wons(4000);

      Money result = money1.subtract(money2);

      assertThat(result).isEqualTo(Money.wons(6000));
    }
  }
}
