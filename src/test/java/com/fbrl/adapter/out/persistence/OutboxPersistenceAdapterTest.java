package com.fbrl.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.fbrl.domain.model.OutboxEvent;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxPersistenceAdapter 단위 테스트")
class OutboxPersistenceAdapterTest {

  @Mock private OutboxEventJpaRepository outboxEventJpaRepository;
  @Mock private OutboxChainTailJpaRepository outboxChainTailJpaRepository;
  @Mock private Tracer tracer;
  @Mock private Span span;
  @Mock private TraceContext traceContext;
  @Mock private Tracer.SpanInScope spanInScope;

  private OutboxPersistenceAdapter outboxPersistenceAdapter;

  @BeforeEach
  void setUp() {
    outboxPersistenceAdapter =
        new OutboxPersistenceAdapter(
            outboxEventJpaRepository, outboxChainTailJpaRepository, tracer);

    given(tracer.nextSpan()).willReturn(span);
    given(span.name(any())).willReturn(span);
    given(span.start()).willReturn(span);
    given(tracer.withSpan(span)).willReturn(spanInScope);
    given(span.context()).willReturn(traceContext);
    given(traceContext.traceId()).willReturn("a".repeat(32));
    given(traceContext.spanId()).willReturn("b".repeat(16));
  }

  @Test
  @DisplayName("save()는 outbox.save span에서 얻은 traceId/spanId를 저장 대상 엔티티에 반영한다.")
  void save_stampsTraceContextFromSpan() {
    OutboxChainTailJpaEntity tail = new OutboxChainTailJpaEntity();
    tail.updateLatestEntryHash(OutboxEvent.GENESIS_PREVIOUS_HASH);
    given(outboxChainTailJpaRepository.findForUpdate()).willReturn(Optional.of(tail));
    given(outboxEventJpaRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

    OutboxEvent draft = OutboxEvent.create("Account", "111-111", "TRANSFER_COMPLETED", "{}");

    OutboxEvent saved = outboxPersistenceAdapter.save(draft);

    assertThat(saved.getTraceId()).isEqualTo("a".repeat(32));
    assertThat(saved.getSpanId()).isEqualTo("b".repeat(16));
    assertThat(saved.recomputeEntryHash()).isEqualTo(saved.getEntryHash());
  }
}
