package com.fbrl.application.port.in;

import com.fbrl.domain.model.Account;

public interface GetAccountUseCase {
  Account getAccount(String accountNumber);
}
