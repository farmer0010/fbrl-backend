package com.fbrl.application.service;

import com.fbrl.application.port.out.LoadLatestEodSnapshotPort;
import com.fbrl.application.port.out.LoadLedgerEntriesPort;
import com.fbrl.domain.model.Account;
import com.fbrl.domain.model.EodSnapshot;
import com.fbrl.domain.model.LedgerEntry;
import com.fbrl.domain.model.Money;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AccountBalanceCalculator {

  private final LoadLatestEodSnapshotPort loadLatestEodSnapshotPort;
  private final LoadLedgerEntriesPort loadLedgerEntriesPort;

  @Transactional(readOnly = true)
  public Money calculate(Account account) {
    Optional<EodSnapshot> anchor =
        loadLatestEodSnapshotPort.findLatestByAccountNumber(account.getAccountNumber());

    Money anchorBalance = anchor.map(EodSnapshot::totalBalance).orElse(Money.ZERO);
    Instant since = anchor.map(EodSnapshot::computedAt).orElse(Instant.EPOCH);

    List<LedgerEntry> entriesSinceAnchor =
        loadLedgerEntriesPort.loadByAccountNumberSince(account.getAccountNumber(), since);

    return account.calculateBalance(anchorBalance, entriesSinceAnchor);
  }
}
