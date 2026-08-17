package com.fbrl.application.port.in;

import com.fbrl.application.port.out.PagedResult;
import com.fbrl.domain.model.ApprovalStatus;
import com.fbrl.domain.model.TransferApprovalRequest;
import java.time.Instant;
import java.util.Objects;

public interface SearchTransferApprovalsUseCase {
  PagedResult<TransferApprovalRequest> search(SearchTransferApprovalsQuery query);

  record SearchTransferApprovalsQuery(
      ApprovalStatus status, Instant from, Instant to, int page, int size) {
    public SearchTransferApprovalsQuery {
      Objects.requireNonNull(from, "조회 시작 시각은 필수입니다.");
      Objects.requireNonNull(to, "조회 종료 시각은 필수입니다.");
    }
  }
}
