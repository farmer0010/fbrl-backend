package com.fbrl.adapter.in.kafka;

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
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayName("TransferEventConsumer 단위 테스트")
class TransferEventConsumerTest {

  private PayloadDeserializerPort payloadDeserializerPort;
  private ProcessTransferEventUseCase processTransferEventUseCase;
  private TransferEventConsumer consumer;

  @BeforeEach
  void setUp() {
    payloadDeserializerPort = Mockito.mock(PayloadDeserializerPort.class);
    processTransferEventUseCase = Mockito.mock(ProcessTransferEventUseCase.class);
    consumer = new TransferEventConsumer(payloadDeserializerPort, processTransferEventUseCase);
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

    consumer.consume(payload);

    verify(processTransferEventUseCase).handle(event);
  }

  @Test
  @DisplayName("역직렬화 실패 시 예외를 삼키지 않고 그대로 전파한다 (재시도/DLT 판단은 RetryTopicConfiguration이 담당).")
  void consume_deserializationFailure_propagatesExceptionWithoutSwallowing() {
    String malformedPayload = "broken-json";

    willThrow(new PayloadDeserializationException("역직렬화 실패", new RuntimeException("cause")))
        .given(payloadDeserializerPort)
        .deserialize(malformedPayload, TransferCompletedEvent.class);

    assertThatThrownBy(() -> consumer.consume(malformedPayload))
        .isInstanceOf(PayloadDeserializationException.class);

    verify(processTransferEventUseCase, never()).handle(any());
  }
}
