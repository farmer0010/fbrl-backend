package com.fbrl.application.port.out;

import com.fbrl.domain.model.Money;

public interface DepositParticipantPort {

  DepositResult deposit(String sagaId, String accountNumber, Money amount);

  record DepositResult(boolean success, String failureReason) {}
}
