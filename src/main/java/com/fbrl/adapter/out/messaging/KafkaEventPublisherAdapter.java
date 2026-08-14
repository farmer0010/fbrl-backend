package com.fbrl.adapter.out.messaging;

import com.fbrl.application.port.out.EventPublisherPort;
import com.fbrl.domain.exception.EventPublishException;
import com.fbrl.domain.model.OutboxEvent;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisherAdapter implements EventPublisherPort {

  private static final String TRANSFER_EVENTS_TOPIC = "transfer-events";

  private final KafkaTemplate<String, String> kafkaTemplate;

  @Override
  @CircuitBreaker(name = "kafkaEventPublisher", fallbackMethod = "publishFallback")
  public void publish(OutboxEvent outboxEvent) {
    try {
      kafkaTemplate
          .send(TRANSFER_EVENTS_TOPIC, outboxEvent.getAggregateId(), outboxEvent.getPayload())
          .get();
    } catch (Exception e) {
      throw new EventPublishException("Kafka 이벤트 발행 실패. eventId=" + outboxEvent.getId(), e);
    }
  }

  private void publishFallback(OutboxEvent outboxEvent, Throwable throwable) {
    if (throwable instanceof CallNotPermittedException) {
      log.warn("서킷 브레이커 OPEN 상태 - Kafka 발행 시도 자체가 차단됨. eventId={}", outboxEvent.getId());
      throw new EventPublishException(
          "서킷 브레이커 OPEN 상태(Kafka 장애 감지 중)로 발행이 차단되었습니다. eventId=" + outboxEvent.getId(), throwable);
    }
    log.warn("Kafka 이벤트 발행 실패. eventId={}", outboxEvent.getId(), throwable);
    throw new EventPublishException("Kafka 이벤트 발행 실패. eventId=" + outboxEvent.getId(), throwable);
  }
}
