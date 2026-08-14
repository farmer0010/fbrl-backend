package com.fbrl.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fbrl.adapter.out.persistence.AccountPersistenceAdapter;
import com.fbrl.adapter.out.persistence.EodSnapshotPersistenceAdapter;
import com.fbrl.adapter.out.persistence.LedgerEntryPersistenceAdapter;
import com.fbrl.application.port.in.TransferMoneyCommand;
import com.fbrl.application.port.out.SaveLedgerEntryPort;
import com.fbrl.domain.model.Account;
import com.fbrl.domain.model.LedgerDirection;
import com.fbrl.domain.model.LedgerEntry;
import com.fbrl.domain.model.Money;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@DisplayName("송금 동시성 통합 테스트 (Redisson 분산 락)")
class TransferConcurrencyTest {

  @Autowired private TransferMoneyService transferMoneyService;

  @Autowired private AccountPersistenceAdapter accountPersistenceAdapter;

  @Autowired private LedgerEntryPersistenceAdapter ledgerEntryPersistenceAdapter;

  @Autowired private EodSnapshotPersistenceAdapter eodSnapshotPersistenceAdapter;

  @Autowired private SaveLedgerEntryPort saveLedgerEntryPort;

  @Autowired private AccountBalanceCalculator accountBalanceCalculator;

  private final String SENDER_ACCOUNT_NUMBER = "111-111";
  private final String RECEIVER_ACCOUNT_NUMBER = "222-222";

  @BeforeEach
  void setUp() {
    ledgerEntryPersistenceAdapter.deleteAllInBatch();
    eodSnapshotPersistenceAdapter.deleteAllInBatch();
    accountPersistenceAdapter.deleteAllInBatch();

    accountPersistenceAdapter.save(Account.create(SENDER_ACCOUNT_NUMBER));
    accountPersistenceAdapter.save(Account.create(RECEIVER_ACCOUNT_NUMBER));

    saveLedgerEntryPort.saveAll(
        List.of(
            LedgerEntry.of(
                SENDER_ACCOUNT_NUMBER,
                LedgerDirection.CREDIT,
                Money.wons(1_000_000),
                "TEST_SEED",
                Instant.now())));
  }

  @Test
  @DisplayName("100개의 스레드가 동시에 10,000원씩 송금할 때 단 1원의 오차 없이 잔액이 정확히 차감된다.")
  void transferConcurrently_100Threads() throws InterruptedException {

    int threadCount = 100;

    ExecutorService executorService = Executors.newFixedThreadPool(32);

    CountDownLatch latch = new CountDownLatch(threadCount);

    TransferMoneyCommand command =
        new TransferMoneyCommand(
            SENDER_ACCOUNT_NUMBER, RECEIVER_ACCOUNT_NUMBER, Money.wons(10_000));

    for (int i = 0; i < threadCount; i++) {
      executorService.submit(
          () -> {
            try {
              transferMoneyService.transfer(command);
            } finally {
              latch.countDown();
            }
          });
    }

    latch.await();

    Money senderBalance = accountBalanceCalculator.calculate(Account.create(SENDER_ACCOUNT_NUMBER));
    Money receiverBalance =
        accountBalanceCalculator.calculate(Account.create(RECEIVER_ACCOUNT_NUMBER));

    assertThat(senderBalance).isEqualTo(Money.ZERO);
    assertThat(receiverBalance).isEqualTo(Money.wons(1_000_000));
  }
}
