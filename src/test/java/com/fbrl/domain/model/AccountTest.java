package com.fbrl.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Account 엔티티 단위 테스트")
class AccountTest {

  @Test
  @DisplayName("계좌 생성 시 계좌번호가 올바르게 설정된다.")
  void createAccount() {
    Account account = Account.create("123-456-789");

    assertThat(account.getAccountNumber()).isEqualTo("123-456-789");
  }

  @Nested
  @DisplayName("앵커+델타 기반 잔액 계산 검증")
  class CalculateBalanceTest {

    @Test
    @DisplayName("델타 원장이 없으면 앵커 잔액을 그대로 반환한다.")
    void calculateBalance_noDelta_returnsAnchor() {
      Account account = Account.create("123-456-789");

      Money balance = account.calculateBalance(Money.wons(10000), List.of());

      assertThat(balance).isEqualTo(Money.wons(10000));
    }

    @Test
    @DisplayName("CREDIT 원장은 잔액을 증가시킨다.")
    void calculateBalance_credit_increasesBalance() {
      Account account = Account.create("123-456-789");
      List<LedgerEntry> entries =
          List.of(
              LedgerEntry.of(
                  "123-456-789", LedgerDirection.CREDIT, Money.wons(5000), "tx-1", Instant.now()));

      Money balance = account.calculateBalance(Money.wons(10000), entries);

      assertThat(balance).isEqualTo(Money.wons(15000));
    }

    @Test
    @DisplayName("DEBIT 원장은 잔액을 감소시킨다.")
    void calculateBalance_debit_decreasesBalance() {
      Account account = Account.create("123-456-789");
      List<LedgerEntry> entries =
          List.of(
              LedgerEntry.of(
                  "123-456-789", LedgerDirection.DEBIT, Money.wons(4000), "tx-1", Instant.now()));

      Money balance = account.calculateBalance(Money.wons(10000), entries);

      assertThat(balance).isEqualTo(Money.wons(6000));
    }

    @Test
    @DisplayName("DEBIT/CREDIT이 섞여 있으면 누적 합산된 잔액을 반환한다.")
    void calculateBalance_mixedEntries_accumulates() {
      Account account = Account.create("123-456-789");
      List<LedgerEntry> entries =
          List.of(
              LedgerEntry.of(
                  "123-456-789", LedgerDirection.CREDIT, Money.wons(3000), "tx-1", Instant.now()),
              LedgerEntry.of(
                  "123-456-789", LedgerDirection.DEBIT, Money.wons(1000), "tx-2", Instant.now()));

      Money balance = account.calculateBalance(Money.wons(10000), entries);

      assertThat(balance).isEqualTo(Money.wons(12000));
    }
  }
}
