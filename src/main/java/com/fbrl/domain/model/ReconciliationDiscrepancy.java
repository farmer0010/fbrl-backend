package com.fbrl.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public record ReconciliationDiscrepancy(
    Long id,
    String accountNumber,
    LocalDate settlementDate,
    Money expectedBalance,
    Money actualBalance,
    ReconciliationStatus status,
    Instant computedAt) {

  public ReconciliationDiscrepancy {
    Objects.requireNonNull(accountNumber, "계좌번호는 null일 수 없습니다.");
    Objects.requireNonNull(settlementDate, "정산 기준일자는 null일 수 없습니다.");
    Objects.requireNonNull(status, "상태는 null일 수 없습니다.");
    Objects.requireNonNull(computedAt, "계산 시각은 null일 수 없습니다.");
  }

  public static ReconciliationDiscrepancy noSnapshot(
      String accountNumber, LocalDate settlementDate) {
    return new ReconciliationDiscrepancy(
        null,
        accountNumber,
        settlementDate,
        null,
        null,
        ReconciliationStatus.NO_SNAPSHOT,
        Instant.now());
  }

  public static ReconciliationDiscrepancy mismatch(
      String accountNumber, LocalDate settlementDate, Money expectedBalance, Money actualBalance) {
    return new ReconciliationDiscrepancy(
        null,
        accountNumber,
        settlementDate,
        expectedBalance,
        actualBalance,
        ReconciliationStatus.MISMATCH,
        Instant.now());
  }
}
