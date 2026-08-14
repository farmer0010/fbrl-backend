package com.fbrl.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.fbrl.application.port.out.LoadLatestEodSnapshotPort;
import com.fbrl.application.port.out.LoadLedgerEntriesPort;
import com.fbrl.domain.model.Account;
import com.fbrl.domain.model.EodSnapshot;
import com.fbrl.domain.model.LedgerDirection;
import com.fbrl.domain.model.LedgerEntry;
import com.fbrl.domain.model.Money;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountBalanceCalculator 단위 테스트")
class AccountBalanceCalculatorTest {

  @Mock private LoadLatestEodSnapshotPort loadLatestEodSnapshotPort;
  @Mock private LoadLedgerEntriesPort loadLedgerEntriesPort;
  @InjectMocks private AccountBalanceCalculator accountBalanceCalculator;

  @Test
  @DisplayName("EOD 스냅샷이 없으면 앵커를 0원, 커트오프를 EPOCH로 잡아 전체 원장을 델타로 합산한다.")
  void calculate_withoutSnapshot_usesZeroAnchorAndEpochCutoff() {
    Account account = Account.create("111-111");
    given(loadLatestEodSnapshotPort.findLatestByAccountNumber("111-111"))
        .willReturn(Optional.empty());
    given(loadLedgerEntriesPort.loadByAccountNumberSince(eq("111-111"), eq(Instant.EPOCH)))
        .willReturn(
            List.of(
                LedgerEntry.of(
                    "111-111", LedgerDirection.CREDIT, Money.wons(7000), "tx-1", Instant.now())));

    Money balance = accountBalanceCalculator.calculate(account);

    assertThat(balance).isEqualTo(Money.wons(7000));
  }

  @Test
  @DisplayName("EOD 스냅샷이 있으면 스냅샷의 totalBalance를 앵커로, computedAt을 커트오프로 사용한다.")
  void calculate_withSnapshot_usesSnapshotAsAnchor() {
    Account account = Account.create("111-111");
    Instant computedAt = Instant.parse("2026-08-13T17:00:00Z");
    EodSnapshot snapshot =
        new EodSnapshot(
            1L, "111-111", Money.wons(10000), Money.wons(0), LocalDate.of(2026, 8, 13), computedAt);

    given(loadLatestEodSnapshotPort.findLatestByAccountNumber("111-111"))
        .willReturn(Optional.of(snapshot));
    given(loadLedgerEntriesPort.loadByAccountNumberSince(eq("111-111"), eq(computedAt)))
        .willReturn(
            List.of(
                LedgerEntry.of(
                    "111-111", LedgerDirection.DEBIT, Money.wons(2000), "tx-2", Instant.now())));

    Money balance = accountBalanceCalculator.calculate(account);

    assertThat(balance).isEqualTo(Money.wons(8000));
  }
}
