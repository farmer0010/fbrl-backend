package com.fbrl.application.service;

import com.fbrl.application.port.in.DemoTransferMoneyUseCase;
import com.fbrl.application.port.in.TransferMoneyCommand;
import com.fbrl.application.port.out.AccountRepositoryPort;
import com.fbrl.application.port.out.FraudCheckPort;
import com.fbrl.application.port.out.PayloadSerializerPort;
import com.fbrl.application.port.out.SaveLedgerEntryPort;
import com.fbrl.application.port.out.SaveOutboxEventPort;
import com.fbrl.domain.event.TransferCompletedEvent;
import com.fbrl.domain.exception.AccountNotFoundException;
import com.fbrl.domain.exception.InsufficientBalanceException;
import com.fbrl.domain.exception.ReservedAccountException;
import com.fbrl.domain.exception.SuspiciousTransferException;
import com.fbrl.domain.model.Account;
import com.fbrl.domain.model.LedgerEntry;
import com.fbrl.domain.model.LedgerEntry.LedgerEntryPair;
import com.fbrl.domain.model.Money;
import com.fbrl.domain.model.OutboxEvent;
import com.fbrl.domain.model.SystemAccounts;
import com.fbrl.global.common.annotation.DemoDistributedLock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class DemoTransferMoneyService implements DemoTransferMoneyUseCase {

  private static final String AGGREGATE_TYPE_ACCOUNT = "Account";
  private static final String EVENT_TYPE_TRANSFER_COMPLETED = "TRANSFER_COMPLETED";

  private final AccountRepositoryPort demoAccountRepositoryPort;
  private final DemoAccountBalanceCalculator demoAccountBalanceCalculator;
  private final SaveLedgerEntryPort demoSaveLedgerEntryPort;
  private final SaveOutboxEventPort demoSaveOutboxEventPort;
  private final PayloadSerializerPort payloadSerializerPort;
  private final FraudCheckPort fraudCheckPort;

  public DemoTransferMoneyService(
      @Qualifier("demo") AccountRepositoryPort demoAccountRepositoryPort,
      DemoAccountBalanceCalculator demoAccountBalanceCalculator,
      @Qualifier("demo") SaveLedgerEntryPort demoSaveLedgerEntryPort,
      @Qualifier("demo") SaveOutboxEventPort demoSaveOutboxEventPort,
      PayloadSerializerPort payloadSerializerPort,
      FraudCheckPort fraudCheckPort) {
    this.demoAccountRepositoryPort = demoAccountRepositoryPort;
    this.demoAccountBalanceCalculator = demoAccountBalanceCalculator;
    this.demoSaveLedgerEntryPort = demoSaveLedgerEntryPort;
    this.demoSaveOutboxEventPort = demoSaveOutboxEventPort;
    this.payloadSerializerPort = payloadSerializerPort;
    this.fraudCheckPort = fraudCheckPort;
  }

  @Override
  @DemoDistributedLock(key = "#command.senderAccountNumber")
  public void transfer(TransferMoneyCommand command) {
    assertNotReservedAccount(command.senderAccountNumber());
    assertNotReservedAccount(command.receiverAccountNumber());
    assertNotSuspicious(command);

    Account senderAccount =
        demoAccountRepositoryPort
            .findByAccountNumber(command.senderAccountNumber())
            .orElseThrow(
                () ->
                    new AccountNotFoundException(
                        "출금 계좌를 찾을 수 없습니다. 계좌번호: " + command.senderAccountNumber()));

    demoAccountRepositoryPort
        .findByAccountNumber(command.receiverAccountNumber())
        .orElseThrow(
            () ->
                new AccountNotFoundException(
                    "입금 계좌를 찾을 수 없습니다. 계좌번호: " + command.receiverAccountNumber()));

    Money senderBalance = demoAccountBalanceCalculator.calculate(senderAccount);
    if (!senderBalance.isGreaterThanOrEqual(command.money())) {
      throw new InsufficientBalanceException(
          "잔액이 부족합니다. 현재 잔액: " + senderBalance + ", 요청금액: " + command.money());
    }

    LedgerEntryPair pair =
        LedgerEntry.transferPair(
            command.senderAccountNumber(),
            command.receiverAccountNumber(),
            command.money(),
            UUID.randomUUID().toString(),
            Instant.now());
    demoSaveLedgerEntryPort.saveAll(pair.entries());

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
    demoSaveOutboxEventPort.save(outboxEvent);
  }

  private void assertNotReservedAccount(String accountNumber) {
    if (SystemAccounts.OPENING_BALANCE_SOURCE.equals(accountNumber)) {
      throw new ReservedAccountException("예약된 시스템 계좌는 이체 대상으로 사용할 수 없습니다. 계좌번호: " + accountNumber);
    }
  }

  private void assertNotSuspicious(TransferMoneyCommand command) {
    if (fraudCheckPort.isSuspicious(command.senderAccountNumber(), command.money())) {
      throw new SuspiciousTransferException(command.senderAccountNumber(), command.money());
    }
  }
}
