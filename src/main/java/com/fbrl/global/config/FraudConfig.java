package com.fbrl.global.config;

import com.fbrl.domain.model.Money;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FraudConfig {

  @Bean
  public Money fraudThreshold(FraudPolicyProperties fraudPolicyProperties) {
    return Money.of(fraudPolicyProperties.threshold());
  }
}
