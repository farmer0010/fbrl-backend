package com.fbrl.adapter.in.web.demo;

import org.springframework.batch.core.job.JobExecution;

public record DemoReconciliationTriggerResponse(Long jobExecutionId, String status) {
  public static DemoReconciliationTriggerResponse from(JobExecution jobExecution) {
    return new DemoReconciliationTriggerResponse(
        jobExecution.getId(), jobExecution.getStatus().toString());
  }
}
