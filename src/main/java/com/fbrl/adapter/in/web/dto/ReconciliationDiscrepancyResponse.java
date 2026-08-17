package com.fbrl.adapter.in.web.dto;

import com.fbrl.domain.model.ReconciliationDiscrepancy;
import com.fbrl.domain.model.ReconciliationStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ReconciliationDiscrepancyResponse(
    Long id,
    String accountNumber,
    LocalDate settlementDate,
    BigDecimal expectedBalance,
    BigDecimal actualBalance,
    ReconciliationStatus status,
    Instant computedAt) {
  public static ReconciliationDiscrepancyResponse from(ReconciliationDiscrepancy discrepancy) {
    return new ReconciliationDiscrepancyResponse(
        discrepancy.id(),
        discrepancy.accountNumber(),
        discrepancy.settlementDate(),
        discrepancy.expectedBalance() == null ? null : discrepancy.expectedBalance().getAmount(),
        discrepancy.actualBalance() == null ? null : discrepancy.actualBalance().getAmount(),
        discrepancy.status(),
        discrepancy.computedAt());
  }
}
