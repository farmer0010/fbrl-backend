package com.fbrl.adapter.out.persistence.demo;

import com.fbrl.domain.model.ApprovalStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface DemoApprovalRequestJpaRepository extends JpaRepository<DemoApprovalRequestEntity, Long> {
  Optional<DemoApprovalRequestEntity> findByRequestId(String requestId);

  List<DemoApprovalRequestEntity> findByStatus(ApprovalStatus status);

  @Query(
      value =
          "select t from DemoApprovalRequestEntity t "
              + "where (:status is null or t.status = :status) "
              + "and t.requestedAt >= :from and t.requestedAt <= :to",
      countQuery =
          "select count(t) from DemoApprovalRequestEntity t "
              + "where (:status is null or t.status = :status) "
              + "and t.requestedAt >= :from and t.requestedAt <= :to")
  Page<DemoApprovalRequestEntity> search(
      @Param("status") ApprovalStatus status,
      @Param("from") Instant from,
      @Param("to") Instant to,
      Pageable pageable);
}
