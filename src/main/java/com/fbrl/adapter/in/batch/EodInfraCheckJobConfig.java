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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class EodInfraCheckJobConfig {
  private static final Logger log = LoggerFactory.getLogger(EodInfraCheckJobConfig.class);

  @Bean
  public Job eodInfraCheckJob(JobRepository jobRepository, Step eodInfraCheckStep) {
    return new JobBuilder("eodInfraCheckJob", jobRepository).start(eodInfraCheckStep).build();
  }

  @Bean
  public Step eodInfraCheckStep(
      JobRepository jobRepository, PlatformTransactionManager transactionManager) {
    return new StepBuilder("eodInfraCheckStep", jobRepository)
        .tasklet(eodInfraCheckTasklet(), transactionManager)
        .build();
  }

  private Tasklet eodInfraCheckTasklet() {
    return (contribution, chunkContext) -> {
      log.info("[EOD-INFRA-CHECK] JobRepository 연동 확인용 Tasklet 실행됨");
      return RepeatStatus.FINISHED;
    };
  }
}
