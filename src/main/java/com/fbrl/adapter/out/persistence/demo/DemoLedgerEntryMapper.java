package com.fbrl.adapter.out.persistence.demo;

import com.fbrl.domain.model.LedgerEntry;
import com.fbrl.domain.model.Money;
import org.springframework.stereotype.Component;

@Component
public class DemoLedgerEntryMapper {
  public LedgerEntry toDomain(DemoLedgerEntryEntity entity) {
    if (entity == null) {
      return null;
    }
    return new LedgerEntry(
        entity.getId(),
        entity.getAccountNumber(),
        entity.getDirection(),
        Money.of(entity.getAmount()),
        entity.getTransactionId(),
        entity.getOccurredAt());
  }

  public DemoLedgerEntryEntity toEntity(LedgerEntry ledgerEntry) {
    if (ledgerEntry == null) {
      return null;
    }
    return new DemoLedgerEntryEntity(
        ledgerEntry.id(),
        ledgerEntry.accountNumber(),
        ledgerEntry.direction(),
        ledgerEntry.amount().getAmount(),
        ledgerEntry.transactionId(),
        ledgerEntry.occurredAt());
  }
}
