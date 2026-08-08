package com.fbrl.application.service;

import com.fbrl.application.port.in.TransferMoneyCommand;
import com.fbrl.application.port.in.TransferMoneyUseCase;
import com.fbrl.application.port.out.AccountRepositoryPort;
import com.fbrl.application.port.out.PayloadSerializerPort;
import com.fbrl.application.port.out.SaveOutboxEventPort;
import com.fbrl.domain.event.TransferCompletedEvent;
import com.fbrl.domain.exception.AccountNotFoundException;
import com.fbrl.domain.model.Account;
import com.fbrl.domain.model.OutboxEvent;
import com.fbrl.global.common.annotation.DistributedLock;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class TransferMoneyService implements TransferMoneyUseCase {

  private static final String AGGREGATE_TYPE_ACCOUNT = "Account";
  private static final String EVENT_TYPE_TRANSFER_COMPLETED = "TRANSFER_COMPLETED";

  private final AccountRepositoryPort accountRepositoryPort;
  private final SaveOutboxEventPort saveOutboxEventPort;
  private final PayloadSerializerPort payloadSerializerPort;

  public TransferMoneyService(
      AccountRepositoryPort accountRepositoryPort,
      SaveOutboxEventPort saveOutboxEventPort,
      PayloadSerializerPort payloadSerializerPort) {
    this.accountRepositoryPort = accountRepositoryPort;
    this.saveOutboxEventPort = saveOutboxEventPort;
    this.payloadSerializerPort = payloadSerializerPort;
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

    TransferCompletedEvent event =
        new TransferCompletedEvent(
            command.senderAccountNumber(),
            command.receiverAccountNumber(),
            command.money(),
            Instant.now());
    String payload = payloadSerializerPort.serialize(event);

    OutboxEvent outboxEvent =
        OutboxEvent.create(
            AGGREGATE_TYPE_ACCOUNT,
            command.senderAccountNumber(),
            EVENT_TYPE_TRANSFER_COMPLETED,
            payload);
    saveOutboxEventPort.save(outboxEvent);
  }
}
