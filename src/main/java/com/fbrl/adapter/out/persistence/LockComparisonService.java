package com.fbrl.adapter.out.persistence;

import com.fbrl.application.port.out.AccountRepositoryPort;
import com.fbrl.application.port.out.SaveLedgerEntryPort;
import com.fbrl.application.service.AccountBalanceCalculator;
import com.fbrl.domain.exception.AccountNotFoundException;
import com.fbrl.domain.exception.InsufficientBalanceException;
import com.fbrl.domain.model.Account;
import com.fbrl.domain.model.LedgerEntry;
import com.fbrl.domain.model.LedgerEntry.LedgerEntryPair;
import com.fbrl.domain.model.Money;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LockComparisonService {

  private final AccountLockAnchorJpaRepository accountLockAnchorJpaRepository;
  private final AccountRepositoryPort accountRepositoryPort;
  private final AccountBalanceCalculator accountBalanceCalculator;
  private final SaveLedgerEntryPort saveLedgerEntryPort;

  public LockComparisonService(
      AccountLockAnchorJpaRepository accountLockAnchorJpaRepository,
      AccountRepositoryPort accountRepositoryPort,
      AccountBalanceCalculator accountBalanceCalculator,
      SaveLedgerEntryPort saveLedgerEntryPort) {
    this.accountLockAnchorJpaRepository = accountLockAnchorJpaRepository;
    this.accountRepositoryPort = accountRepositoryPort;
    this.accountBalanceCalculator = accountBalanceCalculator;
    this.saveLedgerEntryPort = saveLedgerEntryPort;
  }

  @Transactional
  public void ensureAnchor(String accountNumber) {
    accountLockAnchorJpaRepository
        .findByAccountNumber(accountNumber)
        .orElseGet(
            () ->
                accountLockAnchorJpaRepository.save(new AccountLockAnchorJpaEntity(accountNumber)));
  }

  @Transactional
  public void transferWithPessimisticLock(String senderNo, String receiverNo, Money amount) {
    AccountLockAnchorJpaEntity senderAnchor =
        accountLockAnchorJpaRepository
            .findByAccountNumberWithPessimisticLock(senderNo)
            .orElseThrow();
    AccountLockAnchorJpaEntity receiverAnchor =
        accountLockAnchorJpaRepository
            .findByAccountNumberWithPessimisticLock(receiverNo)
            .orElseThrow();

    appendTransfer(senderNo, receiverNo, amount);

    senderAnchor.bumpGeneration();
    receiverAnchor.bumpGeneration();
  }

  @Transactional
  public void updateBalanceWithOptimisticLock(String senderNo, String receiverNo, Money amount) {
    AccountLockAnchorJpaEntity senderAnchor =
        accountLockAnchorJpaRepository
            .findByAccountNumberWithOptimisticLock(senderNo)
            .orElseThrow();
    AccountLockAnchorJpaEntity receiverAnchor =
        accountLockAnchorJpaRepository
            .findByAccountNumberWithOptimisticLock(receiverNo)
            .orElseThrow();

    appendTransfer(senderNo, receiverNo, amount);

    senderAnchor.bumpGeneration();
    receiverAnchor.bumpGeneration();
  }

  private void appendTransfer(String senderNo, String receiverNo, Money amount) {
    Account sender =
        accountRepositoryPort
            .findByAccountNumber(senderNo)
            .orElseThrow(() -> new AccountNotFoundException("출금 계좌를 찾을 수 없습니다. 계좌번호: " + senderNo));

    Money currentBalance = accountBalanceCalculator.calculate(sender);
    if (!currentBalance.isGreaterThanOrEqual(amount)) {
      throw new InsufficientBalanceException(
          "잔액이 부족합니다. 현재 잔액: " + currentBalance + ", 요청금액: " + amount);
    }

    LedgerEntryPair pair =
        LedgerEntry.transferPair(
            senderNo, receiverNo, amount, UUID.randomUUID().toString(), Instant.now());
    saveLedgerEntryPort.saveAll(pair.entries());
  }
}
