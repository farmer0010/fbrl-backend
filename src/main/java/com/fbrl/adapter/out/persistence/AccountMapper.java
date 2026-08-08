package com.fbrl.adapter.out.persistence;

import com.fbrl.domain.model.Account;
import com.fbrl.domain.model.Money;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {
  public Account toDomain(AccountEntity entity) {
    if (entity == null) {
      return null;
    }
    return Account.reconstruct(
        entity.getId(),
        entity.getAccountNumber(),
        Money.of(entity.getBalance()),
        entity.getVersion());
  }

  public AccountEntity toEntity(Account account) {
    if (account == null) {
      return null;
    }
    return new AccountEntity(
        account.getId(),
        account.getAccountNumber(),
        account.getBalance().getAmount(),
        account.getVersion());
  }
}
