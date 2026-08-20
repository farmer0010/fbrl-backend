package com.fbrl.adapter.out.persistence.demo;

import com.fbrl.domain.model.Money;
import com.fbrl.domain.model.ReconciliationDiscrepancy;
import org.springframework.stereotype.Component;

@Component
public class DemoReconciliationDiscrepancyMapper {

  public ReconciliationDiscrepancy toDomain(DemoReconciliationDiscrepancyEntity entity) {
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

  public DemoReconciliationDiscrepancyEntity toEntity(ReconciliationDiscrepancy discrepancy) {
    if (discrepancy == null) {
      return null;
    }
    return new DemoReconciliationDiscrepancyEntity(
        discrepancy.id(),
        discrepancy.accountNumber(),
        discrepancy.settlementDate(),
        discrepancy.expectedBalance() == null ? null : discrepancy.expectedBalance().getAmount(),
        discrepancy.actualBalance() == null ? null : discrepancy.actualBalance().getAmount(),
        discrepancy.status(),
        discrepancy.computedAt());
  }
}
