package com.fbrl.domain.model;

import com.fbrl.domain.exception.InvalidApprovalTransitionException;
import com.fbrl.domain.exception.InvalidTransferAmountException;
import com.fbrl.domain.exception.RejectionReasonRequiredException;
import com.fbrl.domain.exception.SelfApprovalNotAllowedException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class TransferApprovalRequest {
  private final Long id;
  private final String requestId;
  private final String makerId;
  private String checkerId;
  private final String fromAccountNumber;
  private final String toAccountNumber;
  private final Money amount;
  private ApprovalStatus status;
  private String rejectionReason;
  private final Instant requestedAt;
  private Instant decidedAt;
  private final Long version;

  private TransferApprovalRequest(
      Long id,
      String requestId,
      String makerId,
      String checkerId,
      String fromAccountNumber,
      String toAccountNumber,
      Money amount,
      ApprovalStatus status,
      String rejectionReason,
      Instant requestedAt,
      Instant decidedAt,
      Long version) {
    this.id = id;
    this.requestId = requestId;
    this.makerId = makerId;
    this.checkerId = checkerId;
    this.fromAccountNumber = fromAccountNumber;
    this.toAccountNumber = toAccountNumber;
    this.amount = amount;
    this.status = status;
    this.rejectionReason = rejectionReason;
    this.requestedAt = requestedAt;
    this.decidedAt = decidedAt;
    this.version = version;
  }

  public static TransferApprovalRequest request(
      String makerId, String fromAccountNumber, String toAccountNumber, Money amount) {
    Objects.requireNonNull(makerId, "기안자 ID는 필수입니다.");
    Objects.requireNonNull(fromAccountNumber, "출금 계좌번호는 필수입니다.");
    Objects.requireNonNull(toAccountNumber, "입금 계좌번호는 필수입니다.");
    Objects.requireNonNull(amount, "이체 금액은 필수입니다.");

    if (amount.getAmount().signum() <= 0) {
      throw new InvalidTransferAmountException(amount.getAmount());
    }

    Instant now = Instant.now();
    return new TransferApprovalRequest(
        null,
        UUID.randomUUID().toString(),
        makerId,
        null,
        fromAccountNumber,
        toAccountNumber,
        amount,
        ApprovalStatus.PENDING,
        null,
        now,
        null,
        null);
  }

  public static TransferApprovalRequest reconstruct(
      Long id,
      String requestId,
      String makerId,
      String checkerId,
      String fromAccountNumber,
      String toAccountNumber,
      Money amount,
      ApprovalStatus status,
      String rejectionReason,
      Instant requestedAt,
      Instant decidedAt,
      Long version) {
    return new TransferApprovalRequest(
        id,
        requestId,
        makerId,
        checkerId,
        fromAccountNumber,
        toAccountNumber,
        amount,
        status,
        rejectionReason,
        requestedAt,
        decidedAt,
        version);
  }

  public void approve(String checkerId) {
    Objects.requireNonNull(checkerId, "승인자 ID는 필수입니다.");
    assertNotSelfApproval(checkerId);
    transitionTo(ApprovalStatus.APPROVED);
    this.checkerId = checkerId;
    this.decidedAt = Instant.now();
  }

  public void reject(String checkerId, String rejectionReason) {
    Objects.requireNonNull(checkerId, "승인자 ID는 필수입니다.");
    assertNotSelfApproval(checkerId);
    if (rejectionReason == null || rejectionReason.isBlank()) {
      throw new RejectionReasonRequiredException();
    }
    transitionTo(ApprovalStatus.REJECTED);
    this.checkerId = checkerId;
    this.rejectionReason = rejectionReason;
    this.decidedAt = Instant.now();
  }

  private void assertNotSelfApproval(String checkerId) {
    if (checkerId.equals(makerId)) {
      throw new SelfApprovalNotAllowedException(makerId);
    }
  }

  private void transitionTo(ApprovalStatus target) {
    if (!status.canTransitionTo(target)) {
      throw new InvalidApprovalTransitionException(status, target);
    }
    this.status = target;
  }

  public Long getId() {
    return id;
  }

  public String getRequestId() {
    return requestId;
  }

  public String getMakerId() {
    return makerId;
  }

  public String getCheckerId() {
    return checkerId;
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

  public ApprovalStatus getStatus() {
    return status;
  }

  public String getRejectionReason() {
    return rejectionReason;
  }

  public Instant getRequestedAt() {
    return requestedAt;
  }

  public Instant getDecidedAt() {
    return decidedAt;
  }

  public Long getVersion() {
    return version;
  }
}
