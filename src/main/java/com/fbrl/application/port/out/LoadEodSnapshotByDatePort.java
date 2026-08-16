package com.fbrl.application.port.out;

import com.fbrl.domain.model.EodSnapshot;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface LoadEodSnapshotByDatePort {
  Map<String, EodSnapshot> loadByAccountNumbersAndDate(List<String> accountNumbers, LocalDate date);
}
