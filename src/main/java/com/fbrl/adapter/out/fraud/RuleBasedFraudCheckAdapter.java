package com.fbrl.adapter.out.fraud;

import com.fbrl.application.port.out.FraudCheckPort;
import com.fbrl.domain.model.FraudPolicy;
import com.fbrl.domain.model.Money;
import org.springframework.stereotype.Component;

@Component
public class RuleBasedFraudCheckAdapter implements FraudCheckPort {

  private final FraudPolicy fraudPolicy;

  public RuleBasedFraudCheckAdapter(FraudPolicy fraudPolicy) {
    this.fraudPolicy = fraudPolicy;
  }

  @Override
  public boolean isSuspicious(String accountNumber, Money amount) {
    return fraudPolicy.isSuspicious(amount);
  }
}
