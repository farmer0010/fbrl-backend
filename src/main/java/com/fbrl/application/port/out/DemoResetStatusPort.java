package com.fbrl.application.port.out;

import java.time.Instant;
import java.util.Optional;

public interface DemoResetStatusPort {
  void recordResetCompleted(Instant completedAt);

  Optional<Instant> loadLastResetAt();
}
