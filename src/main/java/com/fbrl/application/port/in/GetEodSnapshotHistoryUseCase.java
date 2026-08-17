package com.fbrl.application.port.in;

import com.fbrl.application.port.out.PagedResult;
import com.fbrl.domain.model.EodSnapshot;
import java.time.LocalDate;
import java.util.Objects;

public interface GetEodSnapshotHistoryUseCase {
  PagedResult<EodSnapshot> getHistory(GetEodSnapshotHistoryQuery query);

  record GetEodSnapshotHistoryQuery(
      String accountNumber, LocalDate from, LocalDate to, int page, int size) {
    public GetEodSnapshotHistoryQuery {
      Objects.requireNonNull(accountNumber, "계좌번호는 필수입니다.");
    }
  }
}
