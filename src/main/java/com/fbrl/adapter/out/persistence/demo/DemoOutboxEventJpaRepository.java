package com.fbrl.adapter.out.persistence.demo;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface DemoOutboxEventJpaRepository extends JpaRepository<DemoOutboxEventEntity, Long> {
  List<DemoOutboxEventEntity> findAllByOrderByIdAsc();

  Page<DemoOutboxEventEntity> findAllByOrderByIdAsc(Pageable pageable);

  @Modifying
  @Query("update DemoOutboxEventEntity e set e.payload = :payload where e.id = :id")
  void tamperPayload(@Param("id") Long id, @Param("payload") String payload);
}
