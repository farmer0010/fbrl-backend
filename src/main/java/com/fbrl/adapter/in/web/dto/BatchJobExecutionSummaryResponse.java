package com.fbrl.adapter.in.web.dto;

import com.fbrl.application.port.out.BatchJobExecutionSummary;
import java.time.LocalDateTime;

public record BatchJobExecutionSummaryResponse(
    String jobName,
    String status,
    LocalDateTime startTime,
    LocalDateTime endTime,
    String exitDescription) {
  public static BatchJobExecutionSummaryResponse from(BatchJobExecutionSummary summary) {
    return new BatchJobExecutionSummaryResponse(
        summary.jobName(),
        summary.status(),
        summary.startTime(),
        summary.endTime(),
        summary.exitDescription());
  }
}
