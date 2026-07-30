package com.fbrl.domain.model;

import com.fbrl.domain.exception.InsufficientBalanceException;
import java.util.Objects;

public class Account {
  private final Long id;
  private final String accountNumber;
  private Money balance;
  private final Long version;

  public Account(Long id, String accountNumber, Money balance, Long version) {
    this.id = id;
    this.accountNumber = Objects.requireNonNull(accountNumber, "계좌번호는 null일 수 없습니다.");
    this.balance = Objects.requireNonNull(balance, "잔액은 null일 수 없습니다.");
    this.version = version;
  }

  public static Account create(String accountNumber, Money initialBalance) {
    return new Account(null, accountNumber, initialBalance, null);
  }

  public void deposit(Money money) {
    Objects.requireNonNull(money, "입금 금액은 null일 수 없습니다.");
    this.balance = this.balance.add(money);
  }

  public void withdraw(Money money) {
    Objects.requireNonNull(money, "출금 금액은 null일 수 없습니다.");

    if (!this.balance.isGreaterThanOrEqual(money)) {
      throw new InsufficientBalanceException("잔액이 부족합니다. 현재 잔액: " + balance + ", 요청금액: " + money);
    }
    this.balance = this.balance.subtract(money);
  }

  public Long getId() {
    return id;
  }

  public String getAccountNumber() {
    return accountNumber;
  }

  public Money getBalance() {
    return balance;
  }

  public Long getVersion() {
    return version;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Account account = (Account) o;
    return Objects.equals(accountNumber, account.accountNumber);
  }

  @Override
  public int hashCode() {
    return Objects.hash(accountNumber);
  }
}
