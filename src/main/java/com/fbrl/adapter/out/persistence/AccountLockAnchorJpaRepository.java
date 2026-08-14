package com.fbrl.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface AccountLockAnchorJpaRepository extends JpaRepository<AccountLockAnchorJpaEntity, Long> {

  Optional<AccountLockAnchorJpaEntity> findByAccountNumber(String accountNumber);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select a from AccountLockAnchorJpaEntity a where a.accountNumber = :accountNumber")
  Optional<AccountLockAnchorJpaEntity> findByAccountNumberWithPessimisticLock(
      @Param("accountNumber") String accountNumber);

  @Lock(LockModeType.OPTIMISTIC)
  @Query("select a from AccountLockAnchorJpaEntity a where a.accountNumber = :accountNumber")
  Optional<AccountLockAnchorJpaEntity> findByAccountNumberWithOptimisticLock(
      @Param("accountNumber") String accountNumber);
}
