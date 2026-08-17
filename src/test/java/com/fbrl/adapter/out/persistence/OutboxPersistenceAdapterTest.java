package com.fbrl.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.fbrl.application.port.out.PagedResult;
import com.fbrl.domain.model.OutboxEvent;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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
  }

  @Test
  @DisplayName("save()는 outbox.save span에서 얻은 traceId/spanId를 저장 대상 엔티티에 반영한다.")
  void save_stampsTraceContextFromSpan() {
    given(tracer.nextSpan()).willReturn(span);
    given(span.name(any())).willReturn(span);
    given(span.start()).willReturn(span);
    given(tracer.withSpan(span)).willReturn(spanInScope);
    given(span.context()).willReturn(traceContext);
    given(traceContext.traceId()).willReturn("a".repeat(32));
    given(traceContext.spanId()).willReturn("b".repeat(16));

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

  @Test
  @DisplayName("loadPage()는 Page 결과를 도메인 목록과 totalElements로 변환한다.")
  void loadPage_convertsPageToPagedResult() {
    OutboxEventJpaEntity entity =
        OutboxEventJpaEntity.fromDomain(
            OutboxEvent.create("Account", "111-111", "TRANSFER_COMPLETED", "{}")
                .chainedWith(OutboxEvent.GENESIS_PREVIOUS_HASH));
    Page<OutboxEventJpaEntity> page = new PageImpl<>(List.of(entity), PageRequest.of(0, 20), 1);
    given(outboxEventJpaRepository.findAllByOrderByIdAsc(any(Pageable.class))).willReturn(page);

    PagedResult<OutboxEvent> result = outboxPersistenceAdapter.loadPage(0, 20);

    assertThat(result.totalElements()).isEqualTo(1);
    assertThat(result.items()).hasSize(1);
    assertThat(result.items().get(0).getAggregateId()).isEqualTo("111-111");
  }
}
