package com.fbrl.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fbrl.application.port.out.PagedResult;
import com.fbrl.domain.exception.DuplicateReconciliationDiscrepancyException;
import com.fbrl.domain.model.Money;
import com.fbrl.domain.model.ReconciliationDiscrepancy;
import com.fbrl.domain.model.ReconciliationStatus;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@DisplayName("ReconciliationDiscrepancyPersistenceAdapter 유니크 제약 예외 번역 테스트")
class ReconciliationDiscrepancyPersistenceAdapterTest {

  @Autowired
  private ReconciliationDiscrepancyPersistenceAdapter reconciliationDiscrepancyPersistenceAdapter;

  @BeforeEach
  void setUp() {
    reconciliationDiscrepancyPersistenceAdapter.deleteAllInBatch();
  }

  @Test
  @DisplayName("같은 계좌·정산일자 조합이 중복 저장되면 DuplicateReconciliationDiscrepancyException으로 번역한다.")
  void saveAll_duplicateAccountAndSettlementDate_throwsDomainException() {
    LocalDate settlementDate = LocalDate.of(2026, 8, 16);
    ReconciliationDiscrepancy first =
        ReconciliationDiscrepancy.mismatch(
            "111-111", settlementDate, Money.wons(9000), Money.wons(8500));
    reconciliationDiscrepancyPersistenceAdapter.saveAll(List.of(first));

    ReconciliationDiscrepancy duplicate =
        ReconciliationDiscrepancy.mismatch(
            "111-111", settlementDate, Money.wons(9000), Money.wons(7000));

    assertThatThrownBy(
            () -> reconciliationDiscrepancyPersistenceAdapter.saveAll(List.of(duplicate)))
        .isInstanceOf(DuplicateReconciliationDiscrepancyException.class);
  }

  @Test
  @DisplayName("status를 지정하면 해당 상태만, 지정하지 않으면 기간 내 전체 상태를 반환한다.")
  void search_filtersByStatusAndDateRange() {
    LocalDate day1 = LocalDate.of(2026, 8, 1);
    LocalDate day2 = LocalDate.of(2026, 8, 2);
    LocalDate day3 = LocalDate.of(2026, 8, 3);
    reconciliationDiscrepancyPersistenceAdapter.saveAll(
        List.of(
            ReconciliationDiscrepancy.mismatch("111-111", day1, Money.wons(9000), Money.wons(8500)),
            ReconciliationDiscrepancy.noSnapshot("222-222", day2),
            ReconciliationDiscrepancy.mismatch(
                "333-333", day3, Money.wons(5000), Money.wons(4000))));

    PagedResult<ReconciliationDiscrepancy> all =
        reconciliationDiscrepancyPersistenceAdapter.search(null, day1, day2, 0, 20);
    assertThat(all.totalElements()).isEqualTo(2);

    PagedResult<ReconciliationDiscrepancy> mismatchOnly =
        reconciliationDiscrepancyPersistenceAdapter.search(
            ReconciliationStatus.MISMATCH, day1, day3, 0, 20);
    assertThat(mismatchOnly.totalElements()).isEqualTo(2);
    assertThat(mismatchOnly.items()).allMatch(d -> d.status() == ReconciliationStatus.MISMATCH);
  }

  @Test
  @DisplayName("전체 건수보다 큰 페이지를 요청하면 빈 content를 반환하되 totalElements는 정확하다.")
  void search_pageBeyondTotal_returnsEmptyContent() {
    LocalDate day = LocalDate.of(2026, 8, 1);
    reconciliationDiscrepancyPersistenceAdapter.saveAll(
        List.of(ReconciliationDiscrepancy.noSnapshot("111-111", day)));

    PagedResult<ReconciliationDiscrepancy> result =
        reconciliationDiscrepancyPersistenceAdapter.search(null, day, day, 5, 20);

    assertThat(result.items()).isEmpty();
    assertThat(result.totalElements()).isEqualTo(1);
  }
}
