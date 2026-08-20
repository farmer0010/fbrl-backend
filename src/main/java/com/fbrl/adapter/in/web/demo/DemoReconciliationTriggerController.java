package com.fbrl.adapter.in.web.demo;

import com.fbrl.adapter.in.scheduler.DemoReconciliationScheduler;
import com.fbrl.adapter.in.web.dto.ErrorResponse;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/demo/batch-jobs/reconciliation")
public class DemoReconciliationTriggerController {

  private final JobOperator demoJobOperator;
  private final Job demoReconciliationJob;

  public DemoReconciliationTriggerController(
      @Qualifier("demoJobOperator") JobOperator demoJobOperator,
      @Qualifier("demoReconciliationJob") Job demoReconciliationJob) {
    this.demoJobOperator = demoJobOperator;
    this.demoReconciliationJob = demoReconciliationJob;
  }

  @PostMapping("/trigger")
  public ResponseEntity<?> triggerReconciliation() {
    JobParameters jobParameters = DemoReconciliationScheduler.buildTodayJobParameters();

    try {
      JobExecution execution = demoJobOperator.start(demoReconciliationJob, jobParameters);
      return ResponseEntity.ok(DemoReconciliationTriggerResponse.from(execution));
    } catch (JobInstanceAlreadyCompleteException e) {
      return ResponseEntity.status(HttpStatus.CONFLICT)
          .body(
              ErrorResponse.of(
                  "RECONCILIATION_ALREADY_SETTLED_TODAY",
                  "오늘자 정산 대사는 이미 완료되어 재실행할 수 없습니다. 이는 실제 운영 환경의 '당일 1회 재실행 방지' 안전장치가 정상 작동한 것입니다."));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ErrorResponse.of("RECONCILIATION_TRIGGER_FAILED", "데모 정산 대사 Job 실행 중 오류가 발생했습니다."));
    }
  }
}
