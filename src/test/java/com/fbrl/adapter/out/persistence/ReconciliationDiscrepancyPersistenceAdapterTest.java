package com.fbrl.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fbrl.domain.exception.DuplicateReconciliationDiscrepancyException;
import com.fbrl.domain.model.Money;
import com.fbrl.domain.model.ReconciliationDiscrepancy;
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
}
