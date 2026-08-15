package com.fbrl.global.config;

import com.fbrl.domain.model.ApprovalPolicy;
import com.fbrl.domain.model.Money;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApprovalConfig {

  @Bean
  public ApprovalPolicy approvalPolicy(ApprovalPolicyProperties approvalPolicyProperties) {
    return new ApprovalPolicy(Money.of(approvalPolicyProperties.threshold()));
  }
}
