package com.fbrl.application.port.in;

import com.fbrl.domain.model.Money;
import java.util.Objects;

public record TransferMoneyCommand(
    String senderAccountNumber, String receiverAccountNumber, Money money) {

  public TransferMoneyCommand {
    Objects.requireNonNull(senderAccountNumber, "출금 계좌 번호는 null일 수 없습니다");
    Objects.requireNonNull(receiverAccountNumber, "입금 계좌 번호는 null일 수 없습니다");
    Objects.requireNonNull(money, "송금 금액은 null일 수 없습니다");

    if (senderAccountNumber.equals(receiverAccountNumber)) {
      throw new IllegalArgumentException("출금 계좌와 입금 계좌는 동일 할 수 없습니다.");
    }
  }
}
