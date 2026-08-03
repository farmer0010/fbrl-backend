package com.fbrl.application.service;

import com.fbrl.application.port.in.CreateAccountUseCase;
import com.fbrl.domain.exception.DuplicateAccountNumberException;
import com.fbrl.domain.model.Account;
import org.springframework.stereotype.Service;

@Service
public class CreateAccountService implements CreateAccountUseCase {
  private static final int MAX_RETRY = 3;

  private final AccountNumberPolicy accountNumberPolicy;
  private final AccountCreationExecutor accountCreationExecutor;

  public CreateAccountService(
      AccountNumberPolicy accountNumberPolicy, AccountCreationExecutor accountCreationExecutor) {
    this.accountNumberPolicy = accountNumberPolicy;
    this.accountCreationExecutor = accountCreationExecutor;
  }

  @Override
  public Account createAccount() {
    DuplicateAccountNumberException lastFailure = null;
    for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
      String accountNumber = accountNumberPolicy.generate();
      try {
        return accountCreationExecutor.createInNewTransaction(accountNumber);
      } catch (DuplicateAccountNumberException e) {
        lastFailure = e;
      }
    }
    throw lastFailure;
  }
}
