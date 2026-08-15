package com.fbrl.adapter.out.persistence;

import com.fbrl.domain.model.ApprovalStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface TransferApprovalRequestJpaRepository
    extends JpaRepository<TransferApprovalRequestJpaEntity, Long> {
  Optional<TransferApprovalRequestJpaEntity> findByRequestId(String requestId);

  List<TransferApprovalRequestJpaEntity> findByStatus(ApprovalStatus status);
}
