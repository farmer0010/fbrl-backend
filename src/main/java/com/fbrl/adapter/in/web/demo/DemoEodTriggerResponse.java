package com.fbrl.adapter.in.web.demo;

import org.springframework.batch.core.job.JobExecution;

public record DemoEodTriggerResponse(Long jobExecutionId, String status) {
  public static DemoEodTriggerResponse from(JobExecution jobExecution) {
    return new DemoEodTriggerResponse(jobExecution.getId(), jobExecution.getStatus().toString());
  }
}
