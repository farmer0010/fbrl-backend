package com.fbrl.adapter.in.web.dto;

import com.fbrl.domain.model.LedgerDirection;
import com.fbrl.domain.model.LedgerEntry;
import java.math.BigDecimal;
import java.time.Instant;

public record LedgerEntryResponse(
    Long id,
    String accountNumber,
    LedgerDirection direction,
    BigDecimal amount,
    String transactionId,
    Instant occurredAt) {
  public static LedgerEntryResponse from(LedgerEntry ledgerEntry) {
    return new LedgerEntryResponse(
        ledgerEntry.id(),
        ledgerEntry.accountNumber(),
        ledgerEntry.direction(),
        ledgerEntry.amount().getAmount(),
        ledgerEntry.transactionId(),
        ledgerEntry.occurredAt());
  }
}
