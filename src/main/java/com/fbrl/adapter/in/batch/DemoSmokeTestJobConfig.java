package com.fbrl.adapter.in.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class DemoSmokeTestJobConfig {
  private static final Logger log = LoggerFactory.getLogger(DemoSmokeTestJobConfig.class);

  @Bean
  public Job demoSmokeTestJob(
      @Qualifier("demoJobRepository") JobRepository demoJobRepository, Step demoSmokeTestStep) {
    return new JobBuilder("demoSmokeTestJob", demoJobRepository).start(demoSmokeTestStep).build();
  }

  @Bean
  public Step demoSmokeTestStep(
      @Qualifier("demoJobRepository") JobRepository demoJobRepository,
      @Qualifier("demoTransactionManager") PlatformTransactionManager demoTransactionManager) {
    return new StepBuilder("demoSmokeTestStep", demoJobRepository)
        .tasklet(demoSmokeTestTasklet(), demoTransactionManager)
        .build();
  }

  private Tasklet demoSmokeTestTasklet() {
    return (contribution, chunkContext) -> {
      log.info("[DEMO-SMOKE-TEST] 데모 JobRepository 연동 확인용 Tasklet 실행됨");
      return RepeatStatus.FINISHED;
    };
  }
}
