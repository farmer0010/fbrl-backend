package com.fbrl.adapter.in.web.dto;

import com.fbrl.domain.model.ApprovalStatus;
import com.fbrl.domain.model.TransferApprovalRequest;
import java.math.BigDecimal;
import java.time.Instant;

public record PendingApprovalResponse(
    String requestId,
    String makerId,
    String fromAccountNumber,
    String toAccountNumber,
    BigDecimal amount,
    ApprovalStatus status,
    Instant requestedAt) {
  public static PendingApprovalResponse from(TransferApprovalRequest request) {
    return new PendingApprovalResponse(
        request.getRequestId(),
        request.getMakerId(),
        request.getFromAccountNumber(),
        request.getToAccountNumber(),
        request.getAmount().getAmount(),
        request.getStatus(),
        request.getRequestedAt());
  }
}
