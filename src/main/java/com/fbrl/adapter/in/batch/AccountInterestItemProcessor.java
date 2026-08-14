package com.fbrl.adapter.in.batch;

import com.fbrl.application.service.AccountBalanceCalculator;
import com.fbrl.domain.model.Account;
import com.fbrl.domain.model.EodSnapshot;
import com.fbrl.domain.model.InterestPolicy;
import com.fbrl.domain.model.Money;
import java.time.LocalDate;
import org.springframework.batch.infrastructure.item.ItemProcessor;

public class AccountInterestItemProcessor implements ItemProcessor<Account, EodSnapshot> {

  private final AccountBalanceCalculator accountBalanceCalculator;
  private final InterestPolicy interestPolicy;
  private final LocalDate settlementDate;

  public AccountInterestItemProcessor(
      AccountBalanceCalculator accountBalanceCalculator,
      InterestPolicy interestPolicy,
      LocalDate settlementDate) {
    this.accountBalanceCalculator = accountBalanceCalculator;
    this.interestPolicy = interestPolicy;
    this.settlementDate = settlementDate;
  }

  @Override
  public EodSnapshot process(Account account) {
    Money closingBalance = accountBalanceCalculator.calculate(account);
    Money interest = interestPolicy.calculateDailyInterest(closingBalance);
    return EodSnapshot.of(account.getAccountNumber(), closingBalance, interest, settlementDate);
  }
}
