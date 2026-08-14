package com.fbrl.adapter.out.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
    name = "eod_snapshots",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_eod_snapshot_account_date",
            columnNames = {"account_number", "settlement_date"}))
public class EodSnapshotJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "account_number", nullable = false)
  private String accountNumber;

  @Column(name = "closing_balance", nullable = false)
  private BigDecimal closingBalance;

  @Column(name = "interest_amount", nullable = false)
  private BigDecimal interestAmount;

  @Column(name = "settlement_date", nullable = false)
  private LocalDate settlementDate;

  @Column(name = "computed_at", nullable = false)
  private Instant computedAt;

  protected EodSnapshotJpaEntity() {}

  public EodSnapshotJpaEntity(
      Long id,
      String accountNumber,
      BigDecimal closingBalance,
      BigDecimal interestAmount,
      LocalDate settlementDate,
      Instant computedAt) {
    this.id = id;
    this.accountNumber = accountNumber;
    this.closingBalance = closingBalance;
    this.interestAmount = interestAmount;
    this.settlementDate = settlementDate;
    this.computedAt = computedAt;
  }

  public Long getId() {
    return id;
  }

  public String getAccountNumber() {
    return accountNumber;
  }

  public BigDecimal getClosingBalance() {
    return closingBalance;
  }

  public BigDecimal getInterestAmount() {
    return interestAmount;
  }

  public LocalDate getSettlementDate() {
    return settlementDate;
  }

  public Instant getComputedAt() {
    return computedAt;
  }
}
