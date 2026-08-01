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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@DisplayName("송금 동시성 통합 테스트 (Redisson 분산 락)")
class TransferConcurrencyTest {

  @Autowired private TransferMoneyService transferMoneyService;

  @Autowired private AccountJpaRepository accountJpaRepository;

  @Autowired private AccountPersistenceAdapter accountPersistenceAdapter;

  private final String SENDER_ACCOUNT_NUMBER = "111-111";
  private final String RECEIVER_ACCOUNT_NUMBER = "222-222";

  @BeforeEach
  void setUp() {
    accountJpaRepository.deleteAllInBatch();

    accountPersistenceAdapter.save(
        new Account(null, SENDER_ACCOUNT_NUMBER, Money.of(BigDecimal.valueOf(1_000_000)), null));

    accountPersistenceAdapter.save(
        new Account(null, RECEIVER_ACCOUNT_NUMBER, Money.of(BigDecimal.ZERO), null));
  }

  @Test
  @DisplayName("100개의 스레드가 동시에 10,000원씩 송금할 때 단 1원의 오차 없이 잔액이 정확히 차감된다.")
  void transferConcurrently_100Threads() throws InterruptedException {

    int threadCount = 100;

    ExecutorService executorService = Executors.newFixedThreadPool(32);

    CountDownLatch latch = new CountDownLatch(threadCount);

    TransferMoneyCommand command =
        new TransferMoneyCommand(
            SENDER_ACCOUNT_NUMBER, RECEIVER_ACCOUNT_NUMBER, Money.of(BigDecimal.valueOf(10_000)));

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

    Account sender =
        accountPersistenceAdapter.findByAccountNumber(SENDER_ACCOUNT_NUMBER).orElseThrow();

    Account receiver =
        accountPersistenceAdapter.findByAccountNumber(RECEIVER_ACCOUNT_NUMBER).orElseThrow();

    assertThat(sender.getBalance()).isEqualTo(Money.of(BigDecimal.ZERO));

    assertThat(receiver.getBalance()).isEqualTo(Money.of(BigDecimal.valueOf(1_000_000)));
  }
}
