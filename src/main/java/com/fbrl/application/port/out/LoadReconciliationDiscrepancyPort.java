package com.fbrl.application.port.out;

import com.fbrl.domain.model.ReconciliationDiscrepancy;
import com.fbrl.domain.model.ReconciliationStatus;
import java.time.LocalDate;

public interface LoadReconciliationDiscrepancyPort {
  PagedResult<ReconciliationDiscrepancy> search(
      ReconciliationStatus status, LocalDate from, LocalDate to, int page, int size);
}
