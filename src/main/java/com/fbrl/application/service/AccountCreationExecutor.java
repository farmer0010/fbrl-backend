package com.fbrl.application.service;

import com.fbrl.application.port.out.AccountRepositoryPort;
import com.fbrl.domain.model.Account;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AccountCreationExecutor {
  private final AccountRepositoryPort accountRepositoryPort;

  public AccountCreationExecutor(AccountRepositoryPort accountRepositoryPort) {
    this.accountRepositoryPort = accountRepositoryPort;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Account createInNewTransaction(String accountNumber) {
    return accountRepositoryPort.save(Account.open(accountNumber));
  }
}
