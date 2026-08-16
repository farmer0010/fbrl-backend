package com.fbrl.domain.model;

import java.util.Objects;

public record LedgerBalanceDelta(Money creditTotal, Money debitTotal) {

  public LedgerBalanceDelta {
    Objects.requireNonNull(creditTotal, "대변 합계는 null일 수 없습니다.");
    Objects.requireNonNull(debitTotal, "차변 합계는 null일 수 없습니다.");
  }
}
