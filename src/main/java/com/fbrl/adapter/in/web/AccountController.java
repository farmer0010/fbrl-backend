package com.fbrl.adapter.in.web;

import com.fbrl.adapter.in.web.dto.AccountResponse;
import com.fbrl.adapter.in.web.dto.EodSnapshotResponse;
import com.fbrl.adapter.in.web.dto.LedgerEntryResponse;
import com.fbrl.adapter.in.web.dto.PageResponse;
import com.fbrl.application.port.in.CreateAccountUseCase;
import com.fbrl.application.port.in.GetAccountUseCase;
import com.fbrl.application.port.in.GetAccountUseCase.AccountDetail;
import com.fbrl.application.port.in.GetEodSnapshotHistoryUseCase;
import com.fbrl.application.port.in.GetEodSnapshotHistoryUseCase.GetEodSnapshotHistoryQuery;
import com.fbrl.application.port.in.GetLedgerEntriesUseCase;
import com.fbrl.application.port.in.GetLedgerEntriesUseCase.GetLedgerEntriesQuery;
import com.fbrl.application.port.out.PagedResult;
import com.fbrl.domain.model.Account;
import com.fbrl.domain.model.EodSnapshot;
import com.fbrl.domain.model.LedgerEntry;
import com.fbrl.domain.model.Money;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {
  private final CreateAccountUseCase createAccountUseCase;
  private final GetAccountUseCase getAccountUseCase;
  private final GetLedgerEntriesUseCase getLedgerEntriesUseCase;
  private final GetEodSnapshotHistoryUseCase getEodSnapshotHistoryUseCase;

  public AccountController(
      CreateAccountUseCase createAccountUseCase,
      GetAccountUseCase getAccountUseCase,
      GetLedgerEntriesUseCase getLedgerEntriesUseCase,
      GetEodSnapshotHistoryUseCase getEodSnapshotHistoryUseCase) {
    this.createAccountUseCase = createAccountUseCase;
    this.getAccountUseCase = getAccountUseCase;
    this.getLedgerEntriesUseCase = getLedgerEntriesUseCase;
    this.getEodSnapshotHistoryUseCase = getEodSnapshotHistoryUseCase;
  }

  @PostMapping
  public ResponseEntity<AccountResponse> createAccount() {
    Account account = createAccountUseCase.createAccount();
    return ResponseEntity.created(URI.create("/api/v1/accounts/" + account.getAccountNumber()))
        .body(AccountResponse.from(account, Money.ZERO));
  }

  @GetMapping("/{accountNumber}")
  public ResponseEntity<AccountResponse> getAccount(@PathVariable String accountNumber) {
    AccountDetail detail = getAccountUseCase.getAccount(accountNumber);
    return ResponseEntity.ok(AccountResponse.from(detail.account(), detail.balance()));
  }

  @GetMapping("/{accountNumber}/ledger-entries")
  public ResponseEntity<PageResponse<LedgerEntryResponse>> getLedgerEntries(
      @PathVariable String accountNumber,
      @RequestParam Instant from,
      @RequestParam Instant to,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    PagedResult<LedgerEntry> result =
        getLedgerEntriesUseCase.getLedgerEntries(
            new GetLedgerEntriesQuery(accountNumber, from, to, page, size));
    List<LedgerEntryResponse> content =
        result.items().stream().map(LedgerEntryResponse::from).toList();
    return ResponseEntity.ok(PageResponse.of(content, result.totalElements(), page, size));
  }

  @GetMapping("/{accountNumber}/eod-snapshots")
  public ResponseEntity<PageResponse<EodSnapshotResponse>> getEodSnapshotHistory(
      @PathVariable String accountNumber,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    PagedResult<EodSnapshot> result =
        getEodSnapshotHistoryUseCase.getHistory(
            new GetEodSnapshotHistoryQuery(accountNumber, from, to, page, size));
    List<EodSnapshotResponse> content =
        result.items().stream().map(EodSnapshotResponse::from).toList();
    return ResponseEntity.ok(PageResponse.of(content, result.totalElements(), page, size));
  }
}
