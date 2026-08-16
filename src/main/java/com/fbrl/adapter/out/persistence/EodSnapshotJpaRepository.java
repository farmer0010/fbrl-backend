package com.fbrl.adapter.out.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface EodSnapshotJpaRepository extends JpaRepository<EodSnapshotJpaEntity, Long> {
  Optional<EodSnapshotJpaEntity> findTopByAccountNumberOrderByComputedAtDesc(String accountNumber);

  List<EodSnapshotJpaEntity> findByAccountNumberInAndSettlementDate(
      List<String> accountNumbers, LocalDate settlementDate);
}
