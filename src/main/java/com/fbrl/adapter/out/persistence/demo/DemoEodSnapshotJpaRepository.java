package com.fbrl.adapter.out.persistence.demo;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface DemoEodSnapshotJpaRepository extends JpaRepository<DemoEodSnapshotEntity, Long> {
  Optional<DemoEodSnapshotEntity> findTopByAccountNumberOrderByComputedAtDesc(String accountNumber);
}
