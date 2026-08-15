package com.fbrl.global.config;

import com.fbrl.domain.model.FraudPolicy;
import com.fbrl.domain.model.Money;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FraudConfig {

  @Bean
  public FraudPolicy fraudPolicy(FraudPolicyProperties fraudPolicyProperties) {
    return new FraudPolicy(Money.of(fraudPolicyProperties.threshold()));
  }
}
