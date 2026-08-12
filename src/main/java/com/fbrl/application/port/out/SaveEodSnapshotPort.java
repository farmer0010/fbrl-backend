package com.fbrl.application.port.out;

import com.fbrl.domain.model.EodSnapshot;
import java.util.List;

public interface SaveEodSnapshotPort {
  void saveAll(List<EodSnapshot> snapshots);
}
