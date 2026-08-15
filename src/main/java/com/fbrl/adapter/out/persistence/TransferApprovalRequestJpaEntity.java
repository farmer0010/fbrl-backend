package com.fbrl.adapter.out.persistence;

import com.fbrl.domain.model.ApprovalStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transfer_approval_requests")
public class TransferApprovalRequestJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "request_id", nullable = false, unique = true)
  private String requestId;

  @Column(name = "maker_id", nullable = false)
  private String makerId;

  @Column(name = "checker_id")
  private String checkerId;

  @Column(name = "from_account_number", nullable = false)
  private String fromAccountNumber;

  @Column(name = "to_account_number", nullable = false)
  private String toAccountNumber;

  @Column(name = "amount", nullable = false)
  private BigDecimal amount;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private ApprovalStatus status;

  @Column(name = "rejection_reason")
  private String rejectionReason;

  @Column(name = "requested_at", nullable = false)
  private Instant requestedAt;

  @Column(name = "decided_at")
  private Instant decidedAt;

  @Version private Long version;

  protected TransferApprovalRequestJpaEntity() {}

  public TransferApprovalRequestJpaEntity(
      Long id,
      String requestId,
      String makerId,
      String checkerId,
      String fromAccountNumber,
      String toAccountNumber,
      BigDecimal amount,
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

  public BigDecimal getAmount() {
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
