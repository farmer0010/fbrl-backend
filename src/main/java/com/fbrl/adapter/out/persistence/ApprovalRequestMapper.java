package com.fbrl.adapter.out.persistence;

import com.fbrl.domain.model.Money;
import com.fbrl.domain.model.TransferApprovalRequest;
import org.springframework.stereotype.Component;

@Component
public class ApprovalRequestMapper {

  public TransferApprovalRequest toDomain(TransferApprovalRequestJpaEntity entity) {
    if (entity == null) {
      return null;
    }
    return TransferApprovalRequest.reconstruct(
        entity.getId(),
        entity.getRequestId(),
        entity.getMakerId(),
        entity.getCheckerId(),
        entity.getFromAccountNumber(),
        entity.getToAccountNumber(),
        Money.of(entity.getAmount()),
        entity.getStatus(),
        entity.getRejectionReason(),
        entity.getExecutionStatus(),
        entity.getExecutionFailureReason(),
        entity.getRequestedAt(),
        entity.getDecidedAt(),
        entity.getVersion());
  }

  public TransferApprovalRequestJpaEntity toEntity(TransferApprovalRequest domain) {
    if (domain == null) {
      return null;
    }
    return new TransferApprovalRequestJpaEntity(
        domain.getId(),
        domain.getRequestId(),
        domain.getMakerId(),
        domain.getCheckerId(),
        domain.getFromAccountNumber(),
        domain.getToAccountNumber(),
        domain.getAmount().getAmount(),
        domain.getStatus(),
        domain.getRejectionReason(),
        domain.getExecutionStatus(),
        domain.getExecutionFailureReason(),
        domain.getRequestedAt(),
        domain.getDecidedAt(),
        domain.getVersion());
  }
}
