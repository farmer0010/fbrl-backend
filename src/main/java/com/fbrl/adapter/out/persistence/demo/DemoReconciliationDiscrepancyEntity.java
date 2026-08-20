package com.fbrl.adapter.out.persistence.demo;

import com.fbrl.domain.model.ReconciliationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
    name = "reconciliation_discrepancies",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_demo_reconciliation_discrepancy_account_date",
            columnNames = {"account_number", "settlement_date"}))
public class DemoReconciliationDiscrepancyEntity {

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

  protected DemoReconciliationDiscrepancyEntity() {}

  public DemoReconciliationDiscrepancyEntity(
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
