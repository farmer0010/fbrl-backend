package com.fbrl.application.service;

import com.fbrl.application.port.in.CreateDemoAccountUseCase;
import com.fbrl.application.port.out.AccountRepositoryPort;
import com.fbrl.domain.model.Account;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateDemoAccountService implements CreateDemoAccountUseCase {
  private final AccountNumberPolicy accountNumberPolicy;
  private final AccountRepositoryPort demoAccountRepositoryPort;

  public CreateDemoAccountService(
      AccountNumberPolicy accountNumberPolicy,
      @Qualifier("demo") AccountRepositoryPort demoAccountRepositoryPort) {
    this.accountNumberPolicy = accountNumberPolicy;
    this.demoAccountRepositoryPort = demoAccountRepositoryPort;
  }

  @Override
  @Transactional("demoTransactionManager")
  public Account createAccount() {
    return demoAccountRepositoryPort.save(Account.open(accountNumberPolicy.generate()));
  }
}
