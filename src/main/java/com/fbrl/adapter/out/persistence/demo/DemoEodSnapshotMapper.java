package com.fbrl.adapter.out.persistence.demo;

import com.fbrl.domain.model.EodSnapshot;
import com.fbrl.domain.model.Money;
import org.springframework.stereotype.Component;

@Component
public class DemoEodSnapshotMapper {
  public EodSnapshot toDomain(DemoEodSnapshotEntity entity) {
    if (entity == null) {
      return null;
    }
    return new EodSnapshot(
        entity.getId(),
        entity.getAccountNumber(),
        Money.of(entity.getClosingBalance()),
        Money.of(entity.getInterestAmount()),
        entity.getSettlementDate(),
        entity.getComputedAt());
  }

  public DemoEodSnapshotEntity toEntity(EodSnapshot snapshot) {
    if (snapshot == null) {
      return null;
    }
    return new DemoEodSnapshotEntity(
        snapshot.id(),
        snapshot.accountNumber(),
        snapshot.closingBalance().getAmount(),
        snapshot.interestAmount().getAmount(),
        snapshot.settlementDate(),
        snapshot.computedAt());
  }
}
