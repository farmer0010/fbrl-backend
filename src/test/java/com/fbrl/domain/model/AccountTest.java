package com.fbrl.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fbrl.domain.exception.InsufficientBalanceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Account 엔티티 단위 테스트")
class AccountTest {

  @Test
  @DisplayName("계좌 생성 시 초기 잔액이 올바르게 설정된다.")
  void createAccount() {
    String accountNumber = "123-456-789";
    Money initialBalance = Money.wons(50000);

    Account account = Account.create(accountNumber, initialBalance);

    assertThat(account.getAccountNumber()).isEqualTo("123-456-789");
    assertThat(account.getBalance()).isEqualTo(Money.wons(50000));
  }

  @Nested
  @DisplayName("입출금 비즈니스 로직 검증")
  class TransactionTest {

    @Test
    @DisplayName("입금 시 잔액이 증가한다.")
    void deposit() {
      Account account = Account.create("123-456-789", Money.wons(10000));

      account.deposit(Money.wons(5000));

      assertThat(account.getBalance()).isEqualTo(Money.wons(15000));
    }

    @Test
    @DisplayName("출금 시 잔액이 차감된다.")
    void withdrawSuccess() {
      Account account = Account.create("123-456-789", Money.wons(10000));

      account.withdraw(Money.wons(4000));

      assertThat(account.getBalance()).isEqualTo(Money.wons(6000));
    }

    @Test
    @DisplayName("잔액보다 큰 금액을 출금 시 InsufficientBalanceException이 발생한다.")
    void withdrawWithInsufficientBalanceThrowsException() {
      Account account = Account.create("123-456-789", Money.wons(10000));

      assertThatThrownBy(() -> account.withdraw(Money.wons(20000)))
          .isInstanceOf(InsufficientBalanceException.class)
          .hasMessageContaining("잔액이 부족합니다");
    }
  }
}
