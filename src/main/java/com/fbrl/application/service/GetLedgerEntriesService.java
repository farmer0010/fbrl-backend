package com.fbrl.application.service;

import com.fbrl.application.port.in.GetLedgerEntriesUseCase;
import com.fbrl.application.port.out.LoadLedgerEntriesPort;
import com.fbrl.application.port.out.PagedResult;
import com.fbrl.domain.model.LedgerEntry;
import org.springframework.stereotype.Service;

@Service
public class GetLedgerEntriesService implements GetLedgerEntriesUseCase {

  private final LoadLedgerEntriesPort loadLedgerEntriesPort;

  public GetLedgerEntriesService(LoadLedgerEntriesPort loadLedgerEntriesPort) {
    this.loadLedgerEntriesPort = loadLedgerEntriesPort;
  }

  @Override
  public PagedResult<LedgerEntry> getLedgerEntries(GetLedgerEntriesQuery query) {
    return loadLedgerEntriesPort.loadByAccountNumberAndPeriod(
        query.accountNumber(), query.from(), query.to(), query.page(), query.size());
  }
}
