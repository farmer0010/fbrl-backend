package com.fbrl.adapter.out.fraud;

import com.fbrl.application.port.out.FraudCheckPort;
import com.fbrl.domain.model.Money;
import org.springframework.stereotype.Component;

@Component
public class RuleBasedFraudCheckAdapter implements FraudCheckPort {

  private final Money threshold;

  public RuleBasedFraudCheckAdapter(Money fraudThreshold) {
    this.threshold = fraudThreshold;
  }

  @Override
  public boolean isSuspicious(String accountNumber, Money amount) {
    return amount.isGreaterThanOrEqual(threshold);
  }
}
