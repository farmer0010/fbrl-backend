package com.fbrl.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fbrl.adapter.out.persistence.ApprovalPersistenceAdapter;
import com.fbrl.application.port.in.ApproveTransferUseCase.ApproveTransferCommand;
import com.fbrl.application.port.in.TransferMoneyUseCase;
import com.fbrl.application.port.out.LoadApprovalRequestPort;
import com.fbrl.domain.exception.ConcurrentApprovalModificationException;
import com.fbrl.domain.model.ApprovalStatus;
import com.fbrl.domain.model.Money;
import com.fbrl.domain.model.TransferApprovalRequest;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@DisplayName("승인 요청 동시성 통합 테스트 (JPA 낙관적 락)")
class ApproveTransferConcurrencyTest {

  @Autowired private ApproveTransferService approveTransferService;
  @Autowired private ApprovalPersistenceAdapter approvalPersistenceAdapter;
  @Autowired private LoadApprovalRequestPort loadApprovalRequestPort;

  @MockitoBean private TransferMoneyUseCase transferMoneyUseCase;

  @BeforeEach
  void setUp() {
    approvalPersistenceAdapter.deleteAllInBatch();
  }

  @Test
  @DisplayName("두 명의 Checker가 동시에 같은 요청을 승인하면 단 하나만 성공한다.")
  void approve_concurrentlyByTwoCheckers_onlyOneSucceeds() throws InterruptedException {
    TransferApprovalRequest request =
        TransferApprovalRequest.request("maker-1", "111-111", "222-222", Money.wons(20_000_000));
    approvalPersistenceAdapter.save(request);

    int threadCount = 2;
    ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);
    AtomicInteger successCount = new AtomicInteger();
    AtomicInteger conflictCount = new AtomicInteger();

    for (int i = 0; i < threadCount; i++) {
      String checkerId = "checker-" + i;
      executorService.submit(
          () -> {
            try {
              approveTransferService.approve(
                  new ApproveTransferCommand(request.getRequestId(), checkerId));
              successCount.incrementAndGet();
            } catch (ConcurrentApprovalModificationException e) {
              conflictCount.incrementAndGet();
            } finally {
              latch.countDown();
            }
          });
    }

    latch.await();
    executorService.shutdown();

    assertThat(successCount.get()).isEqualTo(1);
    assertThat(conflictCount.get()).isEqualTo(1);

    TransferApprovalRequest saved =
        loadApprovalRequestPort.loadByRequestId(request.getRequestId()).orElseThrow();
    assertThat(saved.getStatus()).isEqualTo(ApprovalStatus.APPROVED);
  }
}
