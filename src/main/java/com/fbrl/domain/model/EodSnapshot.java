package com.fbrl.domain.model;

import java.time.LocalDate;
import java.util.Objects;

public record EodSnapshot(
    Long id,
    String accountNumber,
    Money closingBalance,
    Money interestAmount,
    LocalDate settlementDate) {

  public EodSnapshot {
    Objects.requireNonNull(accountNumber, "계좌번호는 null일 수 없습니다.");
    Objects.requireNonNull(closingBalance, "마감 잔액은 null일 수 없습니다.");
    Objects.requireNonNull(interestAmount, "이자 금액은 null일 수 없습니다.");
    Objects.requireNonNull(settlementDate, "정산 기준일자는 null일 수 없습니다.");
  }

  public static EodSnapshot of(
      String accountNumber, Money closingBalance, Money interestAmount, LocalDate settlementDate) {
    return new EodSnapshot(null, accountNumber, closingBalance, interestAmount, settlementDate);
  }

  public Money totalBalance() {
    return closingBalance.add(interestAmount);
  }
}
