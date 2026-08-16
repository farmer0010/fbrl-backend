package com.fbrl.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.fbrl.application.port.out.SaveLedgerEntryPort;
import com.fbrl.domain.model.Account;
import com.fbrl.domain.model.LedgerEntry;
import com.fbrl.domain.model.Money;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.test.JobOperatorTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@SpringBatchTest
@DisplayName("Reconciliation Job 종단 간 테스트")
class ReconciliationJobConfigTest {

  @Autowired private JobOperatorTestUtils jobOperatorTestUtils;
  @Autowired private JobRepositoryTestUtils jobRepositoryTestUtils;
  @Autowired private Job reconciliationJob;
  @Autowired private AccountJpaRepository accountJpaRepository;
  @Autowired private AccountPersistenceAdapter accountPersistenceAdapter;
  @Autowired private EodSnapshotJpaRepository eodSnapshotJpaRepository;
  @Autowired private ReconciliationDiscrepancyJpaRepository reconciliationDiscrepancyJpaRepository;
  @Autowired private LedgerEntryPersistenceAdapter ledgerEntryPersistenceAdapter;
  @Autowired private SaveLedgerEntryPort saveLedgerEntryPort;

  @BeforeEach
  void setUp() {
    ledgerEntryPersistenceAdapter.deleteAllInBatch();
    accountJpaRepository.deleteAllInBatch();
    eodSnapshotJpaRepository.deleteAllInBatch();
    reconciliationDiscrepancyJpaRepository.deleteAllInBatch();
    jobRepositoryTestUtils.removeJobExecutions();

    jobOperatorTestUtils.setJob(reconciliationJob);

    accountPersistenceAdapter.save(Account.create("111-111"));

    saveLedgerEntryPort.saveAll(
        LedgerEntry.transferPair(
                "TEST-SEED-SOURCE", "111-111", Money.wons(1_000_000), "TEST_SEED", Instant.now())
            .entries());
  }

  @Test
  @DisplayName("스냅샷 없는 계좌를 NO_SNAPSHOT으로 기록하고 Job이 COMPLETED된다.")
  void executeReconciliationJob() throws Exception {
    JobParameters jobParameters =
        new JobParametersBuilder()
            .addString("settlementDate", "2026-08-16")
            .addString("asOf", Instant.now().toString(), false)
            .toJobParameters();

    JobExecution execution = jobOperatorTestUtils.launchJob(jobParameters);

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

    List<ReconciliationDiscrepancyJpaEntity> discrepancies =
        reconciliationDiscrepancyJpaRepository.findAll();
    assertThat(discrepancies).hasSize(1);
    assertThat(discrepancies.get(0).getAccountNumber()).isEqualTo("111-111");
    assertThat(discrepancies.get(0).getStatus().name()).isEqualTo("NO_SNAPSHOT");
  }
}
