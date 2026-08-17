package com.fbrl.adapter.out.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface EodSnapshotJpaRepository extends JpaRepository<EodSnapshotJpaEntity, Long> {
  Optional<EodSnapshotJpaEntity> findTopByAccountNumberOrderByComputedAtDesc(String accountNumber);

  List<EodSnapshotJpaEntity> findByAccountNumberInAndSettlementDate(
      List<String> accountNumbers, LocalDate settlementDate);

  @Query(
      value =
          "select e from EodSnapshotJpaEntity e "
              + "where e.accountNumber = :accountNumber "
              + "and (cast(:from as date) is null or e.settlementDate >= :from) "
              + "and (cast(:to as date) is null or e.settlementDate <= :to)",
      countQuery =
          "select count(e) from EodSnapshotJpaEntity e "
              + "where e.accountNumber = :accountNumber "
              + "and (cast(:from as date) is null or e.settlementDate >= :from) "
              + "and (cast(:to as date) is null or e.settlementDate <= :to)")
  Page<EodSnapshotJpaEntity> search(
      @Param("accountNumber") String accountNumber,
      @Param("from") LocalDate from,
      @Param("to") LocalDate to,
      Pageable pageable);

  Page<EodSnapshotJpaEntity> findBySettlementDate(LocalDate settlementDate, Pageable pageable);
}
