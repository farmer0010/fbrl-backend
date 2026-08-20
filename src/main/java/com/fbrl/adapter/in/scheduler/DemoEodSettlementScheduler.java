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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DemoEodSettlementScheduler {
  private static final Logger log = LoggerFactory.getLogger(DemoEodSettlementScheduler.class);

  private final JobOperator demoJobOperator;
  private final Job demoEodSettlementJob;

  public DemoEodSettlementScheduler(
      @Qualifier("demoJobOperator") JobOperator demoJobOperator,
      @Qualifier("demoEodSettlementJob") Job demoEodSettlementJob) {
    this.demoJobOperator = demoJobOperator;
    this.demoEodSettlementJob = demoEodSettlementJob;
  }

  public static JobParameters buildTodayJobParameters() {
    return new JobParametersBuilder()
        .addString("settlementDate", LocalDate.now().toString())
        .toJobParameters();
  }

  @Scheduled(cron = "${demo.eod.batch.cron:0 10 2 * * *}")
  @SchedulerLock(name = "demoEodSettlementJob", lockAtMostFor = "2h", lockAtLeastFor = "5m")
  public void triggerDemoEodSettlement() {
    LockAssert.assertLocked();

    JobParameters jobParameters = buildTodayJobParameters();

    try {
      demoJobOperator.start(demoEodSettlementJob, jobParameters);
    } catch (JobInstanceAlreadyCompleteException e) {
      log.info("데모 {}자 EOD 정산은 이미 완료되어 이번 트리거는 건너뜁니다.", jobParameters.getString("settlementDate"));
    } catch (Exception e) {
      log.error(
          "데모 {}자 EOD 정산 Job 트리거 중 오류가 발생했습니다.", jobParameters.getString("settlementDate"), e);
    }
  }
}
