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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReconciliationScheduler {
  private static final Logger log = LoggerFactory.getLogger(ReconciliationScheduler.class);

  private final JobOperator jobOperator;
  private final Job reconciliationJob;

  public ReconciliationScheduler(JobOperator jobOperator, Job reconciliationJob) {
    this.jobOperator = jobOperator;
    this.reconciliationJob = reconciliationJob;
  }

  @Scheduled(cron = "${reconciliation.batch.cron:0 0 3 * * *}")
  @SchedulerLock(name = "reconciliationJob", lockAtMostFor = "2h", lockAtLeastFor = "5m")
  public void triggerReconciliation() {
    LockAssert.assertLocked();

    LocalDate settlementDate = LocalDate.now();
    Instant asOf = Instant.now();
    JobParameters jobParameters =
        new JobParametersBuilder()
            .addString("settlementDate", settlementDate.toString())
            .addString("asOf", asOf.toString(), false)
            .toJobParameters();

    try {
      jobOperator.start(reconciliationJob, jobParameters);
    } catch (JobInstanceAlreadyCompleteException e) {
      log.info("{}자 Reconciliation은 이미 완료되어 이번 트리거는 건너뜁니다.", settlementDate);
    } catch (Exception e) {
      log.error("{}자 Reconciliation Job 트리거 중 오류가 발생했습니다.", settlementDate, e);
    }
  }
}
