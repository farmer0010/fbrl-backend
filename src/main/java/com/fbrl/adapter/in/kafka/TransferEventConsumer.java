package com.fbrl.adapter.in.kafka;

import com.fbrl.application.port.in.ProcessTransferEventUseCase;
import com.fbrl.application.port.out.PayloadDeserializerPort;
import com.fbrl.domain.event.TransferCompletedEvent;
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

  private final PayloadDeserializerPort payloadDeserializerPort;
  private final ProcessTransferEventUseCase processTransferEventUseCase;

  @KafkaListener(topics = "transfer-events", groupId = "transfer-event-processor")
  public void consume(@Payload String payload) {
    TransferCompletedEvent event =
        payloadDeserializerPort.deserialize(payload, TransferCompletedEvent.class);

    processTransferEventUseCase.handle(event);
  }

  @DltHandler
  public void handleDlt(
      @Payload String payload,
      @Header(KafkaHeaders.EXCEPTION_MESSAGE) String exceptionMessage,
      @Header(KafkaHeaders.ORIGINAL_TOPIC) String originalTopic) {
    log.error("DLT 도착 - 원본 토픽={}, 실패 사유={}, payload={}", originalTopic, exceptionMessage, payload);
  }
}
