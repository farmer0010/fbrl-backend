package com.fbrl.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fbrl.adapter.out.persistence.AccountPersistenceAdapter;
import com.fbrl.adapter.out.persistence.LedgerEntryPersistenceAdapter;
import com.fbrl.adapter.out.persistence.demo.DemoAccountPersistenceAdapter;
import com.fbrl.adapter.out.persistence.demo.DemoEodSnapshotPersistenceAdapter;
import com.fbrl.adapter.out.persistence.demo.DemoLedgerEntryPersistenceAdapter;
import com.fbrl.adapter.out.persistence.demo.DemoOutboxPersistenceAdapter;
import com.fbrl.application.port.in.DemoTransferMoneyUseCase;
import com.fbrl.application.port.in.TransferMoneyCommand;
import com.fbrl.application.port.out.LoadAllOutboxEventsPort;
import com.fbrl.application.port.out.LoadLedgerEntriesPort;
import com.fbrl.application.port.out.SaveLedgerEntryPort;
import com.fbrl.domain.model.Account;
import com.fbrl.domain.model.LedgerDirection;
import com.fbrl.domain.model.LedgerEntry;
import com.fbrl.domain.model.Money;
import com.fbrl.domain.model.OutboxEvent;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@DisplayName("DemoTransferMoneyService 통합 테스트 — 운영 DB 격리 및 해시체인 무결성 검증")
class DemoTransferMoneyServiceIntegrationTest {

  @Autowired private DemoTransferMoneyUseCase demoTransferMoneyService;

  @Autowired private DemoAccountPersistenceAdapter demoAccountPersistenceAdapter;
  @Autowired private DemoLedgerEntryPersistenceAdapter demoLedgerEntryPersistenceAdapter;
  @Autowired private DemoEodSnapshotPersistenceAdapter demoEodSnapshotPersistenceAdapter;
  @Autowired private DemoOutboxPersistenceAdapter demoOutboxPersistenceAdapter;

  @Autowired
  @Qualifier("demo")
  private SaveLedgerEntryPort demoSaveLedgerEntryPort;

  @Autowired private AccountPersistenceAdapter accountPersistenceAdapter;
  @Autowired private LedgerEntryPersistenceAdapter ledgerEntryPersistenceAdapter;
  @Autowired private LoadLedgerEntriesPort loadLedgerEntriesPort;
  @Autowired private LoadAllOutboxEventsPort loadAllOutboxEventsPort;

  private final String SENDER_ACCOUNT_NUMBER = "DEMO-ISO-111";
  private final String RECEIVER_ACCOUNT_NUMBER = "DEMO-ISO-222";

  @BeforeEach
  void setUp() {
    demoLedgerEntryPersistenceAdapter.deleteAllInBatch();
    demoEodSnapshotPersistenceAdapter.deleteAllInBatch();
    demoAccountPersistenceAdapter.deleteAllInBatch();
    demoOutboxPersistenceAdapter.deleteAllInBatch();

    demoAccountPersistenceAdapter.save(Account.create(SENDER_ACCOUNT_NUMBER));
    demoAccountPersistenceAdapter.save(Account.create(RECEIVER_ACCOUNT_NUMBER));

    demoSaveLedgerEntryPort.saveAll(
        List.of(
            LedgerEntry.of(
                SENDER_ACCOUNT_NUMBER,
                LedgerDirection.CREDIT,
                Money.wons(100_000),
                "DEMO_ISO_SEED",
                Instant.now())));
  }

  @Test
  @DisplayName("데모 이체 결과(LedgerEntry+OutboxEvent)는 데모 DB에만 기록되고 운영 DB엔 전혀 없다.")
  void demoTransfer_recordsOnlyInDemoDatabase() {
    TransferMoneyCommand command =
        new TransferMoneyCommand(
            SENDER_ACCOUNT_NUMBER, RECEIVER_ACCOUNT_NUMBER, Money.wons(10_000));

    demoTransferMoneyService.transfer(command);

    List<LedgerEntry> demoEntries =
        demoLedgerEntryPersistenceAdapter.loadByAccountNumberSince(
            SENDER_ACCOUNT_NUMBER, Instant.EPOCH);
    assertThat(demoEntries).isNotEmpty();

    List<OutboxEvent> demoOutboxEvents = demoOutboxPersistenceAdapter.loadAllOrderedById();
    assertThat(demoOutboxEvents).anyMatch(e -> e.getAggregateId().equals(SENDER_ACCOUNT_NUMBER));

    assertThat(accountPersistenceAdapter.findByAccountNumber(SENDER_ACCOUNT_NUMBER)).isEmpty();
    assertThat(accountPersistenceAdapter.findByAccountNumber(RECEIVER_ACCOUNT_NUMBER)).isEmpty();
    assertThat(loadLedgerEntriesPort.loadByAccountNumberSince(SENDER_ACCOUNT_NUMBER, Instant.EPOCH))
        .isEmpty();
    assertThat(loadAllOutboxEventsPort.loadAllOrderedById())
        .noneMatch(e -> e.getAggregateId().equals(SENDER_ACCOUNT_NUMBER));
  }

  @Test
  @DisplayName("데모 이체 3건을 연속 실행한 뒤 outbox_event 체인을 재계산하면 previousHash 연결과 entryHash가 모두 무결하다.")
  void demoTransfer_multipleEntries_hashChainIsIntact() {
    TransferMoneyCommand command =
        new TransferMoneyCommand(
            SENDER_ACCOUNT_NUMBER, RECEIVER_ACCOUNT_NUMBER, Money.wons(10_000));

    demoTransferMoneyService.transfer(command);
    demoTransferMoneyService.transfer(command);
    demoTransferMoneyService.transfer(command);

    List<OutboxEvent> events = demoOutboxPersistenceAdapter.loadAllOrderedById();
    assertThat(events).hasSize(3);

    String expectedPreviousHash = OutboxEvent.GENESIS_PREVIOUS_HASH;
    for (OutboxEvent event : events) {
      assertThat(event.getPreviousHash()).isEqualTo(expectedPreviousHash);
      assertThat(event.getEntryHash()).isEqualTo(event.recomputeEntryHash());
      expectedPreviousHash = event.getEntryHash();
    }
  }
}
