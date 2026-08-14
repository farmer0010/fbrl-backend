package com.fbrl.application.port.out;

import com.fbrl.domain.model.LedgerEntry;
import java.util.List;

public interface SaveLedgerEntryPort {
  List<LedgerEntry> saveAll(List<LedgerEntry> entries);
}
