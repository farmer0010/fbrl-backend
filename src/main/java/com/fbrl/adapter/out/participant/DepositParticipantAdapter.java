package com.fbrl.adapter.out.participant;

import com.fbrl.application.port.out.AccountRepositoryPort;
import com.fbrl.application.port.out.DepositParticipantPort;
import com.fbrl.application.port.out.SaveLedgerEntryPort;
import com.fbrl.domain.exception.AccountNotFoundException;
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
public class DepositParticipantAdapter implements DepositParticipantPort {

  private static final Logger log = LoggerFactory.getLogger(DepositParticipantAdapter.class);

  private final AccountRepositoryPort accountRepositoryPort;
  private final SaveLedgerEntryPort saveLedgerEntryPort;

  public DepositParticipantAdapter(
      AccountRepositoryPort accountRepositoryPort, SaveLedgerEntryPort saveLedgerEntryPort) {
    this.accountRepositoryPort = accountRepositoryPort;
    this.saveLedgerEntryPort = saveLedgerEntryPort;
  }

  @Override
  @DistributedLock(key = "#accountNumber")
  public DepositResult deposit(String sagaId, String accountNumber, Money amount) {
    try {
      accountRepositoryPort
          .findByAccountNumber(accountNumber)
          .orElseThrow(
              () -> new AccountNotFoundException("입금 계좌를 찾을 수 없습니다. 계좌번호: " + accountNumber));

      LedgerEntry creditEntry =
          LedgerEntry.of(accountNumber, LedgerDirection.CREDIT, amount, sagaId, Instant.now());
      saveLedgerEntryPort.saveAll(List.of(creditEntry));

      return new DepositResult(true, null);
    } catch (AccountNotFoundException e) {
      return new DepositResult(false, e.getMessage());
    } catch (RuntimeException e) {
      log.error("sagaId={}, accountNumber={} 입금 처리 중 예기치 못한 오류 발생", sagaId, accountNumber, e);
      return new DepositResult(false, "일시적인 오류로 입금 처리에 실패했습니다.");
    }
  }
}
