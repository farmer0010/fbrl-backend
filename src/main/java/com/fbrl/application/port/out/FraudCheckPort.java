package com.fbrl.application.port.out;

import com.fbrl.domain.model.Money;

public interface FraudCheckPort {

  boolean isSuspicious(String accountNumber, Money amount);
}
