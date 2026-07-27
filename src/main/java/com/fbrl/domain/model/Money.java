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

  private void validate(BigDecimal amount) {
    if (amount == null) {
      throw new InvalidMoneyException("금액은 null일 수 없습니다.");
    }
    if (amount.compareTo(BigDecimal.ZERO) < 0) {
      throw new InvalidMoneyException("금액은 0원 이상이어야 합니다. 입력값: " + amount);
    }
  }

  public Money add(Money money) {
    Objects.requireNonNull(money, "가산할 금액 객체는 null 일 수 없습니다.");
    return new Money(amount.add(money.amount));
  }

  public Money subtract(Money money) {
    Objects.requireNonNull(money, "차감할 금액 객체는 null일 수 없습니다.");
    return new Money(amount.subtract(money.amount));
  }

  public boolean isGreaterThanOrEqual(Money money) {
    Objects.requireNonNull(money, "비교할 금액 객체는 null 일 수 없습니다");
    return this.amount.compareTo(money.amount) >= 0;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Money money = (Money) o;
    return amount != null && money.amount != null && amount.compareTo(money.amount) == 0;
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
