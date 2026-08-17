package com.fbrl.adapter.in.web.dto;

import com.fbrl.application.port.in.RequestTransferApprovalUseCase.RequestTransferApprovalCommand;
import com.fbrl.domain.model.Money;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record RequestTransferApprovalRequest(
    @NotBlank(message = "출금 계좌번호는 필수입니다.") String fromAccountNumber,
    @NotBlank(message = "입금 계좌번호는 필수입니다.") String toAccountNumber,
    @NotNull(message = "이체 금액은 필수입니다.") @Positive(message = "이체 금액은 0원보다 커야 합니다.")
        BigDecimal amount) {
  public RequestTransferApprovalCommand toCommand(String makerId) {
    return new RequestTransferApprovalCommand(
        makerId, fromAccountNumber, toAccountNumber, Money.of(amount));
  }
}
