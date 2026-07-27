package com.fbrl.application.service;

import com.fbrl.application.port.in.TransferMoneyCommand;
import com.fbrl.application.port.in.TransferMoneyUseCase;
import com.fbrl.application.port.out.AccountRepositoryPort;
import com.fbrl.domain.exception.AccountNotFoundException;
import com.fbrl.domain.model.Account;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TransferMoneyService implements TransferMoneyUseCase {

  private final AccountRepositoryPort accountRepositoryPort;

  public TransferMoneyService(AccountRepositoryPort accountRepositoryPort) {
    this.accountRepositoryPort = accountRepositoryPort;
  }

  @Override
  public void transfer(TransferMoneyCommand command) {
    Account senderAccount =
        accountRepositoryPort
            .findByAccountNumber(command.senderAccountNumber())
            .orElseThrow(
                () ->
                    new AccountNotFoundException(
                        "출금 계좌를 찾을 수 없습니다. 계좌번호: " + command.senderAccountNumber()));

    Account receiverAccount =
        accountRepositoryPort
            .findByAccountNumber(command.receiverAccountNumber())
            .orElseThrow(
                () ->
                    new AccountNotFoundException(
                        "입금 계좌를 찾을 수 없습니다. 계좌번호: " + command.receiverAccountNumber()));

    senderAccount.withdraw(command.money());
    receiverAccount.deposit(command.money());

    accountRepositoryPort.save(senderAccount);
    accountRepositoryPort.save(receiverAccount);
  }
}
