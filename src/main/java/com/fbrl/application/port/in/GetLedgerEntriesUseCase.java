package com.fbrl.application.port.in;

import com.fbrl.application.port.out.PagedResult;
import com.fbrl.domain.model.LedgerEntry;
import java.time.Instant;
import java.util.Objects;

public interface GetLedgerEntriesUseCase {
  PagedResult<LedgerEntry> getLedgerEntries(GetLedgerEntriesQuery query);

  record GetLedgerEntriesQuery(String accountNumber, Instant from, Instant to, int page, int size) {
    public GetLedgerEntriesQuery {
      Objects.requireNonNull(accountNumber, "계좌번호는 필수입니다.");
      Objects.requireNonNull(from, "조회 시작 시각은 필수입니다.");
      Objects.requireNonNull(to, "조회 종료 시각은 필수입니다.");
    }
  }
}
