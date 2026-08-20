package com.fbrl.adapter.in.scheduler;

import java.time.Instant;
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
public class DemoReconciliationScheduler {
  private static final Logger log = LoggerFactory.getLogger(DemoReconciliationScheduler.class);

  private final JobOperator demoJobOperator;
  private final Job demoReconciliationJob;

  public DemoReconciliationScheduler(
      @Qualifier("demoJobOperator") JobOperator demoJobOperator,
      @Qualifier("demoReconciliationJob") Job demoReconciliationJob) {
    this.demoJobOperator = demoJobOperator;
    this.demoReconciliationJob = demoReconciliationJob;
  }

  public static JobParameters buildTodayJobParameters() {
    LocalDate settlementDate = LocalDate.now();
    Instant asOf = Instant.now();
    return new JobParametersBuilder()
        .addString("settlementDate", settlementDate.toString())
        .addString("asOf", asOf.toString(), false)
        .toJobParameters();
  }

  @Scheduled(cron = "${demo.reconciliation.batch.cron:0 20 3 * * *}")
  @SchedulerLock(name = "demoReconciliationJob", lockAtMostFor = "2h", lockAtLeastFor = "5m")
  public void triggerDemoReconciliation() {
    LockAssert.assertLocked();

    JobParameters jobParameters = buildTodayJobParameters();

    try {
      demoJobOperator.start(demoReconciliationJob, jobParameters);
    } catch (JobInstanceAlreadyCompleteException e) {
      log.info(
          "데모 {}자 Reconciliation은 이미 완료되어 이번 트리거는 건너뜁니다.",
          jobParameters.getString("settlementDate"));
    } catch (Exception e) {
      log.error(
          "데모 {}자 Reconciliation Job 트리거 중 오류가 발생했습니다.",
          jobParameters.getString("settlementDate"),
          e);
    }
  }
}
