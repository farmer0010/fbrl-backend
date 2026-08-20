package com.fbrl.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fbrl.adapter.out.persistence.demo.DemoAccountPersistenceAdapter;
import com.fbrl.adapter.out.persistence.demo.DemoApprovalRequestPersistenceAdapter;
import com.fbrl.adapter.out.persistence.demo.DemoEodSnapshotPersistenceAdapter;
import com.fbrl.adapter.out.persistence.demo.DemoLedgerEntryPersistenceAdapter;
import com.fbrl.adapter.out.persistence.demo.DemoOutboxPersistenceAdapter;
import com.fbrl.adapter.out.persistence.demo.DemoReconciliationDiscrepancyPersistenceAdapter;
import com.fbrl.domain.model.Account;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@DisplayName("데모 데이터 리셋 동시성 테스트 — 리셋 진행 중 조회 시 중간 상태가 노출되지 않는다")
class DemoDataResetConcurrencyTest {

  @Autowired private DemoDataResetService demoDataResetService;
  @Autowired private DemoAccountPersistenceAdapter demoAccountPersistenceAdapter;
  @Autowired private DemoLedgerEntryPersistenceAdapter demoLedgerEntryPersistenceAdapter;
  @Autowired private DemoEodSnapshotPersistenceAdapter demoEodSnapshotPersistenceAdapter;

  @Autowired
  private DemoReconciliationDiscrepancyPersistenceAdapter
      demoReconciliationDiscrepancyPersistenceAdapter;

  @Autowired private DemoApprovalRequestPersistenceAdapter demoApprovalRequestPersistenceAdapter;
  @Autowired private DemoOutboxPersistenceAdapter demoOutboxPersistenceAdapter;

  private static final int SEED_JUNK_ACCOUNT_COUNT = 5;

  @BeforeEach
  void setUp() {
    demoOutboxPersistenceAdapter.deleteAllInBatch();
    demoApprovalRequestPersistenceAdapter.deleteAllInBatch();
    demoReconciliationDiscrepancyPersistenceAdapter.deleteAllInBatch();
    demoEodSnapshotPersistenceAdapter.deleteAllInBatch();
    demoLedgerEntryPersistenceAdapter.deleteAllInBatch();
    demoAccountPersistenceAdapter.deleteAllInBatch();

    for (int i = 0; i < SEED_JUNK_ACCOUNT_COUNT; i++) {
      demoAccountPersistenceAdapter.save(Account.create("CONC-JUNK-" + i));
    }
  }

  @Test
  @DisplayName(
      "리셋 트랜잭션 진행 중 다른 스레드가 계좌 개수를 반복 조회해도, 리셋 전 개수 또는 리셋 완료 후 개수(2건)만 관측되고 중간값은 절대 관측되지 않는다.")
  void concurrentReads_neverObservePartialState() throws InterruptedException {
    int beforeCount = demoAccountPersistenceAdapter.loadAccounts(0, 1000).size();
    int afterCount = 2;

    ExecutorService executor = Executors.newFixedThreadPool(2);
    CountDownLatch resetStarted = new CountDownLatch(1);
    CountDownLatch readerDone = new CountDownLatch(1);
    AtomicBoolean resetFinished = new AtomicBoolean(false);
    Set<Integer> observedCounts = new HashSet<>();

    executor.submit(
        () -> {
          resetStarted.countDown();
          demoDataResetService.reset();
          resetFinished.set(true);
        });

    executor.submit(
        () -> {
          resetStarted.await();
          while (!resetFinished.get()) {
            int count = demoAccountPersistenceAdapter.loadAccounts(0, 1000).size();
            synchronized (observedCounts) {
              observedCounts.add(count);
            }
          }
          int finalCount = demoAccountPersistenceAdapter.loadAccounts(0, 1000).size();
          synchronized (observedCounts) {
            observedCounts.add(finalCount);
          }
          readerDone.countDown();
          return null;
        });

    readerDone.await();
    executor.shutdown();

    assertThat(observedCounts)
        .as(
            "관측된 계좌 개수는 리셋 전 개수(%d) 또는 리셋 후 개수(%d)만 존재해야 한다 — 관측값: %s",
            beforeCount, afterCount, observedCounts)
        .isSubsetOf(Set.of(beforeCount, afterCount));
    assertThat(observedCounts).contains(afterCount);
  }
}
