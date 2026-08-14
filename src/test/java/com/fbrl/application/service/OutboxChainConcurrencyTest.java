package com.fbrl.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fbrl.adapter.out.persistence.AccountPersistenceAdapter;
import com.fbrl.adapter.out.persistence.OutboxPersistenceAdapter;
import com.fbrl.application.port.in.TransferMoneyCommand;
import com.fbrl.application.port.in.VerifyAuditChainUseCase;
import com.fbrl.application.port.in.VerifyAuditChainUseCase.AuditChainVerificationResult;
import com.fbrl.domain.model.Account;
import com.fbrl.domain.model.Money;
import com.fbrl.domain.model.OutboxEvent;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@DisplayName("Outbox 해시체인 동시 삽입 통합 테스트")
class OutboxChainConcurrencyTest {

  @Autowired private TransferMoneyService transferMoneyService;
  @Autowired private AccountPersistenceAdapter accountPersistenceAdapter;
  @Autowired private OutboxPersistenceAdapter outboxPersistenceAdapter;
  @Autowired private VerifyAuditChainUseCase verifyAuditChainUseCase;

  private static final int PAIR_COUNT = 50;

  @BeforeEach
  void setUp() {
    accountPersistenceAdapter.deleteAllInBatch();
    outboxPersistenceAdapter.deleteAllInBatch();

    for (int i = 0; i < PAIR_COUNT; i++) {
      accountPersistenceAdapter.save(
          Account.create(senderAccountNumber(i), Money.of(BigDecimal.valueOf(100_000))));
      accountPersistenceAdapter.save(
          Account.create(receiverAccountNumber(i), Money.of(BigDecimal.ZERO)));
    }
  }

  @Test
  @DisplayName(
      "서로 다른 계좌쌍 50건이 동시에 송금해도(기존 계좌별 락으로는 서로 경합하지 않음) "
          + "해시체인은 끊어지거나 갈라지지 않고 정확히 50개의 항목으로 이어진다.")
  void concurrentTransfersFromDifferentAccountPairs_produceValidUnbrokenChain()
      throws InterruptedException {

    ExecutorService executorService = Executors.newFixedThreadPool(32);
    CountDownLatch latch = new CountDownLatch(PAIR_COUNT);

    for (int i = 0; i < PAIR_COUNT; i++) {
      TransferMoneyCommand command =
          new TransferMoneyCommand(
              senderAccountNumber(i),
              receiverAccountNumber(i),
              Money.of(BigDecimal.valueOf(1_000)));
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
    executorService.shutdown();

    AuditChainVerificationResult result = verifyAuditChainUseCase.verify();

    assertThat(result.valid()).isTrue();
    assertThat(result.totalEntries()).isEqualTo(PAIR_COUNT);

    List<OutboxEvent> events = outboxPersistenceAdapter.loadAllOrderedById();
    assertThat(events).hasSize(PAIR_COUNT);

    Set<String> distinctEntryHashes =
        events.stream().map(OutboxEvent::getEntryHash).collect(Collectors.toSet());
    assertThat(distinctEntryHashes).hasSize(PAIR_COUNT);

    Set<String> distinctPreviousHashes =
        events.stream().map(OutboxEvent::getPreviousHash).collect(Collectors.toSet());
    assertThat(distinctPreviousHashes).hasSize(PAIR_COUNT);
  }

  private String senderAccountNumber(int i) {
    return "SENDER-" + i;
  }

  private String receiverAccountNumber(int i) {
    return "RECEIVER-" + i;
  }
}
