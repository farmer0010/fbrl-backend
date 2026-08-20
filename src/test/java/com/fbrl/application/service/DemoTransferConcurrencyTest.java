package com.fbrl.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fbrl.adapter.out.persistence.demo.DemoAccountPersistenceAdapter;
import com.fbrl.adapter.out.persistence.demo.DemoEodSnapshotPersistenceAdapter;
import com.fbrl.adapter.out.persistence.demo.DemoLedgerEntryPersistenceAdapter;
import com.fbrl.application.port.in.DemoTransferMoneyUseCase;
import com.fbrl.application.port.in.TransferMoneyCommand;
import com.fbrl.application.port.out.SaveLedgerEntryPort;
import com.fbrl.domain.exception.InsufficientBalanceException;
import com.fbrl.domain.model.Account;
import com.fbrl.domain.model.LedgerDirection;
import com.fbrl.domain.model.LedgerEntry;
import com.fbrl.domain.model.Money;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@DisplayName("데모 송금 동시성 통합 테스트 (Demo Redisson 분산 락, DEMO-LOCK: 네임스페이스)")
class DemoTransferConcurrencyTest {

  @Autowired private DemoTransferMoneyUseCase demoTransferMoneyService;

  @Autowired private DemoAccountPersistenceAdapter demoAccountPersistenceAdapter;

  @Autowired private DemoLedgerEntryPersistenceAdapter demoLedgerEntryPersistenceAdapter;

  @Autowired private DemoEodSnapshotPersistenceAdapter demoEodSnapshotPersistenceAdapter;

  @Autowired
  @Qualifier("demo")
  private SaveLedgerEntryPort demoSaveLedgerEntryPort;

  @Autowired private DemoAccountBalanceCalculator demoAccountBalanceCalculator;

  private final String SENDER_ACCOUNT_NUMBER = "DEMO-CONC-111";
  private final String RECEIVER_ACCOUNT_NUMBER = "DEMO-CONC-222";
  private final int THREAD_COUNT = 100;
  private final Money AMOUNT = Money.wons(10_000);
  private final Money SEED_BALANCE = Money.wons(500_000);

  @BeforeEach
  void setUp() {
    demoLedgerEntryPersistenceAdapter.deleteAllInBatch();
    demoEodSnapshotPersistenceAdapter.deleteAllInBatch();
    demoAccountPersistenceAdapter.deleteAllInBatch();

    demoAccountPersistenceAdapter.save(Account.create(SENDER_ACCOUNT_NUMBER));
    demoAccountPersistenceAdapter.save(Account.create(RECEIVER_ACCOUNT_NUMBER));

    demoSaveLedgerEntryPort.saveAll(
        List.of(
            LedgerEntry.of(
                SENDER_ACCOUNT_NUMBER,
                LedgerDirection.CREDIT,
                SEED_BALANCE,
                "DEMO_CONC_SEED",
                Instant.now())));
  }

  @Test
  @DisplayName(
      "잔액(50만원)을 초과하는 100건(건당 1만원)의 동시 이체 요청 중 정확히 잔액만큼(50건)만 성공하고 나머지 50건은 InsufficientBalanceException으로 거부된다.")
  void transferConcurrently_rejectsExcessRequestsBeyondBalance() throws InterruptedException {

    ExecutorService executorService = Executors.newFixedThreadPool(32);
    CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

    TransferMoneyCommand command =
        new TransferMoneyCommand(SENDER_ACCOUNT_NUMBER, RECEIVER_ACCOUNT_NUMBER, AMOUNT);

    AtomicInteger successCount = new AtomicInteger();
    AtomicInteger insufficientBalanceCount = new AtomicInteger();
    AtomicInteger otherFailureCount = new AtomicInteger();

    for (int i = 0; i < THREAD_COUNT; i++) {
      executorService.submit(
          () -> {
            try {
              demoTransferMoneyService.transfer(command);
              successCount.incrementAndGet();
            } catch (InsufficientBalanceException e) {
              insufficientBalanceCount.incrementAndGet();
            } catch (Exception e) {
              otherFailureCount.incrementAndGet();
            } finally {
              latch.countDown();
            }
          });
    }

    latch.await();

    int expectedSuccessCount = SEED_BALANCE.getAmount().divide(AMOUNT.getAmount()).intValue();

    assertThat(otherFailureCount.get()).isZero();
    assertThat(successCount.get()).isEqualTo(expectedSuccessCount);
    assertThat(insufficientBalanceCount.get()).isEqualTo(THREAD_COUNT - expectedSuccessCount);

    Money senderBalance =
        demoAccountBalanceCalculator.calculate(Account.create(SENDER_ACCOUNT_NUMBER));
    Money receiverBalance =
        demoAccountBalanceCalculator.calculate(Account.create(RECEIVER_ACCOUNT_NUMBER));

    assertThat(senderBalance).isEqualTo(Money.ZERO);
    assertThat(receiverBalance).isEqualTo(SEED_BALANCE);
  }
}
