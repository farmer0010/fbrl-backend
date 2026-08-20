package com.fbrl.adapter.out.persistence.demo;

import com.fbrl.application.port.out.DemoOutboxTamperPort;
import com.fbrl.application.port.out.LoadAllOutboxEventsPort;
import com.fbrl.application.port.out.LoadOutboxEventsPort;
import com.fbrl.application.port.out.PagedResult;
import com.fbrl.application.port.out.SaveOutboxEventPort;
import com.fbrl.domain.model.OutboxEvent;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
@Qualifier("demo")
public class DemoOutboxPersistenceAdapter
    implements SaveOutboxEventPort,
        LoadAllOutboxEventsPort,
        LoadOutboxEventsPort,
        DemoOutboxTamperPort {
  private static final String SPAN_NAME = "demo.outbox.save";

  private final DemoOutboxEventJpaRepository demoOutboxEventJpaRepository;
  private final DemoOutboxChainTailJpaRepository demoOutboxChainTailJpaRepository;
  private final Tracer tracer;

  public DemoOutboxPersistenceAdapter(
      DemoOutboxEventJpaRepository demoOutboxEventJpaRepository,
      DemoOutboxChainTailJpaRepository demoOutboxChainTailJpaRepository,
      Tracer tracer) {
    this.demoOutboxEventJpaRepository = demoOutboxEventJpaRepository;
    this.demoOutboxChainTailJpaRepository = demoOutboxChainTailJpaRepository;
    this.tracer = tracer;
  }

  @Override
  public OutboxEvent save(OutboxEvent outboxEvent) {
    Span span = tracer.nextSpan().name(SPAN_NAME).start();
    try (Tracer.SpanInScope ignored = tracer.withSpan(span)) {
      return doSave(outboxEvent, span);
    } catch (RuntimeException e) {
      span.error(e);
      throw e;
    } finally {
      span.end();
    }
  }

  private OutboxEvent doSave(OutboxEvent outboxEvent, Span span) {
    demoOutboxChainTailJpaRepository.ensureInitialized(OutboxEvent.GENESIS_PREVIOUS_HASH);
    DemoOutboxChainTailEntity tail =
        demoOutboxChainTailJpaRepository
            .findForUpdate()
            .orElseThrow(() -> new IllegalStateException("outbox_chain_tail 초기화에 실패했습니다."));

    OutboxEvent chainedEvent =
        outboxEvent
            .chainedWith(tail.getLatestEntryHash())
            .withTraceContext(span.context().traceId(), span.context().spanId());

    DemoOutboxEventEntity entity = DemoOutboxEventEntity.fromDomain(chainedEvent);
    DemoOutboxEventEntity savedEntity = demoOutboxEventJpaRepository.save(entity);

    tail.updateLatestEntryHash(savedEntity.getEntryHash());

    return savedEntity.toDomain();
  }

  @Override
  public List<OutboxEvent> loadAllOrderedById() {
    return demoOutboxEventJpaRepository.findAllByOrderByIdAsc().stream()
        .map(DemoOutboxEventEntity::toDomain)
        .toList();
  }

  @Override
  public PagedResult<OutboxEvent> loadPage(int page, int size) {
    Page<DemoOutboxEventEntity> result =
        demoOutboxEventJpaRepository.findAllByOrderByIdAsc(PageRequest.of(page, size));
    List<OutboxEvent> items =
        result.getContent().stream().map(DemoOutboxEventEntity::toDomain).toList();
    return new PagedResult<>(items, result.getTotalElements());
  }

  public void deleteAllInBatch() {
    demoOutboxEventJpaRepository.deleteAllInBatch();
    demoOutboxChainTailJpaRepository.deleteAllInBatch();
  }

  @Override
  public void tamperPayload(Long outboxEventId, String corruptedPayload) {
    demoOutboxEventJpaRepository.tamperPayload(outboxEventId, corruptedPayload);
  }
}
