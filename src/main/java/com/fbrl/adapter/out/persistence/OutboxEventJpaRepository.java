package com.fbrl.adapter.out.persistence;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface OutboxEventJpaRepository extends JpaRepository<OutboxEventJpaEntity, Long> {
  List<OutboxEventJpaEntity> findAllByOrderByIdAsc();

  Page<OutboxEventJpaEntity> findAllByOrderByIdAsc(Pageable pageable);
}
