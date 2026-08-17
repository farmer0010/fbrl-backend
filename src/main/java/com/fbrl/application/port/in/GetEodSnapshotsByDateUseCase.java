package com.fbrl.application.port.in;

import com.fbrl.application.port.out.PagedResult;
import com.fbrl.domain.model.EodSnapshot;
import java.time.LocalDate;
import java.util.Objects;

public interface GetEodSnapshotsByDateUseCase {
  PagedResult<EodSnapshot> getByDate(GetEodSnapshotsByDateQuery query);

  record GetEodSnapshotsByDateQuery(LocalDate date, int page, int size) {
    public GetEodSnapshotsByDateQuery {
      Objects.requireNonNull(date, "조회 날짜는 필수입니다.");
    }
  }
}
