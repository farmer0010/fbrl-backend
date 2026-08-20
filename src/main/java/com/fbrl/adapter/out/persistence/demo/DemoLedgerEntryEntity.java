package com.fbrl.adapter.out.persistence.demo;

import com.fbrl.domain.model.LedgerDirection;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
    name = "ledger_entries",
    indexes =
        @Index(
            name = "idx_demo_ledger_account_occurred",
            columnList = "account_number, occurred_at"))
public class DemoLedgerEntryEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "account_number", nullable = false)
  private String accountNumber;

  @Enumerated(EnumType.STRING)
  @Column(name = "direction", nullable = false)
  private LedgerDirection direction;

  @Column(name = "amount", nullable = false)
  private BigDecimal amount;

  @Column(name = "transaction_id", nullable = false)
  private String transactionId;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  protected DemoLedgerEntryEntity() {}

  public DemoLedgerEntryEntity(
      Long id,
      String accountNumber,
      LedgerDirection direction,
      BigDecimal amount,
      String transactionId,
      Instant occurredAt) {
    this.id = id;
    this.accountNumber = accountNumber;
    this.direction = direction;
    this.amount = amount;
    this.transactionId = transactionId;
    this.occurredAt = occurredAt;
  }

  public Long getId() {
    return id;
  }

  public String getAccountNumber() {
    return accountNumber;
  }

  public LedgerDirection getDirection() {
    return direction;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getTransactionId() {
    return transactionId;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }
}
