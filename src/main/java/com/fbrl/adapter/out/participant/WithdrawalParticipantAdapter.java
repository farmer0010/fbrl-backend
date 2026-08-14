package com.fbrl.adapter.out.participant;

import com.fbrl.application.port.out.AccountRepositoryPort;
import com.fbrl.application.port.out.SaveLedgerEntryPort;
import com.fbrl.application.port.out.WithdrawalParticipantPort;
import com.fbrl.application.service.AccountBalanceCalculator;
import com.fbrl.domain.exception.AccountNotFoundException;
import com.fbrl.domain.exception.InsufficientBalanceException;
import com.fbrl.domain.model.Account;
import com.fbrl.domain.model.LedgerDirection;
import com.fbrl.domain.model.LedgerEntry;
import com.fbrl.domain.model.Money;
import com.fbrl.global.common.annotation.DistributedLock;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class WithdrawalParticipantAdapter implements WithdrawalParticipantPort {

  private static final Logger log = LoggerFactory.getLogger(WithdrawalParticipantAdapter.class);

  private final AccountRepositoryPort accountRepositoryPort;
  private final AccountBalanceCalculator accountBalanceCalculator;
  private final SaveLedgerEntryPort saveLedgerEntryPort;

  public WithdrawalParticipantAdapter(
      AccountRepositoryPort accountRepositoryPort,
      AccountBalanceCalculator accountBalanceCalculator,
      SaveLedgerEntryPort saveLedgerEntryPort) {
    this.accountRepositoryPort = accountRepositoryPort;
    this.accountBalanceCalculator = accountBalanceCalculator;
    this.saveLedgerEntryPort = saveLedgerEntryPort;
  }

  @Override
  @DistributedLock(key = "#accountNumber")
  public WithdrawalResult withdraw(String sagaId, String accountNumber, Money amount) {
    try {
      Account account =
          accountRepositoryPort
              .findByAccountNumber(accountNumber)
              .orElseThrow(
                  () -> new AccountNotFoundException("출금 계좌를 찾을 수 없습니다. 계좌번호: " + accountNumber));

      Money currentBalance = accountBalanceCalculator.calculate(account);
      if (!currentBalance.isGreaterThanOrEqual(amount)) {
        throw new InsufficientBalanceException(
            "잔액이 부족합니다. 현재 잔액: " + currentBalance + ", 요청금액: " + amount);
      }

      LedgerEntry debitEntry =
          LedgerEntry.of(accountNumber, LedgerDirection.DEBIT, amount, sagaId, Instant.now());
      saveLedgerEntryPort.saveAll(List.of(debitEntry));

      return new WithdrawalResult(true, null);
    } catch (AccountNotFoundException | InsufficientBalanceException e) {
      return new WithdrawalResult(false, e.getMessage());
    } catch (RuntimeException e) {
      log.error("sagaId={}, accountNumber={} 출금 처리 중 예기치 못한 오류 발생", sagaId, accountNumber, e);
      return new WithdrawalResult(false, "일시적인 오류로 출금 처리에 실패했습니다.");
    }
  }
}
