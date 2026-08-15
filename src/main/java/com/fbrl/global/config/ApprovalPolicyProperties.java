package com.fbrl.global.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "approval")
public record ApprovalPolicyProperties(BigDecimal threshold) {}
