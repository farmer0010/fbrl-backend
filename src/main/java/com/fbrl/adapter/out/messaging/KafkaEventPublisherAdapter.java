package com.fbrl.adapter.out.messaging;

import com.fbrl.application.port.out.EventPublisherPort;
import com.fbrl.domain.model.OutboxEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaEventPublisherAdapter implements EventPublisherPort {

  private static final String TRANSFER_EVENTS_TOPIC = "transfer-events";

  private final KafkaTemplate<String, String> kafkaTemplate;

  @Override
  public void publish(OutboxEvent outboxEvent) {
    try {
      kafkaTemplate
          .send(TRANSFER_EVENTS_TOPIC, outboxEvent.getAggregateId(), outboxEvent.getPayload())
          .get();
    } catch (Exception e) {
      throw new EventPublishException("Kafka 이벤트 발행 실패. eventId=" + outboxEvent.getId(), e);
    }
  }
}
