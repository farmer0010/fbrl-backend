package com.fbrl.application.port.in;

import com.fbrl.domain.model.Account;
import com.fbrl.domain.model.Money;

public interface GetAccountUseCase {
  AccountDetail getAccount(String accountNumber);

  record AccountDetail(Account account, Money balance) {}
}
