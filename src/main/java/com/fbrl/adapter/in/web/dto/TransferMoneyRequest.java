package com.fbrl.adapter.in.web.dto;

import com.fbrl.application.port.in.TransferMoneyCommand;
import com.fbrl.domain.model.Money;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record TransferMoneyRequest(
    @NotBlank(message = "출금 계좌번호는 필수입니다.") String senderAccountNumber,
    @NotBlank(message = "입금 계좌번호는 필수입니다.") String receiverAccountNumber,
    @NotNull(message = "송금 금액은 필수입니다.") @Positive(message = "송금 금액은 0원보다 커야 합니다.")
        BigDecimal amount) {
  public TransferMoneyCommand toCommand() {
    return new TransferMoneyCommand(senderAccountNumber, receiverAccountNumber, Money.of(amount));
  }
}
