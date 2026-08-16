package com.fbrl.application.port.out;

import com.fbrl.domain.model.ReconciliationDiscrepancy;
import java.util.List;

public interface SaveReconciliationDiscrepancyPort {
  void saveAll(List<ReconciliationDiscrepancy> discrepancies);
}
