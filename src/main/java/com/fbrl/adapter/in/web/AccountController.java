package com.fbrl.adapter.in.web;

import com.fbrl.adapter.in.web.dto.AccountResponse;
import com.fbrl.application.port.in.CreateAccountUseCase;
import com.fbrl.application.port.in.GetAccountUseCase;
import com.fbrl.domain.model.Account;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {
  private final CreateAccountUseCase createAccountUseCase;
  private final GetAccountUseCase getAccountUseCase;

  public AccountController(
      CreateAccountUseCase createAccountUseCase, GetAccountUseCase getAccountUseCase) {
    this.createAccountUseCase = createAccountUseCase;
    this.getAccountUseCase = getAccountUseCase;
  }

  @PostMapping
  public ResponseEntity<AccountResponse> createAccount() {
    Account account = createAccountUseCase.createAccount();
    return ResponseEntity.created(URI.create("/api/v1/accounts/" + account.getAccountNumber()))
        .body(AccountResponse.from(account));
  }

  @GetMapping("/{accountNumber}")
  public ResponseEntity<AccountResponse> getAccount(@PathVariable String accountNumber) {
    Account account = getAccountUseCase.getAccount(accountNumber);
    return ResponseEntity.ok(AccountResponse.from(account));
  }
}
