package com.fbrl.adapter.out.persistence;

import com.fbrl.domain.model.OutboxEvent;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface OutboxEventJpaRepository extends JpaRepository<OutboxEventJpaEntity, Long> {
  List<OutboxEventJpaEntity> findByStatusOrderByCreatedAtAsc(
      OutboxEvent.Status status, Pageable pageable);
}
