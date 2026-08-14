package com.fbrl.adapter.in.scheduler;

import java.time.LocalDate;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EodSettlementScheduler {
  private static final Logger log = LoggerFactory.getLogger(EodSettlementScheduler.class);

  private final JobOperator jobOperator;
  private final Job eodSettlementJob;

  public EodSettlementScheduler(JobOperator jobOperator, Job eodSettlementJob) {
    this.jobOperator = jobOperator;
    this.eodSettlementJob = eodSettlementJob;
  }

  @Scheduled(cron = "${eod.batch.cron:0 0 2 * * *}")
  @SchedulerLock(name = "eodSettlementJob", lockAtMostFor = "2h", lockAtLeastFor = "5m")
  public void triggerEodSettlement() {
    LockAssert.assertLocked();

    LocalDate settlementDate = LocalDate.now();
    JobParameters jobParameters =
        new JobParametersBuilder()
            .addString("settlementDate", settlementDate.toString())
            .toJobParameters();

    try {
      jobOperator.start(eodSettlementJob, jobParameters);
    } catch (JobInstanceAlreadyCompleteException e) {
      log.info("{}자 EOD 정산은 이미 완료되어 이번 트리거는 건너뜁니다.", settlementDate);
    } catch (Exception e) {
      log.error("{}자 EOD 정산 Job 트리거 중 오류가 발생했습니다.", settlementDate, e);
    }
  }
}
