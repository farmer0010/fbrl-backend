package com.fbrl.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fbrl.adapter.in.kafka.TransferEventConsumer;
import com.fbrl.adapter.out.persistence.AccountPersistenceAdapter;
import com.fbrl.adapter.out.persistence.EodSnapshotPersistenceAdapter;
import com.fbrl.adapter.out.persistence.LedgerEntryPersistenceAdapter;
import com.fbrl.adapter.out.persistence.OutboxPersistenceAdapter;
import com.fbrl.application.port.in.TransferMoneyCommand;
import com.fbrl.application.port.out.SaveLedgerEntryPort;
import com.fbrl.domain.exception.PayloadDeserializationException;
import com.fbrl.domain.model.Account;
import com.fbrl.domain.model.LedgerDirection;
import com.fbrl.domain.model.LedgerEntry;
import com.fbrl.domain.model.Money;
import com.fbrl.domain.model.OutboxEvent;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@SpringBootTest
@DisplayName("Outbox 저장부터 Consumer 처리까지 trace_id 연속성 통합 테스트")
class TransferTraceContinuityIntegrationTest {

  @TestConfiguration
  static class TracingTestConfig {
    @Bean
    InMemorySpanExporter inMemorySpanExporter() {
      return InMemorySpanExporter.create();
    }

    @Bean
    SpanProcessor inMemorySpanProcessor(InMemorySpanExporter inMemorySpanExporter) {
      return SimpleSpanProcessor.create(inMemorySpanExporter);
    }
  }

  @Autowired private TransferMoneyService transferMoneyService;
  @Autowired private AccountPersistenceAdapter accountPersistenceAdapter;
  @Autowired private OutboxPersistenceAdapter outboxPersistenceAdapter;
  @Autowired private LedgerEntryPersistenceAdapter ledgerEntryPersistenceAdapter;
  @Autowired private EodSnapshotPersistenceAdapter eodSnapshotPersistenceAdapter;
  @Autowired private SaveLedgerEntryPort saveLedgerEntryPort;
  @Autowired private TransferEventConsumer transferEventConsumer;
  @Autowired private InMemorySpanExporter inMemorySpanExporter;
  @Autowired private SdkTracerProvider sdkTracerProvider;

  private static final String SENDER = "TRACE-SENDER";
  private static final String RECEIVER = "TRACE-RECEIVER";

  @BeforeEach
  void setUp() {
    ledgerEntryPersistenceAdapter.deleteAllInBatch();
    eodSnapshotPersistenceAdapter.deleteAllInBatch();
    accountPersistenceAdapter.deleteAllInBatch();
    outboxPersistenceAdapter.deleteAllInBatch();
    inMemorySpanExporter.reset();

    accountPersistenceAdapter.save(Account.create(SENDER));
    accountPersistenceAdapter.save(Account.create(RECEIVER));
    saveLedgerEntryPort.saveAll(
        List.of(
            LedgerEntry.of(
                SENDER, LedgerDirection.CREDIT, Money.wons(100_000), "TEST_SEED", Instant.now())));
  }

  @Test
  @DisplayName(
      "이체 실행 후 outbox.save span의 trace_id가 outbox_event 컬럼에 저장되고,"
          + " Consumer가 해당 trace_id를 이어받아 같은 trace로 span을 남긴다.")
  void transfer_thenConsume_shareSameTraceId() {
    transferMoneyService.transfer(
        new TransferMoneyCommand(SENDER, RECEIVER, Money.of(BigDecimal.valueOf(1_000))));

    List<OutboxEvent> events = outboxPersistenceAdapter.loadAllOrderedById();
    assertThat(events).hasSize(1);
    OutboxEvent persisted = events.get(0);
    assertThat(persisted.getTraceId()).isNotBlank();
    assertThat(persisted.getSpanId()).isNotBlank();

    try {
      transferEventConsumer.consume(
          persisted.getPayload(), persisted.getTraceId(), persisted.getSpanId());
    } catch (PayloadDeserializationException ignored) {
    }

    sdkTracerProvider.forceFlush();
    List<SpanData> spans = inMemorySpanExporter.getFinishedSpanItems();
    assertThat(spans)
        .extracting(SpanData::getName)
        .contains("outbox.save", "transfer-event.consume");
    assertThat(spans).extracting(SpanData::getTraceId).containsOnly(persisted.getTraceId());
  }
}
