package com.fbrl.adapter.in.batch;

import com.fbrl.application.port.out.LoadAllAccountsPort;
import com.fbrl.domain.model.Account;
import java.util.Iterator;
import java.util.List;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.ItemStreamReader;

public class AccountItemReader implements ItemStreamReader<Account> {
  private static final String READ_COUNT_KEY = "account.reader.readCount";
  private static final int PAGE_SIZE = 1000;

  private final LoadAllAccountsPort loadAllAccountsPort;

  private long readCount;
  private int currentPage;
  private Iterator<Account> currentPageIterator;

  public AccountItemReader(LoadAllAccountsPort loadAllAccountsPort) {
    this.loadAllAccountsPort = loadAllAccountsPort;
  }

  @Override
  public void open(ExecutionContext executionContext) throws ItemStreamException {
    this.readCount = executionContext.getLong(READ_COUNT_KEY, 0L);
    this.currentPage = (int) (readCount / PAGE_SIZE);
    this.currentPageIterator = fetchPage(currentPage);
    skipAlreadyReadItemsInPage();
  }

  @Override
  public Account read() {
    if (!currentPageIterator.hasNext()) {
      currentPage++;
      currentPageIterator = fetchPage(currentPage);
      if (!currentPageIterator.hasNext()) {
        return null;
      }
    }
    readCount++;
    return currentPageIterator.next();
  }

  @Override
  public void update(ExecutionContext executionContext) throws ItemStreamException {
    executionContext.putLong(READ_COUNT_KEY, readCount);
  }

  @Override
  public void close() throws ItemStreamException {}

  private Iterator<Account> fetchPage(int page) {
    List<Account> accounts = loadAllAccountsPort.loadAccounts(page, PAGE_SIZE);
    return accounts.iterator();
  }

  private void skipAlreadyReadItemsInPage() {
    long alreadyReadInPage = readCount % PAGE_SIZE;
    for (long i = 0; i < alreadyReadInPage && currentPageIterator.hasNext(); i++) {
      currentPageIterator.next();
    }
  }
}
