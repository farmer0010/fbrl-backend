package com.fbrl.application.service;

import com.fbrl.application.port.in.GetEodSnapshotHistoryUseCase;
import com.fbrl.application.port.out.LoadEodSnapshotHistoryPort;
import com.fbrl.application.port.out.PagedResult;
import com.fbrl.domain.model.EodSnapshot;
import org.springframework.stereotype.Service;

@Service
public class GetEodSnapshotHistoryService implements GetEodSnapshotHistoryUseCase {

  private final LoadEodSnapshotHistoryPort loadEodSnapshotHistoryPort;

  public GetEodSnapshotHistoryService(LoadEodSnapshotHistoryPort loadEodSnapshotHistoryPort) {
    this.loadEodSnapshotHistoryPort = loadEodSnapshotHistoryPort;
  }

  @Override
  public PagedResult<EodSnapshot> getHistory(GetEodSnapshotHistoryQuery query) {
    return loadEodSnapshotHistoryPort.byAccountNumber(
        query.accountNumber(), query.from(), query.to(), query.page(), query.size());
  }
}
