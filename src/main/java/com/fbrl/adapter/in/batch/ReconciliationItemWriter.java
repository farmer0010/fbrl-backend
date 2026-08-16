package com.fbrl.adapter.in.batch;

import com.fbrl.application.port.out.LoadEodSnapshotByDatePort;
import com.fbrl.application.port.out.LoadLedgerBalanceDeltasPort;
import com.fbrl.application.port.out.SaveReconciliationDiscrepancyPort;
import com.fbrl.domain.model.Account;
import com.fbrl.domain.model.EodSnapshot;
import com.fbrl.domain.model.LedgerBalanceDelta;
import com.fbrl.domain.model.Money;
import com.fbrl.domain.model.ReconciliationDiscrepancy;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;

public class ReconciliationItemWriter implements ItemWriter<Account> {

  private final LoadEodSnapshotByDatePort loadEodSnapshotByDatePort;
  private final LoadLedgerBalanceDeltasPort loadLedgerBalanceDeltasPort;
  private final SaveReconciliationDiscrepancyPort saveReconciliationDiscrepancyPort;
  private final LocalDate settlementDate;
  private final Instant asOf;

  public ReconciliationItemWriter(
      LoadEodSnapshotByDatePort loadEodSnapshotByDatePort,
      LoadLedgerBalanceDeltasPort loadLedgerBalanceDeltasPort,
      SaveReconciliationDiscrepancyPort saveReconciliationDiscrepancyPort,
      LocalDate settlementDate,
      Instant asOf) {
    this.loadEodSnapshotByDatePort = loadEodSnapshotByDatePort;
    this.loadLedgerBalanceDeltasPort = loadLedgerBalanceDeltasPort;
    this.saveReconciliationDiscrepancyPort = saveReconciliationDiscrepancyPort;
    this.settlementDate = settlementDate;
    this.asOf = asOf;
  }

  @Override
  public void write(Chunk<? extends Account> chunk) {
    List<String> accountNumbers = chunk.getItems().stream().map(Account::getAccountNumber).toList();

    Map<String, EodSnapshot> snapshotsByAccountNumber =
        loadEodSnapshotByDatePort.loadByAccountNumbersAndDate(accountNumbers, settlementDate);

    List<String> accountNumbersWithSnapshot =
        accountNumbers.stream().filter(snapshotsByAccountNumber::containsKey).toList();

    Map<String, LedgerBalanceDelta> deltasByAccountNumber =
        accountNumbersWithSnapshot.isEmpty()
            ? Map.of()
            : loadLedgerBalanceDeltasPort.loadBalanceDeltasUntil(accountNumbersWithSnapshot, asOf);

    List<ReconciliationDiscrepancy> discrepancies = new ArrayList<>();
    for (String accountNumber : accountNumbers) {
      EodSnapshot snapshot = snapshotsByAccountNumber.get(accountNumber);
      if (snapshot == null) {
        discrepancies.add(ReconciliationDiscrepancy.noSnapshot(accountNumber, settlementDate));
        continue;
      }

      LedgerBalanceDelta delta = deltasByAccountNumber.get(accountNumber);
      Money actualBalance =
          Money.of(delta.creditTotal().getAmount().subtract(delta.debitTotal().getAmount()));
      Money expectedBalance = snapshot.closingBalance();

      if (!expectedBalance.equals(actualBalance)) {
        discrepancies.add(
            ReconciliationDiscrepancy.mismatch(
                accountNumber, settlementDate, expectedBalance, actualBalance));
      }
    }

    if (!discrepancies.isEmpty()) {
      saveReconciliationDiscrepancyPort.saveAll(discrepancies);
    }
  }
}
