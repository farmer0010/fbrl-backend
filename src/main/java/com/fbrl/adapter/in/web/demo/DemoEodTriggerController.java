package com.fbrl.adapter.in.web.demo;

import com.fbrl.adapter.in.scheduler.DemoEodSettlementScheduler;
import com.fbrl.adapter.in.web.dto.ErrorResponse;
import com.fbrl.application.port.out.DemoResetLockPort;
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
@RequestMapping("/api/v1/demo/batch-jobs/eod")
public class DemoEodTriggerController {

  private final JobOperator demoJobOperator;
  private final Job demoEodSettlementJob;
  private final DemoResetLockPort demoResetLockPort;

  public DemoEodTriggerController(
      @Qualifier("demoJobOperator") JobOperator demoJobOperator,
      @Qualifier("demoEodSettlementJob") Job demoEodSettlementJob,
      DemoResetLockPort demoResetLockPort) {
    this.demoJobOperator = demoJobOperator;
    this.demoEodSettlementJob = demoEodSettlementJob;
    this.demoResetLockPort = demoResetLockPort;
  }

  @PostMapping("/trigger")
  public ResponseEntity<?> triggerEodSettlement() {
    if (demoResetLockPort.isLocked()) {
      return ResponseEntity.status(HttpStatus.LOCKED)
          .body(
              ErrorResponse.of("DEMO_RESET_IN_PROGRESS", "데모 데이터 리셋이 진행 중이라 지금은 배치를 실행할 수 없습니다."));
    }

    JobParameters jobParameters = DemoEodSettlementScheduler.buildTodayJobParameters();

    try {
      JobExecution execution = demoJobOperator.start(demoEodSettlementJob, jobParameters);
      return ResponseEntity.ok(DemoEodTriggerResponse.from(execution));
    } catch (JobInstanceAlreadyCompleteException e) {
      return ResponseEntity.status(HttpStatus.CONFLICT)
          .body(
              ErrorResponse.of(
                  "EOD_ALREADY_SETTLED_TODAY",
                  "오늘자 EOD 정산은 이미 완료되어 재실행할 수 없습니다. 이는 실제 운영 환경의 '당일 1회 재실행 방지' 안전장치가 정상 작동한 것입니다."));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ErrorResponse.of("EOD_TRIGGER_FAILED", "데모 EOD 정산 Job 실행 중 오류가 발생했습니다."));
    }
  }
}
