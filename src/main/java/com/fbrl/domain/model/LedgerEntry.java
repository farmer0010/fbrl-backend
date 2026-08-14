package com.fbrl.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record LedgerEntry(
    Long id,
    String accountNumber,
    LedgerDirection direction,
    Money amount,
    String transactionId,
    Instant occurredAt) {

  public LedgerEntry {
    Objects.requireNonNull(accountNumber, "계좌번호는 null일 수 없습니다.");
    Objects.requireNonNull(direction, "거래 방향은 null일 수 없습니다.");
    Objects.requireNonNull(amount, "금액은 null일 수 없습니다.");
    Objects.requireNonNull(transactionId, "거래 식별자는 null일 수 없습니다.");
    Objects.requireNonNull(occurredAt, "발생 시각은 null일 수 없습니다.");
  }

  public static LedgerEntry of(
      String accountNumber,
      LedgerDirection direction,
      Money amount,
      String transactionId,
      Instant occurredAt) {
    return new LedgerEntry(null, accountNumber, direction, amount, transactionId, occurredAt);
  }

  public static LedgerEntryPair transferPair(
      String fromAccountNumber,
      String toAccountNumber,
      Money amount,
      String transactionId,
      Instant occurredAt) {
    LedgerEntry debit =
        of(fromAccountNumber, LedgerDirection.DEBIT, amount, transactionId, occurredAt);
    LedgerEntry credit =
        of(toAccountNumber, LedgerDirection.CREDIT, amount, transactionId, occurredAt);
    return new LedgerEntryPair(debit, credit);
  }

  public record LedgerEntryPair(LedgerEntry debit, LedgerEntry credit) {
    public List<LedgerEntry> entries() {
      return List.of(debit, credit);
    }
  }
}
