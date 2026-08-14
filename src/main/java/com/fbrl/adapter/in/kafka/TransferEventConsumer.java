package com.fbrl.adapter.in.kafka;

import com.fbrl.application.port.in.ProcessTransferEventUseCase;
import com.fbrl.application.port.out.PayloadDeserializerPort;
import com.fbrl.domain.event.TransferCompletedEvent;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransferEventConsumer {
  private static final Logger log = LoggerFactory.getLogger(TransferEventConsumer.class);
  private static final String SPAN_NAME = "transfer-event.consume";
  private static final String TRACEPARENT_HEADER = "traceparent";
  private static final String SAMPLED_TRACE_FLAGS = "01";

  private final PayloadDeserializerPort payloadDeserializerPort;
  private final ProcessTransferEventUseCase processTransferEventUseCase;
  private final Tracer tracer;
  private final Propagator propagator;

  @KafkaListener(topics = "transfer-events", groupId = "transfer-event-processor")
  public void consume(
      @Payload String payload,
      @Header(value = "trace_id", required = false) String traceId,
      @Header(value = "span_id", required = false) String spanId) {
    Span span = startConsumerSpan(traceId, spanId);
    try (Tracer.SpanInScope ignored = tracer.withSpan(span)) {
      TransferCompletedEvent event =
          payloadDeserializerPort.deserialize(payload, TransferCompletedEvent.class);

      processTransferEventUseCase.handle(event);
    } catch (RuntimeException e) {
      span.error(e);
      throw e;
    } finally {
      span.end();
    }
  }

  private Span startConsumerSpan(String traceId, String spanId) {
    if (traceId == null || spanId == null) {
      return tracer.nextSpan().name(SPAN_NAME).start();
    }
    Map<String, String> carrier =
        Map.of(TRACEPARENT_HEADER, "00-" + traceId + "-" + spanId + "-" + SAMPLED_TRACE_FLAGS);
    return propagator.extract(carrier, Map::get).name(SPAN_NAME).start();
  }

  @DltHandler
  public void handleDlt(
      @Payload String payload,
      @Header(KafkaHeaders.EXCEPTION_MESSAGE) String exceptionMessage,
      @Header(KafkaHeaders.ORIGINAL_TOPIC) String originalTopic,
      @Header(value = "trace_id", required = false) String traceId) {
    log.error(
        "DLT 도착 - 원본 토픽={}, 실패 사유={}, traceId={}, payload={}",
        originalTopic,
        exceptionMessage,
        traceId,
        payload);
  }
}
