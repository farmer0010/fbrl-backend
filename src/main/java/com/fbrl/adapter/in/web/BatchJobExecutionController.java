package com.fbrl.adapter.in.web;

import com.fbrl.adapter.in.web.dto.BatchJobExecutionSummaryResponse;
import com.fbrl.adapter.in.web.dto.PageResponse;
import com.fbrl.application.port.in.GetBatchJobExecutionHistoryUseCase;
import com.fbrl.application.port.in.GetBatchJobExecutionHistoryUseCase.GetBatchJobExecutionHistoryQuery;
import com.fbrl.application.port.out.BatchJobExecutionSummary;
import com.fbrl.application.port.out.PagedResult;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/batch-jobs")
public class BatchJobExecutionController {

  private final GetBatchJobExecutionHistoryUseCase getBatchJobExecutionHistoryUseCase;

  public BatchJobExecutionController(
      GetBatchJobExecutionHistoryUseCase getBatchJobExecutionHistoryUseCase) {
    this.getBatchJobExecutionHistoryUseCase = getBatchJobExecutionHistoryUseCase;
  }

  @GetMapping("/{jobName}/executions")
  public ResponseEntity<PageResponse<BatchJobExecutionSummaryResponse>> getExecutions(
      @PathVariable String jobName,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    PagedResult<BatchJobExecutionSummary> result =
        getBatchJobExecutionHistoryUseCase.getHistory(
            new GetBatchJobExecutionHistoryQuery(jobName, page, size));
    List<BatchJobExecutionSummaryResponse> content =
        result.items().stream().map(BatchJobExecutionSummaryResponse::from).toList();
    return ResponseEntity.ok(PageResponse.of(content, result.totalElements(), page, size));
  }
}
