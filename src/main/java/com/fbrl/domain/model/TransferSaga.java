package com.fbrl.domain.model;

import com.fbrl.domain.exception.InvalidSagaTransitionException;
import com.fbrl.domain.exception.InvalidTransferAmountException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class TransferSaga {
  private final Long id;
  private final String sagaId;
  private final String fromAccountNumber;
  private final String toAccountNumber;
  private final Money amount;
  private SagaStatus status;
  private final Instant startedAt;
  private Instant updatedAt;
  private final Long version;

  private TransferSaga(
      Long id,
      String sagaId,
      String fromAccountNumber,
      String toAccountNumber,
      Money amount,
      SagaStatus status,
      Instant startedAt,
      Instant updatedAt,
      Long version) {
    this.id = id;
    this.sagaId = sagaId;
    this.fromAccountNumber = fromAccountNumber;
    this.toAccountNumber = toAccountNumber;
    this.amount = amount;
    this.status = status;
    this.startedAt = startedAt;
    this.updatedAt = updatedAt;
    this.version = version;
  }

  public static TransferSaga start(String fromAccountNumber, String toAccountNumber, Money amount) {
    Objects.requireNonNull(fromAccountNumber, "출금 계좌번호는 필수입니다.");
    Objects.requireNonNull(toAccountNumber, "입금 계좌번호는 필수입니다.");
    Objects.requireNonNull(amount, "이체 금액은 필수입니다.");

    if (amount.getAmount().signum() <= 0) {
      throw new InvalidTransferAmountException(amount.getAmount());
    }

    Instant now = Instant.now();
    return new TransferSaga(
        null,
        UUID.randomUUID().toString(),
        fromAccountNumber,
        toAccountNumber,
        amount,
        SagaStatus.STARTED,
        now,
        now,
        null);
  }

  public static TransferSaga reconstruct(
      Long id,
      String sagaId,
      String fromAccountNumber,
      String toAccountNumber,
      Money amount,
      SagaStatus status,
      Instant startedAt,
      Instant updatedAt,
      Long version) {
    return new TransferSaga(
        id,
        sagaId,
        fromAccountNumber,
        toAccountNumber,
        amount,
        status,
        startedAt,
        updatedAt,
        version);
  }

  public void completeWithdrawal() {
    transitionTo(SagaStatus.WITHDRAWAL_COMPLETED);
  }

  public void complete() {
    transitionTo(SagaStatus.COMPLETED);
  }

  public void startCompensation() {
    transitionTo(SagaStatus.COMPENSATING);
  }

  public void completeCompensation() {
    transitionTo(SagaStatus.COMPENSATED);
  }

  public void fail() {
    transitionTo(SagaStatus.FAILED);
  }

  private void transitionTo(SagaStatus target) {
    if (!status.canTransitionTo(target)) {
      throw new InvalidSagaTransitionException(status, target);
    }
    this.status = target;
    this.updatedAt = Instant.now();
  }

  public Long getId() {
    return id;
  }

  public String getSagaId() {
    return sagaId;
  }

  public String getFromAccountNumber() {
    return fromAccountNumber;
  }

  public String getToAccountNumber() {
    return toAccountNumber;
  }

  public Money getAmount() {
    return amount;
  }

  public SagaStatus getStatus() {
    return status;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public Long getVersion() {
    return version;
  }
}
