package com.fbrl.adapter.in.batch;

import com.fbrl.domain.model.Account;
import com.fbrl.domain.model.EodSnapshot;
import com.fbrl.domain.model.InterestPolicy;
import com.fbrl.domain.model.Money;
import java.time.LocalDate;
import org.springframework.batch.infrastructure.item.ItemProcessor;

public class AccountInterestItemProcessor implements ItemProcessor<Account, EodSnapshot> {

  private final InterestPolicy interestPolicy;
  private final LocalDate settlementDate;

  public AccountInterestItemProcessor(InterestPolicy interestPolicy, LocalDate settlementDate) {
    this.interestPolicy = interestPolicy;
    this.settlementDate = settlementDate;
  }

  @Override
  public EodSnapshot process(Account account) {
    Money interest = interestPolicy.calculateDailyInterest(account.getBalance());
    return EodSnapshot.of(
        account.getAccountNumber(), account.getBalance(), interest, settlementDate);
  }
}
