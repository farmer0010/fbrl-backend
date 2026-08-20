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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class DemoReconciliationJobConfig {
  private static final int CHUNK_SIZE = 1000;

  @Bean
  public Job demoReconciliationJob(
      @Qualifier("demoJobRepository") JobRepository demoJobRepository,
      Step demoReconciliationStep) {
    return new JobBuilder("demoReconciliationJob", demoJobRepository)
        .start(demoReconciliationStep)
        .build();
  }

  @Bean
  public Step demoReconciliationStep(
      @Qualifier("demoJobRepository") JobRepository demoJobRepository,
      @Qualifier("demoTransactionManager") PlatformTransactionManager demoTransactionManager,
      @Qualifier("demoReconciliationAccountItemReader")
          AccountItemReader demoReconciliationAccountItemReader,
      @Qualifier("demoReconciliationItemWriter")
          ReconciliationItemWriter demoReconciliationItemWriter) {
    return new StepBuilder("demoReconciliationStep", demoJobRepository)
        .<Account, Account>chunk(CHUNK_SIZE)
        .transactionManager(demoTransactionManager)
        .reader(demoReconciliationAccountItemReader)
        .writer(demoReconciliationItemWriter)
        .build();
  }

  @Bean
  @StepScope
  public AccountItemReader demoReconciliationAccountItemReader(
      @Qualifier("demo") LoadAllAccountsPort demoLoadAllAccountsPort) {
    return new AccountItemReader(demoLoadAllAccountsPort);
  }

  @Bean
  @StepScope
  public ReconciliationItemWriter demoReconciliationItemWriter(
      @Qualifier("demo") LoadEodSnapshotByDatePort demoLoadEodSnapshotByDatePort,
      @Qualifier("demo") LoadLedgerBalanceDeltasPort demoLoadLedgerBalanceDeltasPort,
      @Qualifier("demo") SaveReconciliationDiscrepancyPort demoSaveReconciliationDiscrepancyPort,
      @Value("#{jobParameters['settlementDate']}") String settlementDate,
      @Value("#{jobParameters['asOf']}") String asOf) {
    return new ReconciliationItemWriter(
        demoLoadEodSnapshotByDatePort,
        demoLoadLedgerBalanceDeltasPort,
        demoSaveReconciliationDiscrepancyPort,
        LocalDate.parse(settlementDate),
        Instant.parse(asOf));
  }
}
