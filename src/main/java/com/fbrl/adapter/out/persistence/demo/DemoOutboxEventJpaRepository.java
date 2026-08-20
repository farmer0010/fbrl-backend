package com.fbrl.adapter.out.persistence.demo;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface DemoOutboxEventJpaRepository extends JpaRepository<DemoOutboxEventEntity, Long> {
  List<DemoOutboxEventEntity> findAllByOrderByIdAsc();

  Page<DemoOutboxEventEntity> findAllByOrderByIdAsc(Pageable pageable);
}
