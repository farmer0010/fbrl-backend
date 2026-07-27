package com.fbrl.application.port.out;

import com.fbrl.domain.model.Account;
import java.util.Optional;

public interface AccountRepositoryPort {
  Optional<Account> findByAccountNumber(String accountNumber);

  Account save(Account account);
}
