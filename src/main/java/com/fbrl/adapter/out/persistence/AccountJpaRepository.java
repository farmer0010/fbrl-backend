package com.fbrl.adapter.out.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountJpaRepository extends JpaRepository<AccountEntity, Long> {
  Optional<AccountEntity> findByAccountNumber(String accountNumber);
}
