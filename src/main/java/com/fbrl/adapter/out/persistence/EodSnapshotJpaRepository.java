package com.fbrl.adapter.out.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface EodSnapshotJpaRepository extends JpaRepository<EodSnapshotJpaEntity, Long> {
  Optional<EodSnapshotJpaEntity> findTopByAccountNumberOrderByComputedAtDesc(String accountNumber);
}
