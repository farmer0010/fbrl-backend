package com.fbrl.adapter.in.web.demo;

import com.fbrl.adapter.in.web.dto.AccountResponse;
import com.fbrl.application.port.in.CreateDemoAccountUseCase;
import com.fbrl.domain.model.Account;
import com.fbrl.domain.model.Money;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/demo/accounts")
public class DemoAccountController {
  private final CreateDemoAccountUseCase createDemoAccountUseCase;

  public DemoAccountController(CreateDemoAccountUseCase createDemoAccountUseCase) {
    this.createDemoAccountUseCase = createDemoAccountUseCase;
  }

  @PostMapping
  public ResponseEntity<AccountResponse> createAccount() {
    Account account = createDemoAccountUseCase.createAccount();
    return ResponseEntity.created(URI.create("/api/v1/demo/accounts/" + account.getAccountNumber()))
        .body(AccountResponse.from(account, Money.ZERO));
  }
}
