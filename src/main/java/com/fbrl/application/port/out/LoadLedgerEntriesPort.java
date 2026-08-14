package com.fbrl.application.port.out;

import com.fbrl.domain.model.LedgerDirection;
import com.fbrl.domain.model.LedgerEntry;
import com.fbrl.domain.model.Money;
import java.time.Instant;
import java.util.List;

public interface LoadLedgerEntriesPort {
  List<LedgerEntry> loadByAccountNumberSince(String accountNumber, Instant since);

  Money sumAmountByDirection(LedgerDirection direction);
}
