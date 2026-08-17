package com.fbrl.adapter.out.batch;

import com.fbrl.application.port.out.BatchJobExecutionSummary;
import com.fbrl.application.port.out.LoadBatchJobExecutionHistoryPort;
import com.fbrl.application.port.out.PagedResult;
import java.util.List;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.stereotype.Component;

@Component
public class BatchJobExecutionHistoryAdapter implements LoadBatchJobExecutionHistoryPort {

  private final JobRepository jobRepository;

  public BatchJobExecutionHistoryAdapter(JobRepository jobRepository) {
    this.jobRepository = jobRepository;
  }

  @Override
  public PagedResult<BatchJobExecutionSummary> recentExecutions(
      String jobName, int page, int size) {
    List<JobInstance> instances = jobRepository.getJobInstances(jobName, page * size, size);
    List<BatchJobExecutionSummary> summaries =
        instances.stream()
            .flatMap(instance -> jobRepository.getJobExecutions(instance).stream())
            .map(this::toSummary)
            .toList();
    return new PagedResult<>(summaries, countInstances(jobName));
  }

  private long countInstances(String jobName) {
    try {
      return jobRepository.getJobInstanceCount(jobName);
    } catch (NoSuchJobException e) {
      return 0;
    }
  }

  private BatchJobExecutionSummary toSummary(JobExecution execution) {
    return new BatchJobExecutionSummary(
        execution.getJobInstance().getJobName(),
        execution.getStatus().name(),
        execution.getStartTime(),
        execution.getEndTime(),
        execution.getExitStatus() == null ? null : execution.getExitStatus().getExitDescription());
  }
}
