package com.fbrl.adapter.out.persistence.demo;

import jakarta.persistence.*;

@Entity
@Table(name = "accounts")
public class DemoAccountEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "account_number", nullable = false, unique = true)
  private String accountNumber;

  @Version private Long version;

  protected DemoAccountEntity() {}

  public DemoAccountEntity(Long id, String accountNumber, Long version) {
    this.id = id;
    this.accountNumber = accountNumber;
    this.version = version;
  }

  public Long getId() {
    return id;
  }

  public String getAccountNumber() {
    return accountNumber;
  }

  public Long getVersion() {
    return version;
  }
}
