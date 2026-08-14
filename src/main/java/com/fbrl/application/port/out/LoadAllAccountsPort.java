package com.fbrl.application.port.out;

import com.fbrl.domain.model.Account;
import java.util.List;

public interface LoadAllAccountsPort {
  List<Account> loadAccounts(int page, int size);
}
