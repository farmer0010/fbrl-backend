package com.fbrl.application.service;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class AccountNumberPolicy {
  private static final SecureRandom RANDOM = new SecureRandom();

  public String generate() {
    return "%03d-%04d-%04d"
        .formatted(RANDOM.nextInt(1000), RANDOM.nextInt(10000), RANDOM.nextInt(10000));
  }
}
