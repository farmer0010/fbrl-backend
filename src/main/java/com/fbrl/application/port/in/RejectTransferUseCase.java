package com.fbrl.application.port.in;

import com.fbrl.domain.model.ApprovalStatus;
import java.util.Objects;

public interface RejectTransferUseCase {
  ApprovalDecisionResult reject(RejectTransferCommand command);

  record RejectTransferCommand(String requestId, String checkerId, String rejectionReason) {
    public RejectTransferCommand {
      Objects.requireNonNull(requestId, "승인 요청 ID는 필수입니다.");
      Objects.requireNonNull(checkerId, "승인자 ID는 필수입니다.");
    }
  }

  record ApprovalDecisionResult(String requestId, ApprovalStatus status) {}
}
