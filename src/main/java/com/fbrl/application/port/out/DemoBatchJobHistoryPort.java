package com.fbrl.application.port.out;

import java.util.List;

public interface DemoBatchJobHistoryPort {
  void deleteByJobNames(List<String> jobNames);
}
