package com.fbrl.adapter.in.batch;

import com.fbrl.application.port.in.VerifyTrialBalanceUseCase;
import com.fbrl.application.port.out.LoadAllAccountsPort;
import com.fbrl.application.port.out.SaveEodSnapshotPort;
import com.fbrl.application.service.DemoAccountBalanceCalculator;
import com.fbrl.domain.model.Account;
import com.fbrl.domain.model.EodSnapshot;
import com.fbrl.domain.model.InterestPolicy;
import java.math.BigDecimal;
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
public class DemoEodSettlementJobConfig {
  private static final int CHUNK_SIZE = 1000;

  private static final BigDecimal FIXED_ANNUAL_RATE = BigDecimal.valueOf(0.0365);

  @Bean
  public Job demoEodSettlementJob(
      @Qualifier("demoJobRepository") JobRepository demoJobRepository,
      Step demoEodSettlementStep,
      Step demoTrialBalanceVerificationStep) {
    return new JobBuilder("demoEodSettlementJob", demoJobRepository)
        .start(demoEodSettlementStep)
        .next(demoTrialBalanceVerificationStep)
        .build();
  }

  @Bean
  public Step demoTrialBalanceVerificationStep(
      @Qualifier("demoJobRepository") JobRepository demoJobRepository,
      @Qualifier("demoTransactionManager") PlatformTransactionManager demoTransactionManager,
      @Qualifier("demo") VerifyTrialBalanceUseCase demoVerifyTrialBalanceUseCase) {
    return new StepBuilder("demoTrialBalanceVerificationStep", demoJobRepository)
        .tasklet(
            new TrialBalanceVerificationTasklet(demoVerifyTrialBalanceUseCase),
            demoTransactionManager)
        .build();
  }

  @Bean
  public Step demoEodSettlementStep(
      @Qualifier("demoJobRepository") JobRepository demoJobRepository,
      @Qualifier("demoTransactionManager") PlatformTransactionManager demoTransactionManager,
      @Qualifier("demoAccountItemReader") AccountItemReader demoAccountItemReader,
      @Qualifier("demoAccountInterestItemProcessor")
          DemoAccountInterestItemProcessor demoAccountInterestItemProcessor,
      @Qualifier("demoEodSnapshotItemWriter") EodSnapshotItemWriter demoEodSnapshotItemWriter) {
    return new StepBuilder("demoEodSettlementStep", demoJobRepository)
        .<Account, EodSnapshot>chunk(CHUNK_SIZE)
        .transactionManager(demoTransactionManager)
        .reader(demoAccountItemReader)
        .processor(demoAccountInterestItemProcessor)
        .writer(demoEodSnapshotItemWriter)
        .build();
  }

  @Bean
  @StepScope
  public AccountItemReader demoAccountItemReader(
      @Qualifier("demo") LoadAllAccountsPort demoLoadAllAccountsPort) {
    return new AccountItemReader(demoLoadAllAccountsPort);
  }

  @Bean
  @StepScope
  public DemoAccountInterestItemProcessor demoAccountInterestItemProcessor(
      DemoAccountBalanceCalculator demoAccountBalanceCalculator,
      @Value("#{jobParameters['settlementDate']}") String settlementDate) {
    InterestPolicy interestPolicy = InterestPolicy.ofAnnualRate(FIXED_ANNUAL_RATE);
    return new DemoAccountInterestItemProcessor(
        demoAccountBalanceCalculator, interestPolicy, LocalDate.parse(settlementDate));
  }

  @Bean
  public EodSnapshotItemWriter demoEodSnapshotItemWriter(
      @Qualifier("demo") SaveEodSnapshotPort demoSaveEodSnapshotPort) {
    return new EodSnapshotItemWriter(demoSaveEodSnapshotPort);
  }
}
