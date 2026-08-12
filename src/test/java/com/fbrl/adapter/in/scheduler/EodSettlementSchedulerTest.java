package com.fbrl.adapter.in.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@DisplayName("ShedLock 기반 EOD 스케줄러 중복 실행 방지 테스트")
class EodSettlementSchedulerTest {

  @Autowired private EodSettlementScheduler eodSettlementScheduler;

  @MockitoBean private JobOperator jobOperator;

  @Test
  @DisplayName("여러 인스턴스가 동시에 트리거해도 ShedLock이 실제 실행을 단 하나로 제한한다.")
  void onlyOneInstanceTriggersJob() throws Exception {
    int concurrentInstanceCount = 5;
    ExecutorService executorService = Executors.newFixedThreadPool(concurrentInstanceCount);
    CountDownLatch readyLatch = new CountDownLatch(concurrentInstanceCount);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(concurrentInstanceCount);

    for (int i = 0; i < concurrentInstanceCount; i++) {
      executorService.submit(
          () -> {
            readyLatch.countDown();
            try {
              startLatch.await();
              eodSettlementScheduler.triggerEodSettlement();
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            } finally {
              doneLatch.countDown();
            }
          });
    }

    readyLatch.await();
    startLatch.countDown();
    doneLatch.await(10, TimeUnit.SECONDS);
    executorService.shutdown();

    verify(jobOperator, times(1)).start(any(Job.class), any(JobParameters.class));
  }
}
