package com.fbrl.application.service;

import com.fbrl.application.port.out.SaveLedgerEntryPort;
import com.fbrl.domain.model.LedgerEntry;
import com.fbrl.domain.model.LedgerEntry.LedgerEntryPair;
import com.fbrl.domain.model.Money;
import com.fbrl.domain.model.SystemAccounts;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OpeningBalanceMigrationService {

  private final SaveLedgerEntryPort saveLedgerEntryPort;

  public OpeningBalanceMigrationService(SaveLedgerEntryPort saveLedgerEntryPort) {
    this.saveLedgerEntryPort = saveLedgerEntryPort;
  }

  @Transactional
  public void seedOpeningBalances(List<LegacyAccountBalance> legacyBalances) {
    for (LegacyAccountBalance legacyBalance : legacyBalances) {
      if (legacyBalance.balance().equals(Money.ZERO)) {
        continue;
      }

      LedgerEntryPair pair =
          LedgerEntry.transferPair(
              SystemAccounts.OPENING_BALANCE_SOURCE,
              legacyBalance.accountNumber(),
              legacyBalance.balance(),
              "OPENING_BALANCE:" + legacyBalance.accountNumber(),
              Instant.now());
      saveLedgerEntryPort.saveAll(pair.entries());
    }
  }

  public record LegacyAccountBalance(String accountNumber, Money balance) {}
}
