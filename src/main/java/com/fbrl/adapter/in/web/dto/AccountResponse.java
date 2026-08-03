package com.fbrl.adapter.in.web.dto;

import com.fbrl.domain.model.Account;
import java.math.BigDecimal;

public record AccountResponse(Long id, String accountNumber, BigDecimal balance, Long version) {
  public static AccountResponse from(Account account) {
    return new AccountResponse(
        account.getId(),
        account.getAccountNumber(),
        account.getBalance().getAmount(),
        account.getVersion());
  }
}
