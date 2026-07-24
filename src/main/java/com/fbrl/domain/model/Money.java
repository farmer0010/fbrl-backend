package com.fbrl.domain.model;

import com.fbrl.domain.exception.InvalidMoneyException;
import java.math.BigDecimal;
import java.util.Objects;

public final class Money {
  public static final Money ZERO = Money.wons(0);
  private final BigDecimal amount;

  private Money(BigDecimal amount) {
    validate(amount);
    this.amount = amount;
  }

  public static Money wons(long amount) {
    return new Money(BigDecimal.valueOf(amount));
  }

  public static Money of(BigDecimal amount) {
    return new Money(amount);
  }

  // 검증 로직
  private void validate(BigDecimal amount) {
    if (amount == null) {
      throw new InvalidMoneyException("금액은 null일 수 없습니다.");
    }
    if (amount.compareTo(BigDecimal.ZERO) < 0) {
      throw new InvalidMoneyException("금액은 0원 이상이어야 합니다. 입력값: " + amount);
    }
  }

  // 덧셈 연산 (불변 객체이므로 새로운 Money 반환)

  public Money add(Money money) {
    Objects.requireNonNull(money, "가산할 금액 객체는 null 일 수 없습니다.");
    return new Money(amount.add(money.amount));
  }

  // 뺄셈 연산 (잔액 부족 여부는 뺼셈 내부 vaildate에서 음수가 될떄 예외 발생)
  public Money subtract(Money money) {
    Objects.requireNonNull(money, "차감할 금액 객체는 null일 수 없습니다.");
    return new Money(amount.subtract(money.amount));
  }

  // 금액 비교
  public boolean isGreaterThanOrEqual(Money money) {
    Objects.requireNonNull(money, "비교할 금액 객체는 null 일 수 없습니다");
    return this.amount.compareTo(money.amount) >= 0;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  // vo핵심: 동등성 재정의 (주소값이 달라도 금액이 같으면 같은 객체로 취급)
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Money money = (Money) o;
    return Objects.equals(amount, money.amount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(amount);
  }

  @Override
  public String toString() {
    return amount.toPlainString() + " KRW";
  }
}
