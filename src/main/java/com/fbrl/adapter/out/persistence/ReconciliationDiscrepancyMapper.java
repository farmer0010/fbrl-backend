package com.fbrl.adapter.out.persistence;

import com.fbrl.domain.model.Money;
import com.fbrl.domain.model.ReconciliationDiscrepancy;
import org.springframework.stereotype.Component;

@Component
public class ReconciliationDiscrepancyMapper {

  public ReconciliationDiscrepancy toDomain(ReconciliationDiscrepancyJpaEntity entity) {
    if (entity == null) {
      return null;
    }
    return new ReconciliationDiscrepancy(
        entity.getId(),
        entity.getAccountNumber(),
        entity.getSettlementDate(),
        entity.getExpectedBalance() == null ? null : Money.of(entity.getExpectedBalance()),
        entity.getActualBalance() == null ? null : Money.of(entity.getActualBalance()),
        entity.getStatus(),
        entity.getComputedAt());
  }

  public ReconciliationDiscrepancyJpaEntity toEntity(ReconciliationDiscrepancy discrepancy) {
    if (discrepancy == null) {
      return null;
    }
    return new ReconciliationDiscrepancyJpaEntity(
        discrepancy.id(),
        discrepancy.accountNumber(),
        discrepancy.settlementDate(),
        discrepancy.expectedBalance() == null ? null : discrepancy.expectedBalance().getAmount(),
        discrepancy.actualBalance() == null ? null : discrepancy.actualBalance().getAmount(),
        discrepancy.status(),
        discrepancy.computedAt());
  }
}
