package com.fbrl.adapter.in.batch;

import static org.assertj.core.api.Assertions.assertThat;

import com.fbrl.adapter.out.persistence.AccountPersistenceAdapter;
import com.fbrl.adapter.out.persistence.EodSnapshotPersistenceAdapter;
import com.fbrl.adapter.out.persistence.LedgerEntryPersistenceAdapter;
import com.fbrl.adapter.out.persistence.demo.DemoAccountPersistenceAdapter;
import com.fbrl.adapter.out.persistence.demo.DemoEodSnapshotPersistenceAdapter;
import com.fbrl.adapter.out.persistence.demo.DemoLedgerEntryPersistenceAdapter;
import com.fbrl.domain.model.Account;
import com.fbrl.domain.model.EodSnapshot;
import com.fbrl.domain.model.LedgerEntry;
import com.fbrl.domain.model.Money;
import java.time.Instant;
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
@DisplayName("데모 EOD 정산 Job 종단 간 테스트 — 데모 DB 격리 검증")
class DemoEodSettlementJobConfigTest {

  @Autowired
  @Qualifier("demoJobOperator")
  private JobOperator demoJobOperator;

  @Autowired
  @Qualifier("demoEodSettlementJob")
  private Job demoEodSettlementJob;

  @Autowired
  @Qualifier("demoJobRepository")
  private JobRepository demoJobRepository;

  @Autowired private DemoAccountPersistenceAdapter demoAccountPersistenceAdapter;
  @Autowired private DemoLedgerEntryPersistenceAdapter demoLedgerEntryPersistenceAdapter;
  @Autowired private DemoEodSnapshotPersistenceAdapter demoEodSnapshotPersistenceAdapter;

  @Autowired private AccountPersistenceAdapter accountPersistenceAdapter;
  @Autowired private LedgerEntryPersistenceAdapter ledgerEntryPersistenceAdapter;
  @Autowired private EodSnapshotPersistenceAdapter eodSnapshotPersistenceAdapter;

  @BeforeEach
  void setUp() {
    new JobRepositoryTestUtils(demoJobRepository).removeJobExecutions();

    demoLedgerEntryPersistenceAdapter.deleteAllInBatch();
    demoAccountPersistenceAdapter.deleteAllInBatch();
    demoEodSnapshotPersistenceAdapter.deleteAllInBatch();

    ledgerEntryPersistenceAdapter.deleteAllInBatch();
    accountPersistenceAdapter.deleteAllInBatch();
    eodSnapshotPersistenceAdapter.deleteAllInBatch();

    demoAccountPersistenceAdapter.save(Account.create("DEMO-EOD-111"));
    demoLedgerEntryPersistenceAdapter.saveAll(
        LedgerEntry.transferPair(
                "DEMO-EOD-SOURCE",
                "DEMO-EOD-111",
                Money.wons(1_000_000),
                "DEMO_EOD_SEED",
                Instant.now())
            .entries());
  }

  @Test
  @DisplayName("데모 EOD 정산 결과는 데모 DB에만 기록되고 운영 DB엔 안 남는다")
  void demoEodSettlementJob_recordsSnapshotOnlyInDemoDatabase() throws Exception {
    JobParameters jobParameters =
        new JobParametersBuilder().addString("settlementDate", "2026-08-19").toJobParameters();

    JobExecution execution = demoJobOperator.start(demoEodSettlementJob, jobParameters);

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

    EodSnapshot demoSnapshot =
        demoEodSnapshotPersistenceAdapter.findLatestByAccountNumber("DEMO-EOD-111").orElseThrow();
    assertThat(demoSnapshot.settlementDate().toString()).isEqualTo("2026-08-19");
    assertThat(demoSnapshot.interestAmount().getAmount()).isPositive();

    assertThat(eodSnapshotPersistenceAdapter.findLatestByAccountNumber("DEMO-EOD-111")).isEmpty();
  }
}
