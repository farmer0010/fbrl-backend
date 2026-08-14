package com.fbrl.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.fbrl.application.port.out.SaveLedgerEntryPort;
import com.fbrl.domain.model.Account;
import com.fbrl.domain.model.LedgerEntry;
import com.fbrl.domain.model.Money;
import java.math.BigDecimal;
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
@DisplayName("EOD 정산 Job 종단 간 테스트")
class EodSettlementJobConfigTest {

  @Autowired private JobOperatorTestUtils jobOperatorTestUtils;
  @Autowired private JobRepositoryTestUtils jobRepositoryTestUtils;
  @Autowired private Job eodSettlementJob;
  @Autowired private AccountJpaRepository accountJpaRepository;
  @Autowired private AccountPersistenceAdapter accountPersistenceAdapter;
  @Autowired private EodSnapshotJpaRepository eodSnapshotJpaRepository;
  @Autowired private LedgerEntryPersistenceAdapter ledgerEntryPersistenceAdapter;
  @Autowired private SaveLedgerEntryPort saveLedgerEntryPort;

  @BeforeEach
  void setUp() {
    ledgerEntryPersistenceAdapter.deleteAllInBatch();
    accountJpaRepository.deleteAllInBatch();
    eodSnapshotJpaRepository.deleteAllInBatch();
    jobRepositoryTestUtils.removeJobExecutions();

    jobOperatorTestUtils.setJob(eodSettlementJob);

    accountPersistenceAdapter.save(Account.create("111-111"));
    accountPersistenceAdapter.save(Account.create("222-222"));

    saveLedgerEntryPort.saveAll(
        LedgerEntry.transferPair(
                "TEST-SEED-SOURCE", "111-111", Money.wons(1_000_000), "TEST_SEED", Instant.now())
            .entries());
    saveLedgerEntryPort.saveAll(
        LedgerEntry.transferPair(
                "TEST-SEED-SOURCE", "222-222", Money.wons(2_000_000), "TEST_SEED", Instant.now())
            .entries());
  }

  @Test
  @DisplayName("계좌 전체를 읽어 이자를 계산하고 EodSnapshot으로 저장한 뒤 Job이 COMPLETED된다.")
  void executeEodSettlementJob() throws Exception {
    JobParameters jobParameters =
        new JobParametersBuilder().addString("settlementDate", "2026-08-12").toJobParameters();

    JobExecution execution = jobOperatorTestUtils.launchJob(jobParameters);

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

    List<EodSnapshotJpaEntity> snapshots = eodSnapshotJpaRepository.findAll();
    assertThat(snapshots).hasSize(2);

    EodSnapshotJpaEntity firstAccountSnapshot =
        snapshots.stream()
            .filter(s -> s.getAccountNumber().equals("111-111"))
            .findFirst()
            .orElseThrow();

    assertThat(firstAccountSnapshot.getInterestAmount())
        .isEqualByComparingTo(BigDecimal.valueOf(100));
    assertThat(firstAccountSnapshot.getSettlementDate().toString()).isEqualTo("2026-08-12");
  }
}
