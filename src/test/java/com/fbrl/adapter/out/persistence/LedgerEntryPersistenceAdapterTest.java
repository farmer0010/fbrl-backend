package com.fbrl.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.fbrl.application.port.out.SaveLedgerEntryPort;
import com.fbrl.domain.model.LedgerBalanceDelta;
import com.fbrl.domain.model.LedgerEntry;
import com.fbrl.domain.model.Money;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@DisplayName("LedgerEntryPersistenceAdapter 델타 배치 조회 테스트")
class LedgerEntryPersistenceAdapterTest {

  @Autowired private LedgerEntryPersistenceAdapter ledgerEntryPersistenceAdapter;
  @Autowired private SaveLedgerEntryPort saveLedgerEntryPort;

  @BeforeEach
  void setUp() {
    ledgerEntryPersistenceAdapter.deleteAllInBatch();
  }

  @Test
  @DisplayName("요청한 계좌 중 활동이 없는 계좌도 결과 Map에 Money.ZERO로 채워진다.")
  void loadBalanceDeltasUntil_fillsInactiveAccountsWithZero() {
    saveLedgerEntryPort.saveAll(
        LedgerEntry.transferPair(
                "TEST-SEED-SOURCE", "111-111", Money.wons(5000), "TEST_SEED", Instant.now())
            .entries());

    Map<String, LedgerBalanceDelta> deltas =
        ledgerEntryPersistenceAdapter.loadBalanceDeltasUntil(
            List.of("111-111", "222-222"), Instant.now().plusSeconds(60));

    assertThat(deltas).containsKey("111-111").containsKey("222-222");
    assertThat(deltas.get("111-111").creditTotal()).isEqualTo(Money.wons(5000));
    assertThat(deltas.get("111-111").debitTotal()).isEqualTo(Money.ZERO);
    assertThat(deltas.get("222-222").creditTotal()).isEqualTo(Money.ZERO);
    assertThat(deltas.get("222-222").debitTotal()).isEqualTo(Money.ZERO);
  }

  @Test
  @DisplayName("until 시각 이후 발생한 원장은 델타 계산에서 제외된다.")
  void loadBalanceDeltasUntil_excludesEntriesAfterUntil() {
    Instant cutoff = Instant.now();
    saveLedgerEntryPort.saveAll(
        LedgerEntry.transferPair(
                "TEST-SEED-SOURCE",
                "111-111",
                Money.wons(3000),
                "TEST_SEED_AFTER",
                cutoff.plusSeconds(3600))
            .entries());

    Map<String, LedgerBalanceDelta> deltas =
        ledgerEntryPersistenceAdapter.loadBalanceDeltasUntil(List.of("111-111"), cutoff);

    assertThat(deltas.get("111-111").creditTotal()).isEqualTo(Money.ZERO);
    assertThat(deltas.get("111-111").debitTotal()).isEqualTo(Money.ZERO);
  }
}
