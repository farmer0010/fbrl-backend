package com.fbrl.adapter.out.batch;

import static org.assertj.core.api.Assertions.assertThat;

import com.fbrl.adapter.out.persistence.AccountPersistenceAdapter;
import com.fbrl.adapter.out.persistence.EodSnapshotPersistenceAdapter;
import com.fbrl.adapter.out.persistence.LedgerEntryPersistenceAdapter;
import com.fbrl.adapter.out.persistence.ReconciliationDiscrepancyPersistenceAdapter;
import com.fbrl.application.port.out.BatchJobExecutionSummary;
import com.fbrl.application.port.out.PagedResult;
import com.fbrl.application.port.out.SaveLedgerEntryPort;
import com.fbrl.domain.model.Account;
import com.fbrl.domain.model.LedgerEntry;
import com.fbrl.domain.model.Money;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.test.JobOperatorTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@SpringBatchTest
@DisplayName("BatchJobExecutionHistoryAdapter jobName별 분리 조회 테스트")
class BatchJobExecutionHistoryAdapterTest {

  @Autowired private BatchJobExecutionHistoryAdapter batchJobExecutionHistoryAdapter;
  @Autowired private JobOperatorTestUtils jobOperatorTestUtils;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private Job eodSettlementJob;
  @Autowired private Job reconciliationJob;
  @Autowired private AccountPersistenceAdapter accountPersistenceAdapter;
  @Autowired private LedgerEntryPersistenceAdapter ledgerEntryPersistenceAdapter;
  @Autowired private SaveLedgerEntryPort saveLedgerEntryPort;
  @Autowired private EodSnapshotPersistenceAdapter eodSnapshotPersistenceAdapter;

  @Autowired
  private ReconciliationDiscrepancyPersistenceAdapter reconciliationDiscrepancyPersistenceAdapter;

  @BeforeEach
  void setUp() {
    ledgerEntryPersistenceAdapter.deleteAllInBatch();
    accountPersistenceAdapter.deleteAllInBatch();
    eodSnapshotPersistenceAdapter.deleteAllInBatch();
    reconciliationDiscrepancyPersistenceAdapter.deleteAllInBatch();
    jdbcTemplate.execute(
        "TRUNCATE TABLE batch_job_execution_context, batch_step_execution_context, "
            + "batch_step_execution, batch_job_execution_params, batch_job_execution, "
            + "batch_job_instance RESTART IDENTITY CASCADE");

    accountPersistenceAdapter.save(Account.create("111-111"));
    saveLedgerEntryPort.saveAll(
        LedgerEntry.transferPair(
                "TEST-SEED-SOURCE", "111-111", Money.wons(1_000_000), "TEST_SEED", Instant.now())
            .entries());
  }

  @Test
  @DisplayName("jobName으로 조회하면 다른 Job의 실행 이력과 섞이지 않고 해당 Job 것만 반환한다.")
  void recentExecutions_separatesByJobName() throws Exception {
    jobOperatorTestUtils.setJob(eodSettlementJob);
    JobExecution eodExecution =
        jobOperatorTestUtils.launchJob(
            new JobParametersBuilder().addString("settlementDate", "2026-08-16").toJobParameters());
    assertThat(eodExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

    jobOperatorTestUtils.setJob(reconciliationJob);
    JobExecution reconciliationExecution =
        jobOperatorTestUtils.launchJob(
            new JobParametersBuilder()
                .addString("settlementDate", "2026-08-16")
                .addString("asOf", Instant.now().toString(), false)
                .toJobParameters());
    assertThat(reconciliationExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

    PagedResult<BatchJobExecutionSummary> eodHistory =
        batchJobExecutionHistoryAdapter.recentExecutions("eodSettlementJob", 0, 20);
    assertThat(eodHistory.totalElements()).isEqualTo(1);
    assertThat(eodHistory.items()).hasSize(1);
    assertThat(eodHistory.items().get(0).jobName()).isEqualTo("eodSettlementJob");
    assertThat(eodHistory.items().get(0).status()).isEqualTo("COMPLETED");

    PagedResult<BatchJobExecutionSummary> reconciliationHistory =
        batchJobExecutionHistoryAdapter.recentExecutions("reconciliationJob", 0, 20);
    assertThat(reconciliationHistory.totalElements()).isEqualTo(1);
    assertThat(reconciliationHistory.items()).hasSize(1);
    assertThat(reconciliationHistory.items().get(0).jobName()).isEqualTo("reconciliationJob");
  }

  @Test
  @DisplayName("한 번도 실행되지 않은 jobName을 조회하면 빈 결과를 반환한다.")
  void recentExecutions_unknownJobName_returnsEmpty() {
    PagedResult<BatchJobExecutionSummary> result =
        batchJobExecutionHistoryAdapter.recentExecutions("neverRanJob", 0, 20);

    assertThat(result.items()).isEmpty();
    assertThat(result.totalElements()).isEqualTo(0);
  }
}
