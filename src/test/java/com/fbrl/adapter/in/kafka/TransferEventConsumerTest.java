package com.fbrl.adapter.in.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fbrl.application.port.in.ProcessTransferEventUseCase;
import com.fbrl.application.port.out.PayloadDeserializerPort;
import com.fbrl.domain.event.TransferCompletedEvent;
import com.fbrl.domain.exception.PayloadDeserializationException;
import com.fbrl.domain.model.Money;
import io.micrometer.tracing.otel.bridge.ArrayListSpanProcessor;
import io.micrometer.tracing.otel.bridge.OtelBaggageManager;
import io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext;
import io.micrometer.tracing.otel.bridge.OtelPropagator;
import io.micrometer.tracing.otel.bridge.OtelTracer;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayName("TransferEventConsumer 단위 테스트")
class TransferEventConsumerTest {

  private PayloadDeserializerPort payloadDeserializerPort;
  private ProcessTransferEventUseCase processTransferEventUseCase;
  private TransferEventConsumer consumer;

  private ArrayListSpanProcessor spanProcessor;
  private OtelTracer otelTracer;
  private OtelPropagator otelPropagator;

  @BeforeEach
  void setUp() {
    payloadDeserializerPort = Mockito.mock(PayloadDeserializerPort.class);
    processTransferEventUseCase = Mockito.mock(ProcessTransferEventUseCase.class);

    spanProcessor = new ArrayListSpanProcessor();
    OtelCurrentTraceContext currentTraceContext = new OtelCurrentTraceContext();
    SdkTracerProvider tracerProvider =
        SdkTracerProvider.builder().addSpanProcessor(spanProcessor).build();

    OpenTelemetry openTelemetry =
        OpenTelemetrySdk.builder()
            .setTracerProvider(tracerProvider)
            .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
            .build();

    otelTracer =
        new OtelTracer(
            openTelemetry.getTracer("test"),
            currentTraceContext,
            event -> {},
            new OtelBaggageManager(currentTraceContext, List.of(), List.of()));

    otelPropagator =
        new OtelPropagator(openTelemetry.getPropagators(), openTelemetry.getTracer("test"));

    consumer =
        new TransferEventConsumer(
            payloadDeserializerPort, processTransferEventUseCase, otelTracer, otelPropagator);
  }

  @Test
  @DisplayName("정상 역직렬화되면 ProcessTransferEventUseCase.handle()이 호출된다.")
  void consume_success_callsUseCase() {
    String payload = "{\"dummy\":\"payload\"}";
    TransferCompletedEvent event =
        new TransferCompletedEvent(
            "111-111", "222-222", Money.of(BigDecimal.valueOf(10_000)), Instant.now());

    given(payloadDeserializerPort.deserialize(payload, TransferCompletedEvent.class))
        .willReturn(event);

    consumer.consume(payload, null, null);

    verify(processTransferEventUseCase).handle(event);
  }

  @Test
  @DisplayName("역직렬화 실패 시 예외를 삼키지 않고 그대로 전파한다 (재시도/DLT 판단은 RetryTopicConfiguration이 담당).")
  void consume_deserializationFailure_propagatesExceptionWithoutSwallowing() {
    String malformedPayload = "broken-json";

    willThrow(new PayloadDeserializationException("역직렬화 실패", new RuntimeException("cause")))
        .given(payloadDeserializerPort)
        .deserialize(malformedPayload, TransferCompletedEvent.class);

    assertThatThrownBy(() -> consumer.consume(malformedPayload, null, null))
        .isInstanceOf(PayloadDeserializationException.class);

    verify(processTransferEventUseCase, never()).handle(any());
  }

  @Test
  @DisplayName("trace_id/span_id 헤더가 있으면 해당 traceId를 부모로 하는 span에서 처리한다.")
  void consume_withTraceHeaders_joinsSameTrace() {
    String payload = "{\"dummy\":\"payload\"}";
    given(payloadDeserializerPort.deserialize(payload, TransferCompletedEvent.class))
        .willReturn(
            new TransferCompletedEvent(
                "111-111", "222-222", Money.of(BigDecimal.valueOf(10_000)), Instant.now()));

    io.micrometer.tracing.Span producerSpan = otelTracer.nextSpan().name("outbox.save").start();
    String traceId = producerSpan.context().traceId();
    String spanId = producerSpan.context().spanId();
    producerSpan.end();

    consumer.consume(payload, traceId, spanId);

    List<SpanData> spans = spanProcessor.spans().stream().toList();
    assertThat(spans).hasSize(2);
    assertThat(spans).extracting(SpanData::getTraceId).containsOnly(traceId);
  }
}
