package com.fbrl.adapter.out.persistence;

import com.fbrl.domain.model.LedgerDirection;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface LedgerEntryJpaRepository extends JpaRepository<LedgerEntryJpaEntity, Long> {

  List<LedgerEntryJpaEntity> findByAccountNumberAndOccurredAtGreaterThanEqual(
      String accountNumber, Instant since);

  @Query(
      "select coalesce(sum(l.amount), 0) from LedgerEntryJpaEntity l where l.direction = :direction")
  BigDecimal sumAmountByDirection(@Param("direction") LedgerDirection direction);
}
