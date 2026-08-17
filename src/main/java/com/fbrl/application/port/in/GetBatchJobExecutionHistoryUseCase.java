package com.fbrl.application.port.in;

import com.fbrl.application.port.out.BatchJobExecutionSummary;
import com.fbrl.application.port.out.PagedResult;
import java.util.Objects;

public interface GetBatchJobExecutionHistoryUseCase {
  PagedResult<BatchJobExecutionSummary> getHistory(GetBatchJobExecutionHistoryQuery query);

  record GetBatchJobExecutionHistoryQuery(String jobName, int page, int size) {
    public GetBatchJobExecutionHistoryQuery {
      Objects.requireNonNull(jobName, "Job 이름은 필수입니다.");
    }
  }
}
