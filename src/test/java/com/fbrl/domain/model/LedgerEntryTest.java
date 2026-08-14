package com.fbrl.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fbrl.domain.model.LedgerEntry.LedgerEntryPair;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LedgerEntry 단위 테스트")
class LedgerEntryTest {

  @Test
  @DisplayName("transferPair는 동일한 금액의 DEBIT/CREDIT 쌍을 생성하여 합이 항상 0이 되도록 보장한다.")
  void transferPair_createsBalancedPair() {
    Instant now = Instant.now();

    LedgerEntryPair pair =
        LedgerEntry.transferPair("111-111", "222-222", Money.wons(5000), "tx-1", now);

    assertThat(pair.debit().accountNumber()).isEqualTo("111-111");
    assertThat(pair.debit().direction()).isEqualTo(LedgerDirection.DEBIT);
    assertThat(pair.credit().accountNumber()).isEqualTo("222-222");
    assertThat(pair.credit().direction()).isEqualTo(LedgerDirection.CREDIT);

    assertThat(pair.debit().amount()).isEqualTo(pair.credit().amount());
    assertThat(pair.debit().transactionId()).isEqualTo(pair.credit().transactionId());
    assertThat(pair.entries()).hasSize(2).containsExactly(pair.debit(), pair.credit());
  }

  @Test
  @DisplayName("of()는 단일 방향 원장 항목을 생성한다.")
  void of_createsSingleEntry() {
    Instant now = Instant.now();

    LedgerEntry entry =
        LedgerEntry.of("111-111", LedgerDirection.CREDIT, Money.wons(3000), "saga-1", now);

    assertThat(entry.accountNumber()).isEqualTo("111-111");
    assertThat(entry.direction()).isEqualTo(LedgerDirection.CREDIT);
    assertThat(entry.amount()).isEqualTo(Money.wons(3000));
    assertThat(entry.transactionId()).isEqualTo("saga-1");
  }

  @Test
  @DisplayName("필수 필드가 null이면 생성할 수 없다.")
  void constructor_rejectsNullFields() {
    assertThatThrownBy(
            () ->
                new LedgerEntry(
                    null, null, LedgerDirection.DEBIT, Money.wons(1000), "tx", Instant.now()))
        .isInstanceOf(NullPointerException.class);
  }
}
