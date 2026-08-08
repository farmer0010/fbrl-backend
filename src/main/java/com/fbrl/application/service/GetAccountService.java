package com.fbrl.application.service;

import com.fbrl.application.port.in.GetAccountUseCase;
import com.fbrl.application.port.out.AccountRepositoryPort;
import com.fbrl.domain.exception.AccountNotFoundException;
import com.fbrl.domain.model.Account;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetAccountService implements GetAccountUseCase {
  private final AccountRepositoryPort accountRepositoryPort;

  public GetAccountService(AccountRepositoryPort accountRepositoryPort) {
    this.accountRepositoryPort = accountRepositoryPort;
  }

  @Override
  @Transactional(readOnly = true)
  public Account getAccount(String accountNumber) {
    return accountRepositoryPort
        .findByAccountNumber(accountNumber)
        .orElseThrow(() -> new AccountNotFoundException("계좌를 찾을 수 없습니다. 계좌번호: " + accountNumber));
  }
}
