package com.fbrl.application.port.in;

import com.fbrl.domain.model.Money;
import com.fbrl.domain.model.SagaStatus;
import java.util.Objects;

public interface StartTransferSagaUseCase {
  TransferSagaResult startTransfer(StartTransferSagaCommand command);

  record StartTransferSagaCommand(String fromAccountNumber, String toAccountNumber, Money amount) {
    public StartTransferSagaCommand {
      Objects.requireNonNull(fromAccountNumber, "출금 계좌번호는 필수입니다.");
      Objects.requireNonNull(toAccountNumber, "입금 계좌번호는 필수입니다.");
      Objects.requireNonNull(amount, "이체 금액은 필수입니다.");
    }
  }

  record TransferSagaResult(String sagaId, SagaStatus finalStatus) {}
}
