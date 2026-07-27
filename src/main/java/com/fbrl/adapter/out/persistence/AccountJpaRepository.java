package com.fbrl.adapter.out.persistence;

import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountJpaRepository extends JpaRepository<AccountEntity, Long> {

  Optional<AccountEntity> findByAccountNumber(String accountNumber);

  @Modifying
  @Query("UPDATE AccountEntity a SET a.balance = :balance WHERE a.accountNumber = :accountNumber")
  int updateBalanceByAccountNumber(
      @Param("accountNumber") String accountNumber, @Param("balance") BigDecimal balance);
}
