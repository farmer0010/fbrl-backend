package com.fbrl.application.port.out;

import com.fbrl.domain.model.LedgerBalanceDelta;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface LoadLedgerBalanceDeltasPort {
  Map<String, LedgerBalanceDelta> loadBalanceDeltasUntil(
      List<String> accountNumbers, Instant until);
}
