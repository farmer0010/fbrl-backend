package com.fbrl.adapter.in.batch;

import com.fbrl.application.service.DemoAccountBalanceCalculator;
import com.fbrl.domain.model.Account;
import com.fbrl.domain.model.EodSnapshot;
import com.fbrl.domain.model.InterestPolicy;
import com.fbrl.domain.model.Money;
import java.time.LocalDate;
import org.springframework.batch.infrastructure.item.ItemProcessor;

public class DemoAccountInterestItemProcessor implements ItemProcessor<Account, EodSnapshot> {

  private final DemoAccountBalanceCalculator demoAccountBalanceCalculator;
  private final InterestPolicy interestPolicy;
  private final LocalDate settlementDate;

  public DemoAccountInterestItemProcessor(
      DemoAccountBalanceCalculator demoAccountBalanceCalculator,
      InterestPolicy interestPolicy,
      LocalDate settlementDate) {
    this.demoAccountBalanceCalculator = demoAccountBalanceCalculator;
    this.interestPolicy = interestPolicy;
    this.settlementDate = settlementDate;
  }

  @Override
  public EodSnapshot process(Account account) {
    Money closingBalance = demoAccountBalanceCalculator.calculate(account);
    Money interest = interestPolicy.calculateDailyInterest(closingBalance);
    return EodSnapshot.of(account.getAccountNumber(), closingBalance, interest, settlementDate);
  }
}
