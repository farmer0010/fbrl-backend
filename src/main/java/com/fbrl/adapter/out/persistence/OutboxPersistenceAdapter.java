package com.fbrl.adapter.out.persistence;

import com.fbrl.application.port.out.LoadAllOutboxEventsPort;
import com.fbrl.application.port.out.LoadOutboxEventsPort;
import com.fbrl.application.port.out.PagedResult;
import com.fbrl.application.port.out.SaveOutboxEventPort;
import com.fbrl.domain.model.OutboxEvent;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxPersistenceAdapter
    implements SaveOutboxEventPort, LoadAllOutboxEventsPort, LoadOutboxEventsPort {
  private static final String SPAN_NAME = "outbox.save";

  private final OutboxEventJpaRepository outboxEventJpaRepository;
  private final OutboxChainTailJpaRepository outboxChainTailJpaRepository;
  private final Tracer tracer;

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
    outboxChainTailJpaRepository.ensureInitialized(OutboxEvent.GENESIS_PREVIOUS_HASH);
    OutboxChainTailJpaEntity tail =
        outboxChainTailJpaRepository
            .findForUpdate()
            .orElseThrow(() -> new IllegalStateException("outbox_chain_tail 초기화에 실패했습니다."));

    OutboxEvent chainedEvent =
        outboxEvent
            .chainedWith(tail.getLatestEntryHash())
            .withTraceContext(span.context().traceId(), span.context().spanId());

    OutboxEventJpaEntity entity = OutboxEventJpaEntity.fromDomain(chainedEvent);
    OutboxEventJpaEntity savedEntity = outboxEventJpaRepository.save(entity);

    tail.updateLatestEntryHash(savedEntity.getEntryHash());

    return savedEntity.toDomain();
  }

  @Override
  public List<OutboxEvent> loadAllOrderedById() {
    return outboxEventJpaRepository.findAllByOrderByIdAsc().stream()
        .map(OutboxEventJpaEntity::toDomain)
        .toList();
  }

  @Override
  public PagedResult<OutboxEvent> loadPage(int page, int size) {
    Page<OutboxEventJpaEntity> result =
        outboxEventJpaRepository.findAllByOrderByIdAsc(PageRequest.of(page, size));
    List<OutboxEvent> items =
        result.getContent().stream().map(OutboxEventJpaEntity::toDomain).toList();
    return new PagedResult<>(items, result.getTotalElements());
  }

  public void deleteAllInBatch() {
    outboxEventJpaRepository.deleteAllInBatch();
    outboxChainTailJpaRepository.deleteAllInBatch();
  }
}
