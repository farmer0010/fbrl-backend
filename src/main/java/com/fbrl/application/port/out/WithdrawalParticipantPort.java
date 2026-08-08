package com.fbrl.application.port.out;

import com.fbrl.domain.model.Money;

public interface WithdrawalParticipantPort {

  WithdrawalResult withdraw(String sagaId, String accountNumber, Money amount);

  record WithdrawalResult(boolean success, String failureReason) {}
}
