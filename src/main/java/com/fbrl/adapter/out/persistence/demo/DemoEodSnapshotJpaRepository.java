package com.fbrl.adapter.out.persistence.demo;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface DemoEodSnapshotJpaRepository extends JpaRepository<DemoEodSnapshotEntity, Long> {
  Optional<DemoEodSnapshotEntity> findTopByAccountNumberOrderByComputedAtDesc(String accountNumber);

  List<DemoEodSnapshotEntity> findByAccountNumberInAndSettlementDate(
      List<String> accountNumbers, LocalDate settlementDate);
}
