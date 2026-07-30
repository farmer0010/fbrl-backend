package com.fbrl.adapter.out.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "accounts")
public class AccountEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "account_number", nullable = false, unique = true)
  private String accountNumber;

  @Column(name = "balance", nullable = false)
  private BigDecimal balance;

  @Version private Long version;

  protected AccountEntity() {}

  public AccountEntity(Long id, String accountNumber, BigDecimal balance, Long version) {
    this.id = id;
    this.accountNumber = accountNumber;
    this.balance = balance;
    this.version = version;
  }

  public Long getId() {
    return id;
  }

  public String getAccountNumber() {
    return accountNumber;
  }

  public BigDecimal getBalance() {
    return balance;
  }

  public void setBalance(BigDecimal balance) {
    this.balance = balance;
  }

  public Long getVersion() {
    return version;
  }
}
