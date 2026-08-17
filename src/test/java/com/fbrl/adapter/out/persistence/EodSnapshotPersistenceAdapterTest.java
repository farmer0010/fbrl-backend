package com.fbrl.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.fbrl.application.port.out.PagedResult;
import com.fbrl.domain.model.EodSnapshot;
import com.fbrl.domain.model.Money;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@DisplayName("EodSnapshotPersistenceAdapter 계좌별/날짜별 이력 조회 테스트")
class EodSnapshotPersistenceAdapterTest {

  @Autowired private EodSnapshotPersistenceAdapter eodSnapshotPersistenceAdapter;

  @BeforeEach
  void setUp() {
    eodSnapshotPersistenceAdapter.deleteAllInBatch();

    eodSnapshotPersistenceAdapter.saveAll(
        List.of(
            EodSnapshot.of(
                "111-111", Money.wons(10_000_000), Money.wons(1000), LocalDate.of(2026, 8, 1)),
            EodSnapshot.of(
                "111-111", Money.wons(11_000_000), Money.wons(1100), LocalDate.of(2026, 8, 2)),
            EodSnapshot.of(
                "111-111", Money.wons(12_000_000), Money.wons(1200), LocalDate.of(2026, 8, 10)),
            EodSnapshot.of(
                "222-222", Money.wons(5_000_000), Money.wons(500), LocalDate.of(2026, 8, 2))));
  }

  @Test
  @DisplayName("from/to 없이 계좌번호만으로 조회하면 해당 계좌의 전체 스냅샷을 반환한다.")
  void byAccountNumber_withoutRange_returnsAllForAccount() {
    PagedResult<EodSnapshot> result =
        eodSnapshotPersistenceAdapter.byAccountNumber("111-111", null, null, 0, 20);

    assertThat(result.totalElements()).isEqualTo(3);
    assertThat(result.items()).allMatch(s -> s.accountNumber().equals("111-111"));
  }

  @Test
  @DisplayName("from/to를 지정하면 해당 계좌의 범위 내 스냅샷만 반환한다.")
  void byAccountNumber_withRange_filtersByDate() {
    PagedResult<EodSnapshot> result =
        eodSnapshotPersistenceAdapter.byAccountNumber(
            "111-111", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 5), 0, 20);

    assertThat(result.totalElements()).isEqualTo(2);
    assertThat(result.items()).allMatch(s -> !s.settlementDate().isAfter(LocalDate.of(2026, 8, 5)));
  }

  @Test
  @DisplayName("byDate로 조회하면 그 날짜에 정산된 모든 계좌의 스냅샷을 반환한다.")
  void byDate_returnsAllAccountsForThatDate() {
    PagedResult<EodSnapshot> result =
        eodSnapshotPersistenceAdapter.byDate(LocalDate.of(2026, 8, 2), 0, 20);

    assertThat(result.totalElements()).isEqualTo(2);
    assertThat(result.items())
        .extracting(EodSnapshot::accountNumber)
        .containsExactlyInAnyOrder("111-111", "222-222");
  }

  @Test
  @DisplayName("전체 건수보다 큰 페이지를 요청하면 빈 content를 반환하되 totalElements는 정확하다.")
  void byAccountNumber_pageBeyondTotal_returnsEmptyContent() {
    PagedResult<EodSnapshot> result =
        eodSnapshotPersistenceAdapter.byAccountNumber("111-111", null, null, 5, 20);

    assertThat(result.items()).isEmpty();
    assertThat(result.totalElements()).isEqualTo(3);
  }
}
