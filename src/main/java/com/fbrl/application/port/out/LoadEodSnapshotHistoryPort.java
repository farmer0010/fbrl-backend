package com.fbrl.application.port.out;

import com.fbrl.domain.model.EodSnapshot;
import java.time.LocalDate;

public interface LoadEodSnapshotHistoryPort {
  PagedResult<EodSnapshot> byAccountNumber(
      String accountNumber, LocalDate from, LocalDate to, int page, int size);

  PagedResult<EodSnapshot> byDate(LocalDate date, int page, int size);
}
