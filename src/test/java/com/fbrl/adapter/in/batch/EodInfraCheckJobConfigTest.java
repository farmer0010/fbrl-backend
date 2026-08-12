package com.fbrl.adapter.in.batch;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.test.JobOperatorTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBatchTest
@SpringBootTest
class EodInfraCheckJobConfigTest {

  @Autowired private JobOperatorTestUtils jobOperatorTestUtils;

  @Autowired private Job eodInfraCheckJob;

  @BeforeEach
  void setUp() {
    jobOperatorTestUtils.setJob(eodInfraCheckJob);
  }

  @Test
  void jobRepository가_정상_배선되면_Job이_COMPLETED_상태로_기록된다() throws Exception {
    JobExecution jobExecution = jobOperatorTestUtils.startJob();

    assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
  }
}
