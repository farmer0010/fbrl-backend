package com.fbrl.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fbrl.adapter.out.persistence.AccountJpaRepository;
import com.fbrl.adapter.out.persistence.AccountPersistenceAdapter;
import com.fbrl.application.port.in.TransferMoneyCommand;
import com.fbrl.domain.model.Account;
import com.fbrl.domain.model.Money;
import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("락 (Redisson 분산 락 vs 비관적 락 vs 낙관적 락) 성능 비교 테스트")
class LockComparisonTest {

  @Autowired private TransferMoneyService transferMoneyService;

  @Autowired private LockComparisonService lockComparisonService;

  @Autowired private AccountJpaRepository accountJpaRepository;

  @Autowired private AccountPersistenceAdapter accountPersistenceAdapter;

  private final String SENDER = "111-111";
  private final String RECEIVER = "222-222";
  private final int THREAD_COUNT = 100;
  private final BigDecimal AMOUNT = BigDecimal.valueOf(10_000);

  @BeforeEach
  void setUp() {
    accountJpaRepository.deleteAllInBatch();

    accountPersistenceAdapter.save(
        new Account(null, SENDER, Money.of(BigDecimal.valueOf(1_000_000)), null));

    accountPersistenceAdapter.save(new Account(null, RECEIVER, Money.of(BigDecimal.ZERO), null));
  }

  @Test
  @Order(1)
  @DisplayName("1. Redisson 분산 락 100개 동시 송금 성능 측정")
  void testRedissonDistributedLock() throws InterruptedException {

    long startTime = System.currentTimeMillis();

    ExecutorService executor = Executors.newFixedThreadPool(32);
    CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

    TransferMoneyCommand command = new TransferMoneyCommand(SENDER, RECEIVER, Money.of(AMOUNT));

    for (int i = 0; i < THREAD_COUNT; i++) {
      executor.submit(
          () -> {
            try {
              transferMoneyService.transfer(command);
            } finally {
              latch.countDown();
            }
          });
    }

    latch.await();

    long endTime = System.currentTimeMillis();

    printResult("🚀 [1. Redisson 분산 락]", endTime - startTime);

    Account sender = accountPersistenceAdapter.findByAccountNumber(SENDER).orElseThrow();

    assertThat(sender.getBalance()).isEqualTo(Money.of(BigDecimal.ZERO));
  }

  @Test
  @Order(2)
  @DisplayName("2. JPA 비관적 락(Pessimistic Lock) 100개 동시 송금 성능 측정")
  void testPessimisticLock() throws InterruptedException {

    long startTime = System.currentTimeMillis();

    ExecutorService executor = Executors.newFixedThreadPool(32);
    CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

    for (int i = 0; i < THREAD_COUNT; i++) {
      executor.submit(
          () -> {
            try {
              lockComparisonService.transferWithPessimisticLock(SENDER, RECEIVER, AMOUNT);
            } finally {
              latch.countDown();
            }
          });
    }

    latch.await();

    long endTime = System.currentTimeMillis();

    printResult("🔒 [2. JPA 비관적 락]", endTime - startTime);

    Account sender = accountPersistenceAdapter.findByAccountNumber(SENDER).orElseThrow();

    assertThat(sender.getBalance()).isEqualTo(Money.of(BigDecimal.ZERO));
  }

  @Test
  @Order(3)
  @DisplayName("3. JPA 낙관적 락(Optimistic Lock + Retry) 100개 동시 송금 성능 측정")
  void testOptimisticLock() throws InterruptedException {

    long startTime = System.currentTimeMillis();

    ExecutorService executor = Executors.newFixedThreadPool(32);
    CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

    for (int i = 0; i < THREAD_COUNT; i++) {
      executor.submit(
          () -> {
            try {
              while (true) {
                try {
                  lockComparisonService.updateBalanceWithOptimisticLock(SENDER, RECEIVER, AMOUNT);
                  break;
                } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
                  Thread.sleep(50);
                }
              }
            } catch (Exception e) {
              e.printStackTrace();
            } finally {
              latch.countDown();
            }
          });
    }

    latch.await();

    long endTime = System.currentTimeMillis();

    printResult("⚡ [3. JPA 낙관적 락]", endTime - startTime);

    Account sender = accountPersistenceAdapter.findByAccountNumber(SENDER).orElseThrow();

    assertThat(sender.getBalance()).isEqualTo(Money.of(BigDecimal.ZERO));
  }

  private void printResult(String lockType, long elapsedTime) {
    System.out.println("\n");
    System.out.println("┌────────────────────────────────────────────────────────┐");
    System.out.printf("│ %-54s │\n", lockType);
    System.out.println("├────────────────────────────────────────────────────────┤");
    System.out.printf("│  동시 요청 수  : %-37s │\n", THREAD_COUNT + " 개");
    System.out.printf("│  총 소요 시간  : %-37s │\n", elapsedTime + " ms");
    System.out.println("└────────────────────────────────────────────────────────┘");
    System.out.println("\n");
  }
}
