package com.fbrl.domain.exception;

import com.fbrl.domain.model.Money;

public class SuspiciousTransferException extends RuntimeException {
  public SuspiciousTransferException(String accountNumber, Money amount) {
    super("이상거래로 의심되어 이체가 차단되었습니다. 계좌번호: " + accountNumber + ", 금액: " + amount);
  }
}
