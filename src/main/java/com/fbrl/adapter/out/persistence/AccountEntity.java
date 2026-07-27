package com.fbrl.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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

  protected AccountEntity() {}

  public AccountEntity(Long id, String accountNumber, BigDecimal balance) {
    this.id = id;
    this.accountNumber = accountNumber;
    this.balance = balance;
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
}
