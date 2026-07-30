package com.fbrl.application.port.out;

import java.time.Duration;

public interface IdempotencyRepositoryPort {
  boolean setIfAbsent(String key, String value, Duration ttl);

  void setValue(String key, String value, Duration ttl);

  String getValue(String key);
}
