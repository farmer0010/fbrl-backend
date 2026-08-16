package com.fbrl.adapter.in.batch;

import com.fbrl.application.port.out.LoadAllAccountsPort;
import com.fbrl.application.port.out.LoadEodSnapshotByDatePort;
import com.fbrl.application.port.out.LoadLedgerBalanceDeltasPort;
import com.fbrl.application.port.out.SaveReconciliationDiscrepancyPort;
import com.fbrl.domain.model.Account;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class ReconciliationJobConfig {
  private static final int CHUNK_SIZE = 1000;

  @Bean
  public Job reconciliationJob(JobRepository jobRepository, Step reconciliationStep) {
    return new JobBuilder("reconciliationJob", jobRepository).start(reconciliationStep).build();
  }

  @Bean
  public Step reconciliationStep(
      JobRepository jobRepository,
      PlatformTransactionManager transactionManager,
      AccountItemReader reconciliationAccountItemReader,
      ReconciliationItemWriter reconciliationItemWriter) {
    return new StepBuilder("reconciliationStep", jobRepository)
        .<Account, Account>chunk(CHUNK_SIZE)
        .transactionManager(transactionManager)
        .reader(reconciliationAccountItemReader)
        .writer(reconciliationItemWriter)
        .build();
  }

  @Bean
  @StepScope
  public AccountItemReader reconciliationAccountItemReader(
      LoadAllAccountsPort loadAllAccountsPort) {
    return new AccountItemReader(loadAllAccountsPort);
  }

  @Bean
  @StepScope
  public ReconciliationItemWriter reconciliationItemWriter(
      LoadEodSnapshotByDatePort loadEodSnapshotByDatePort,
      LoadLedgerBalanceDeltasPort loadLedgerBalanceDeltasPort,
      SaveReconciliationDiscrepancyPort saveReconciliationDiscrepancyPort,
      @Value("#{jobParameters['settlementDate']}") String settlementDate,
      @Value("#{jobParameters['asOf']}") String asOf) {
    return new ReconciliationItemWriter(
        loadEodSnapshotByDatePort,
        loadLedgerBalanceDeltasPort,
        saveReconciliationDiscrepancyPort,
        LocalDate.parse(settlementDate),
        Instant.parse(asOf));
  }
}
