package com.fbrl.adapter.out.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface AdminUserJpaRepository extends JpaRepository<AdminUserJpaEntity, Long> {
  Optional<AdminUserJpaEntity> findByUsername(String username);
}
