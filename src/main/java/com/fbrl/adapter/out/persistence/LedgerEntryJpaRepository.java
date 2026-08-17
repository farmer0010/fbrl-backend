package com.fbrl.adapter.out.persistence;

import com.fbrl.domain.model.LedgerDirection;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface LedgerEntryJpaRepository extends JpaRepository<LedgerEntryJpaEntity, Long> {

  List<LedgerEntryJpaEntity> findByAccountNumberAndOccurredAtGreaterThanEqual(
      String accountNumber, Instant since);

  Page<LedgerEntryJpaEntity> findByAccountNumberAndOccurredAtBetween(
      String accountNumber, Instant from, Instant to, Pageable pageable);

  @Query(
      "select coalesce(sum(l.amount), 0) from LedgerEntryJpaEntity l where l.direction = :direction")
  BigDecimal sumAmountByDirection(@Param("direction") LedgerDirection direction);

  @Query(
      "select new com.fbrl.adapter.out.persistence.LedgerBalanceDeltaProjection("
          + "l.accountNumber, "
          + "coalesce(sum(case when l.direction = :creditDirection then l.amount else 0 end), 0), "
          + "coalesce(sum(case when l.direction = :debitDirection then l.amount else 0 end), 0)) "
          + "from LedgerEntryJpaEntity l "
          + "where l.accountNumber in :accountNumbers and l.occurredAt <= :until "
          + "group by l.accountNumber")
  List<LedgerBalanceDeltaProjection> sumBalanceDeltasUntil(
      @Param("accountNumbers") List<String> accountNumbers,
      @Param("until") Instant until,
      @Param("creditDirection") LedgerDirection creditDirection,
      @Param("debitDirection") LedgerDirection debitDirection);
}
