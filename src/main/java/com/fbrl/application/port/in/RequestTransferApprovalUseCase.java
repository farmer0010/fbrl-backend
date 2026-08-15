package com.fbrl.application.port.in;

import com.fbrl.domain.model.ApprovalStatus;
import com.fbrl.domain.model.Money;
import java.util.Objects;

public interface RequestTransferApprovalUseCase {
  ApprovalRequestResult requestApproval(RequestTransferApprovalCommand command);

  record RequestTransferApprovalCommand(
      String makerId, String fromAccountNumber, String toAccountNumber, Money amount) {
    public RequestTransferApprovalCommand {
      Objects.requireNonNull(makerId, "기안자 ID는 필수입니다.");
      Objects.requireNonNull(fromAccountNumber, "출금 계좌번호는 필수입니다.");
      Objects.requireNonNull(toAccountNumber, "입금 계좌번호는 필수입니다.");
      Objects.requireNonNull(amount, "이체 금액은 필수입니다.");
    }
  }

  record ApprovalRequestResult(String requestId, ApprovalStatus status) {}
}
