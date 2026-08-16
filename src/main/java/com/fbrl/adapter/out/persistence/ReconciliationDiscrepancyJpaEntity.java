package com.fbrl.adapter.out.persistence;

import com.fbrl.domain.model.ReconciliationStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
    name = "reconciliation_discrepancies",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_reconciliation_discrepancy_account_date",
            columnNames = {"account_number", "settlement_date"}))
public class ReconciliationDiscrepancyJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "account_number", nullable = false)
  private String accountNumber;

  @Column(name = "settlement_date", nullable = false)
  private LocalDate settlementDate;

  @Column(name = "expected_balance")
  private BigDecimal expectedBalance;

  @Column(name = "actual_balance")
  private BigDecimal actualBalance;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private ReconciliationStatus status;

  @Column(name = "computed_at", nullable = false)
  private Instant computedAt;

  protected ReconciliationDiscrepancyJpaEntity() {}

  public ReconciliationDiscrepancyJpaEntity(
      Long id,
      String accountNumber,
      LocalDate settlementDate,
      BigDecimal expectedBalance,
      BigDecimal actualBalance,
      ReconciliationStatus status,
      Instant computedAt) {
    this.id = id;
    this.accountNumber = accountNumber;
    this.settlementDate = settlementDate;
    this.expectedBalance = expectedBalance;
    this.actualBalance = actualBalance;
    this.status = status;
    this.computedAt = computedAt;
  }

  public Long getId() {
    return id;
  }

  public String getAccountNumber() {
    return accountNumber;
  }

  public LocalDate getSettlementDate() {
    return settlementDate;
  }

  public BigDecimal getExpectedBalance() {
    return expectedBalance;
  }

  public BigDecimal getActualBalance() {
    return actualBalance;
  }

  public ReconciliationStatus getStatus() {
    return status;
  }

  public Instant getComputedAt() {
    return computedAt;
  }
}
