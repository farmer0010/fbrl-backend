package com.fbrl.application.service;

import com.fbrl.application.port.in.TransferMoneyCommand;
import com.fbrl.application.port.in.TransferMoneyUseCase;
import com.fbrl.application.port.out.AccountRepositoryPort;
import com.fbrl.domain.exception.AccountNotFoundException;
import com.fbrl.domain.model.Account;
import com.fbrl.global.common.annotation.DistributedLock;
import org.springframework.stereotype.Service;

@Service
public class TransferMoneyService implements TransferMoneyUseCase {

  private final AccountRepositoryPort accountRepositoryPort;

  public TransferMoneyService(AccountRepositoryPort accountRepositoryPort) {
    this.accountRepositoryPort = accountRepositoryPort;
  }

  @Override
  @DistributedLock(key = "#command.senderAccountNumber")
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
