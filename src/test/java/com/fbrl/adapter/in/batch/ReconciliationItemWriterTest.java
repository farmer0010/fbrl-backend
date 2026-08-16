package com.fbrl.adapter.in.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fbrl.application.port.out.LoadEodSnapshotByDatePort;
import com.fbrl.application.port.out.LoadLedgerBalanceDeltasPort;
import com.fbrl.application.port.out.SaveReconciliationDiscrepancyPort;
import com.fbrl.domain.model.Account;
import com.fbrl.domain.model.EodSnapshot;
import com.fbrl.domain.model.LedgerBalanceDelta;
import com.fbrl.domain.model.Money;
import com.fbrl.domain.model.ReconciliationDiscrepancy;
import com.fbrl.domain.model.ReconciliationStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.infrastructure.item.Chunk;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReconciliationItemWriter 단위 테스트")
class ReconciliationItemWriterTest {

  private static final LocalDate SETTLEMENT_DATE = LocalDate.of(2026, 8, 16);
  private static final Instant AS_OF = Instant.parse("2026-08-16T03:00:00Z");

  @Mock private LoadEodSnapshotByDatePort loadEodSnapshotByDatePort;
  @Mock private LoadLedgerBalanceDeltasPort loadLedgerBalanceDeltasPort;
  @Mock private SaveReconciliationDiscrepancyPort saveReconciliationDiscrepancyPort;

  private ReconciliationItemWriter writer() {
    return new ReconciliationItemWriter(
        loadEodSnapshotByDatePort,
        loadLedgerBalanceDeltasPort,
        saveReconciliationDiscrepancyPort,
        SETTLEMENT_DATE,
        AS_OF);
  }

  @Test
  @DisplayName("이자가 붙어 있어도 재계산 값이 closingBalance와 일치하면 MATCH이며 저장하지 않는다.")
  void write_matchingAccountWithNonZeroInterest_doesNotSave() {
    ReconciliationItemWriter writer = writer();
    Account account = Account.create("111-111");
    EodSnapshot snapshot =
        EodSnapshot.of("111-111", Money.wons(8000), Money.wons(500), SETTLEMENT_DATE);

    given(
            loadEodSnapshotByDatePort.loadByAccountNumbersAndDate(
                List.of("111-111"), SETTLEMENT_DATE))
        .willReturn(Map.of("111-111", snapshot));
    given(loadLedgerBalanceDeltasPort.loadBalanceDeltasUntil(List.of("111-111"), AS_OF))
        .willReturn(Map.of("111-111", new LedgerBalanceDelta(Money.wons(10000), Money.wons(2000))));

    writer.write(new Chunk<>(List.of(account)));

    verify(saveReconciliationDiscrepancyPort, never()).saveAll(any());
  }

  @Test
  @DisplayName("이자를 제외한 closingBalance와 재계산 값이 다르면 MISMATCH로 저장한다.")
  void write_mismatchingAccount_savesMismatch() {
    ReconciliationItemWriter writer = writer();
    Account account = Account.create("222-222");
    EodSnapshot snapshot =
        EodSnapshot.of("222-222", Money.wons(9000), Money.wons(300), SETTLEMENT_DATE);

    given(
            loadEodSnapshotByDatePort.loadByAccountNumbersAndDate(
                List.of("222-222"), SETTLEMENT_DATE))
        .willReturn(Map.of("222-222", snapshot));
    given(loadLedgerBalanceDeltasPort.loadBalanceDeltasUntil(List.of("222-222"), AS_OF))
        .willReturn(Map.of("222-222", new LedgerBalanceDelta(Money.wons(8500), Money.ZERO)));

    writer.write(new Chunk<>(List.of(account)));

    ArgumentCaptor<List<ReconciliationDiscrepancy>> captor = ArgumentCaptor.forClass(List.class);
    verify(saveReconciliationDiscrepancyPort).saveAll(captor.capture());

    List<ReconciliationDiscrepancy> saved = captor.getValue();
    assertThat(saved).hasSize(1);
    assertThat(saved.get(0).status()).isEqualTo(ReconciliationStatus.MISMATCH);
    assertThat(saved.get(0).accountNumber()).isEqualTo("222-222");
    assertThat(saved.get(0).expectedBalance()).isEqualTo(Money.wons(9000));
    assertThat(saved.get(0).actualBalance()).isEqualTo(Money.wons(8500));
  }

  @Test
  @DisplayName("오늘 날짜 스냅샷이 없으면 NO_SNAPSHOT으로 저장하고 델타 조회는 하지 않는다.")
  void write_noSnapshotAccount_savesNoSnapshot() {
    ReconciliationItemWriter writer = writer();
    Account account = Account.create("333-333");

    given(
            loadEodSnapshotByDatePort.loadByAccountNumbersAndDate(
                List.of("333-333"), SETTLEMENT_DATE))
        .willReturn(Map.of());

    writer.write(new Chunk<>(List.of(account)));

    ArgumentCaptor<List<ReconciliationDiscrepancy>> captor = ArgumentCaptor.forClass(List.class);
    verify(saveReconciliationDiscrepancyPort).saveAll(captor.capture());
    verify(loadLedgerBalanceDeltasPort, never()).loadBalanceDeltasUntil(any(), any());

    List<ReconciliationDiscrepancy> saved = captor.getValue();
    assertThat(saved).hasSize(1);
    assertThat(saved.get(0).status()).isEqualTo(ReconciliationStatus.NO_SNAPSHOT);
    assertThat(saved.get(0).accountNumber()).isEqualTo("333-333");
    assertThat(saved.get(0).expectedBalance()).isNull();
    assertThat(saved.get(0).actualBalance()).isNull();
  }
}
