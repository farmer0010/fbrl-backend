package com.fbrl.adapter.in.batch;

import com.fbrl.application.port.in.VerifyTrialBalanceUseCase;
import com.fbrl.application.port.out.LoadAllAccountsPort;
import com.fbrl.application.port.out.SaveEodSnapshotPort;
import com.fbrl.application.service.AccountBalanceCalculator;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class EodSettlementJobConfig {
  private static final int CHUNK_SIZE = 1000;

  private static final BigDecimal FIXED_ANNUAL_RATE = BigDecimal.valueOf(0.0365);

  @Bean
  public Job eodSettlementJob(
      JobRepository jobRepository, Step eodSettlementStep, Step trialBalanceVerificationStep) {
    return new JobBuilder("eodSettlementJob", jobRepository)
        .start(eodSettlementStep)
        .next(trialBalanceVerificationStep)
        .build();
  }

  @Bean
  public Step trialBalanceVerificationStep(
      JobRepository jobRepository,
      PlatformTransactionManager transactionManager,
      VerifyTrialBalanceUseCase verifyTrialBalanceUseCase) {
    return new StepBuilder("trialBalanceVerificationStep", jobRepository)
        .tasklet(new TrialBalanceVerificationTasklet(verifyTrialBalanceUseCase), transactionManager)
        .build();
  }

  @Bean
  public Step eodSettlementStep(
      JobRepository jobRepository,
      PlatformTransactionManager transactionManager,
      AccountItemReader accountItemReader,
      AccountInterestItemProcessor accountInterestItemProcessor,
      EodSnapshotItemWriter eodSnapshotItemWriter) {
    return new StepBuilder("eodSettlementStep", jobRepository)
        .<Account, EodSnapshot>chunk(CHUNK_SIZE)
        .transactionManager(transactionManager)
        .reader(accountItemReader)
        .processor(accountInterestItemProcessor)
        .writer(eodSnapshotItemWriter)
        .build();
  }

  @Bean
  @StepScope
  public AccountItemReader accountItemReader(LoadAllAccountsPort loadAllAccountsPort) {
    return new AccountItemReader(loadAllAccountsPort);
  }

  @Bean
  @StepScope
  public AccountInterestItemProcessor accountInterestItemProcessor(
      AccountBalanceCalculator accountBalanceCalculator,
      @Value("#{jobParameters['settlementDate']}") String settlementDate) {
    InterestPolicy interestPolicy = InterestPolicy.ofAnnualRate(FIXED_ANNUAL_RATE);
    return new AccountInterestItemProcessor(
        accountBalanceCalculator, interestPolicy, LocalDate.parse(settlementDate));
  }

  @Bean
  public EodSnapshotItemWriter eodSnapshotItemWriter(SaveEodSnapshotPort saveEodSnapshotPort) {
    return new EodSnapshotItemWriter(saveEodSnapshotPort);
  }
}
