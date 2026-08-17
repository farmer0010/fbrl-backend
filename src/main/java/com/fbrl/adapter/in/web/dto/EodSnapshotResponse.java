package com.fbrl.adapter.in.web.dto;

import com.fbrl.domain.model.EodSnapshot;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record EodSnapshotResponse(
    Long id,
    String accountNumber,
    BigDecimal closingBalance,
    BigDecimal interestAmount,
    LocalDate settlementDate,
    Instant computedAt) {
  public static EodSnapshotResponse from(EodSnapshot snapshot) {
    return new EodSnapshotResponse(
        snapshot.id(),
        snapshot.accountNumber(),
        snapshot.closingBalance().getAmount(),
        snapshot.interestAmount().getAmount(),
        snapshot.settlementDate(),
        snapshot.computedAt());
  }
}
