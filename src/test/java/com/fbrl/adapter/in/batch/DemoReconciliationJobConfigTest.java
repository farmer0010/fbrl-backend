package com.fbrl.adapter.in.batch;

import static org.assertj.core.api.Assertions.assertThat;

import com.fbrl.adapter.out.persistence.AccountPersistenceAdapter;
import com.fbrl.adapter.out.persistence.EodSnapshotPersistenceAdapter;
import com.fbrl.adapter.out.persistence.LedgerEntryPersistenceAdapter;
import com.fbrl.adapter.out.persistence.ReconciliationDiscrepancyPersistenceAdapter;
import com.fbrl.adapter.out.persistence.demo.DemoAccountPersistenceAdapter;
import com.fbrl.adapter.out.persistence.demo.DemoEodSnapshotPersistenceAdapter;
import com.fbrl.adapter.out.persistence.demo.DemoLedgerEntryPersistenceAdapter;
import com.fbrl.adapter.out.persistence.demo.DemoReconciliationDiscrepancyPersistenceAdapter;
import com.fbrl.domain.model.Account;
import com.fbrl.domain.model.EodSnapshot;
import com.fbrl.domain.model.LedgerEntry;
import com.fbrl.domain.model.Money;
import com.fbrl.domain.model.ReconciliationDiscrepancy;
import com.fbrl.domain.model.ReconciliationStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@DisplayName("데모 Reconciliation Job 종단 간 테스트 — 데모 DB 격리 검증")
class DemoReconciliationJobConfigTest {

  @Autowired
  @Qualifier("demoJobOperator")
  private JobOperator demoJobOperator;

  @Autowired
  @Qualifier("demoReconciliationJob")
  private Job demoReconciliationJob;

  @Autowired
  @Qualifier("demoJobRepository")
  private JobRepository demoJobRepository;

  @Autowired private DemoAccountPersistenceAdapter demoAccountPersistenceAdapter;
  @Autowired private DemoLedgerEntryPersistenceAdapter demoLedgerEntryPersistenceAdapter;
  @Autowired private DemoEodSnapshotPersistenceAdapter demoEodSnapshotPersistenceAdapter;

  @Autowired
  private DemoReconciliationDiscrepancyPersistenceAdapter
      demoReconciliationDiscrepancyPersistenceAdapter;

  @Autowired private AccountPersistenceAdapter accountPersistenceAdapter;
  @Autowired private LedgerEntryPersistenceAdapter ledgerEntryPersistenceAdapter;
  @Autowired private EodSnapshotPersistenceAdapter eodSnapshotPersistenceAdapter;

  @Autowired
  private ReconciliationDiscrepancyPersistenceAdapter reconciliationDiscrepancyPersistenceAdapter;

  @BeforeEach
  void setUp() {
    new JobRepositoryTestUtils(demoJobRepository).removeJobExecutions();

    demoLedgerEntryPersistenceAdapter.deleteAllInBatch();
    demoAccountPersistenceAdapter.deleteAllInBatch();
    demoEodSnapshotPersistenceAdapter.deleteAllInBatch();
    demoReconciliationDiscrepancyPersistenceAdapter.deleteAllInBatch();

    ledgerEntryPersistenceAdapter.deleteAllInBatch();
    accountPersistenceAdapter.deleteAllInBatch();
    eodSnapshotPersistenceAdapter.deleteAllInBatch();
    reconciliationDiscrepancyPersistenceAdapter.deleteAllInBatch();

    demoAccountPersistenceAdapter.save(Account.create("DEMO-RECON-111"));
    demoLedgerEntryPersistenceAdapter.saveAll(
        LedgerEntry.transferPair(
                "DEMO-RECON-SOURCE",
                "DEMO-RECON-111",
                Money.wons(1_000_000),
                "DEMO_RECON_SEED",
                Instant.now())
            .entries());
  }

  @Test
  @DisplayName("스냅샷 없는 데모 계좌는 NO_SNAPSHOT으로 데모 DB에만 기록되고 운영 DB엔 없다")
  void demoReconciliationJob_noSnapshotCase_recordsOnlyInDemoDatabase() throws Exception {
    JobParameters jobParameters =
        new JobParametersBuilder()
            .addString("settlementDate", "2026-08-20")
            .addString("asOf", Instant.now().toString(), false)
            .toJobParameters();

    JobExecution execution = demoJobOperator.start(demoReconciliationJob, jobParameters);

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

    List<ReconciliationDiscrepancy> demoDiscrepancies =
        demoReconciliationDiscrepancyPersistenceAdapter.findAll();
    assertThat(demoDiscrepancies).hasSize(1);
    assertThat(demoDiscrepancies.get(0).accountNumber()).isEqualTo("DEMO-RECON-111");
    assertThat(demoDiscrepancies.get(0).status()).isEqualTo(ReconciliationStatus.NO_SNAPSHOT);

    assertThat(
            reconciliationDiscrepancyPersistenceAdapter
                .search(null, LocalDate.of(2000, 1, 1), LocalDate.of(2099, 12, 31), 0, 20)
                .items())
        .noneMatch(d -> d.accountNumber().equals("DEMO-RECON-111"));
  }

  @Test
  @DisplayName("스냅샷과 실제 원장이 다른 데모 계좌는 MISMATCH로 데모 DB에만 기록되고 운영 DB엔 없다")
  void demoReconciliationJob_mismatchCase_recordsOnlyInDemoDatabase() throws Exception {
    demoEodSnapshotPersistenceAdapter.saveAll(
        List.of(
            EodSnapshot.of(
                "DEMO-RECON-111",
                Money.wons(900_000),
                Money.wons(5_000),
                LocalDate.of(2026, 8, 20))));

    JobParameters jobParameters =
        new JobParametersBuilder()
            .addString("settlementDate", "2026-08-20")
            .addString("asOf", Instant.now().toString(), false)
            .toJobParameters();

    JobExecution execution = demoJobOperator.start(demoReconciliationJob, jobParameters);

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

    List<ReconciliationDiscrepancy> demoDiscrepancies =
        demoReconciliationDiscrepancyPersistenceAdapter.findAll();
    assertThat(demoDiscrepancies).hasSize(1);
    assertThat(demoDiscrepancies.get(0).accountNumber()).isEqualTo("DEMO-RECON-111");
    assertThat(demoDiscrepancies.get(0).status()).isEqualTo(ReconciliationStatus.MISMATCH);
    assertThat(demoDiscrepancies.get(0).expectedBalance().getAmount())
        .isEqualByComparingTo("900000");
    assertThat(demoDiscrepancies.get(0).actualBalance().getAmount())
        .isEqualByComparingTo("1000000");

    assertThat(
            reconciliationDiscrepancyPersistenceAdapter
                .search(null, LocalDate.of(2000, 1, 1), LocalDate.of(2099, 12, 31), 0, 20)
                .items())
        .noneMatch(d -> d.accountNumber().equals("DEMO-RECON-111"));
  }
}
