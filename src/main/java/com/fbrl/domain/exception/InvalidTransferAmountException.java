package com.fbrl.domain.exception;

import java.math.BigDecimal;

public class InvalidTransferAmountException extends RuntimeException {
  public InvalidTransferAmountException(BigDecimal attemptedAmount) {
    super("이체 금액은 0원보다 커야 합니다. 요청된 금액: " + attemptedAmount);
  }
}
