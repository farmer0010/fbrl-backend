package com.fbrl.application.port.in;

import com.fbrl.application.port.out.PagedResult;
import com.fbrl.domain.model.ReconciliationDiscrepancy;
import com.fbrl.domain.model.ReconciliationStatus;
import java.time.LocalDate;
import java.util.Objects;

public interface SearchReconciliationDiscrepanciesUseCase {
  PagedResult<ReconciliationDiscrepancy> search(SearchReconciliationDiscrepanciesQuery query);

  record SearchReconciliationDiscrepanciesQuery(
      ReconciliationStatus status, LocalDate from, LocalDate to, int page, int size) {
    public SearchReconciliationDiscrepanciesQuery {
      Objects.requireNonNull(from, "조회 시작일자는 필수입니다.");
      Objects.requireNonNull(to, "조회 종료일자는 필수입니다.");
    }
  }
}
