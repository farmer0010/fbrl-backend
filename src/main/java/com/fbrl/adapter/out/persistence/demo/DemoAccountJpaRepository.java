package com.fbrl.adapter.out.persistence.demo;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface DemoAccountJpaRepository extends JpaRepository<DemoAccountEntity, Long> {

  Optional<DemoAccountEntity> findByAccountNumber(String accountNumber);
}
