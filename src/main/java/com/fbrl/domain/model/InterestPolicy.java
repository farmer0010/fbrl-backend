package com.fbrl.domain.model;

import com.fbrl.domain.exception.InvalidInterestRateException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record InterestPolicy(BigDecimal annualRate) {
  private static final int DAYS_IN_YEAR = 365;
  private static final int DAILY_RATE_SCALE = 10;
  private static final int WON_SCALE = 0;

  public InterestPolicy {
    Objects.requireNonNull(annualRate, "연 이자율은 null일 수 없습니다.");
    if (annualRate.compareTo(BigDecimal.ZERO) < 0) {
      throw new InvalidInterestRateException("연 이자율은 0 이상이어야 합니다. 입력값: " + annualRate);
    }
  }

  public static InterestPolicy ofAnnualRate(BigDecimal annualRate) {
    return new InterestPolicy(annualRate);
  }

  public Money calculateDailyInterest(Money balance) {
    Objects.requireNonNull(balance, "잔액은 null 일 수 없습니다.");

    BigDecimal dailyRate =
        annualRate.divide(BigDecimal.valueOf(DAYS_IN_YEAR), DAILY_RATE_SCALE, RoundingMode.HALF_UP);
    BigDecimal interestAmount =
        balance.getAmount().multiply(dailyRate).setScale(WON_SCALE, RoundingMode.HALF_UP);

    return Money.of(interestAmount);
  }
}
