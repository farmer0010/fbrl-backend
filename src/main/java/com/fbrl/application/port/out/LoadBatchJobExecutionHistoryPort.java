package com.fbrl.application.port.out;

public interface LoadBatchJobExecutionHistoryPort {
  PagedResult<BatchJobExecutionSummary> recentExecutions(String jobName, int page, int size);
}
