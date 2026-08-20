package com.fbrl.adapter.in.batch;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@DisplayName("데모 Job이 데모 BATCH_* 테이블에만 기록되는지 검증")
class DemoSmokeTestJobConfigTest {

  @Autowired
  @Qualifier("demoJobOperator")
  private JobOperator demoJobOperator;

  @Autowired private Job demoSmokeTestJob;

  @Autowired
  @Qualifier("demoJobRepository")
  private JobRepository demoJobRepository;

  @Autowired private JobRepository jobRepository;

  @Test
  @DisplayName("demoSmokeTestJob 실행 이력은 데모 JobRepository에만 남고 운영 JobRepository에는 없다")
  void demoSmokeTestJob_recordsOnlyInDemoJobRepository() throws Exception {
    JobParameters jobParameters =
        new JobParametersBuilder()
            .addLong("timestamp", System.currentTimeMillis())
            .toJobParameters();

    JobExecution execution = demoJobOperator.start(demoSmokeTestJob, jobParameters);

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

    List<JobInstance> demoInstances = demoJobRepository.getJobInstances("demoSmokeTestJob", 0, 10);
    assertThat(demoInstances).isNotEmpty();

    List<JobInstance> mainInstances = jobRepository.getJobInstances("demoSmokeTestJob", 0, 10);
    assertThat(mainInstances).isEmpty();
  }
}
