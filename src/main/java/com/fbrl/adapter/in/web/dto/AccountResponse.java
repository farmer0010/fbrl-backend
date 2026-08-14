package com.fbrl.adapter.in.web.dto;

import com.fbrl.domain.model.Account;
import com.fbrl.domain.model.Money;
import java.math.BigDecimal;

public record AccountResponse(Long id, String accountNumber, BigDecimal balance, Long version) {
  public static AccountResponse from(Account account, Money balance) {
    return new AccountResponse(
        account.getId(), account.getAccountNumber(), balance.getAmount(), account.getVersion());
  }
}
