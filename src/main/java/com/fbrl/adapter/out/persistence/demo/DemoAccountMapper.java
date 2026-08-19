package com.fbrl.adapter.out.persistence.demo;

import com.fbrl.domain.model.Account;
import org.springframework.stereotype.Component;

@Component
public class DemoAccountMapper {
  public Account toDomain(DemoAccountEntity entity) {
    if (entity == null) {
      return null;
    }
    return Account.reconstruct(entity.getId(), entity.getAccountNumber(), entity.getVersion());
  }

  public DemoAccountEntity toEntity(Account account) {
    if (account == null) {
      return null;
    }
    return new DemoAccountEntity(account.getId(), account.getAccountNumber(), account.getVersion());
  }
}
