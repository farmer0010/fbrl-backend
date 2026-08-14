package com.fbrl.domain.model;

import java.util.List;
import java.util.Objects;

public class Account {
  private final Long id;
  private final String accountNumber;
  private final Long version;

  private Account(Long id, String accountNumber, Long version) {
    this.id = id;
    this.accountNumber = Objects.requireNonNull(accountNumber, "계좌번호는 null일 수 없습니다.");
    this.version = version;
  }

  public static Account create(String accountNumber) {
    return new Account(null, accountNumber, null);
  }

  public static Account open(String accountNumber) {
    return create(accountNumber);
  }

  public static Account reconstruct(Long id, String accountNumber, Long version) {
    return new Account(id, accountNumber, version);
  }

  public Money calculateBalance(Money anchorBalance, List<LedgerEntry> entriesSinceAnchor) {
    Objects.requireNonNull(anchorBalance, "앵커 잔액은 null일 수 없습니다.");
    Objects.requireNonNull(entriesSinceAnchor, "델타 원장 목록은 null일 수 없습니다.");

    Money creditTotal = Money.ZERO;
    Money debitTotal = Money.ZERO;
    for (LedgerEntry entry : entriesSinceAnchor) {
      if (entry.direction() == LedgerDirection.CREDIT) {
        creditTotal = creditTotal.add(entry.amount());
      } else {
        debitTotal = debitTotal.add(entry.amount());
      }
    }

    return anchorBalance.add(creditTotal).subtract(debitTotal);
  }

  public Long getId() {
    return id;
  }

  public String getAccountNumber() {
    return accountNumber;
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
