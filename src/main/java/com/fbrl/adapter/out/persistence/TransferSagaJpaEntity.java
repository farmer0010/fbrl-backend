package com.fbrl.adapter.out.persistence;

import com.fbrl.domain.model.SagaStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transfer_sagas")
public class TransferSagaJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "saga_id", nullable = false, unique = true)
  private String sagaId;

  @Column(name = "from_account_number", nullable = false)
  private String fromAccountNumber;

  @Column(name = "to_account_number", nullable = false)
  private String toAccountNumber;

  @Column(name = "amount", nullable = false)
  private BigDecimal amount;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private SagaStatus status;

  @Column(name = "started_at", nullable = false)
  private Instant startedAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version private Long version;

  protected TransferSagaJpaEntity() {}

  public TransferSagaJpaEntity(
      Long id,
      String sagaId,
      String fromAccountNumber,
      String toAccountNumber,
      BigDecimal amount,
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

  public BigDecimal getAmount() {
    return amount;
  }

  public SagaStatus getStatus() {
    return status;
  }

  public void setStatus(SagaStatus status) {
    this.status = status;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  public Long getVersion() {
    return version;
  }
}
