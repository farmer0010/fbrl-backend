package com.fbrl.application.port.out;

import com.fbrl.domain.model.EodSnapshot;
import java.util.Optional;

public interface LoadLatestEodSnapshotPort {
  Optional<EodSnapshot> findLatestByAccountNumber(String accountNumber);
}
