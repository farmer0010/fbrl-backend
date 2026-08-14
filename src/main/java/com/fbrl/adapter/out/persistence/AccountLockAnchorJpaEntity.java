package com.fbrl.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "account_lock_anchors")
public class AccountLockAnchorJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "account_number", nullable = false, unique = true)
  private String accountNumber;

  @Column(name = "lock_generation", nullable = false)
  private long lockGeneration;

  @Version private Long version;

  protected AccountLockAnchorJpaEntity() {}

  public AccountLockAnchorJpaEntity(String accountNumber) {
    this.accountNumber = accountNumber;
    this.lockGeneration = 0L;
  }

  public void bumpGeneration() {
    this.lockGeneration++;
  }

  public Long getId() {
    return id;
  }

  public String getAccountNumber() {
    return accountNumber;
  }

  public long getLockGeneration() {
    return lockGeneration;
  }

  public Long getVersion() {
    return version;
  }
}
