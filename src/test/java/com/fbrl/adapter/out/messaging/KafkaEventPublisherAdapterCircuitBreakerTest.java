package com.fbrl.adapter.out.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fbrl.domain.exception.EventPublishException;
import com.fbrl.domain.model.OutboxEvent;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.common.errors.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class KafkaEventPublisherAdapterCircuitBreakerTest {

  @MockitoBean private KafkaTemplate<String, String> kafkaTemplate;

  @Autowired private KafkaEventPublisherAdapter adapter;

  @Autowired private CircuitBreakerRegistry circuitBreakerRegistry;

  private CircuitBreaker circuitBreaker;

  @BeforeEach
  void setUp() {
    circuitBreaker = circuitBreakerRegistry.circuitBreaker("kafkaEventPublisher");
    circuitBreaker.reset();
  }

  @Test
  void shouldTransitionToOpenWhenFailureThresholdExceeded() {
    CompletableFuture<SendResult<String, String>> failedFuture = new CompletableFuture<>();
    failedFuture.completeExceptionally(new TimeoutException("Kafka broker unreachable"));
    given(kafkaTemplate.send(anyString(), anyString(), anyString())).willReturn(failedFuture);

    OutboxEvent event = OutboxEvent.create("ACCOUNT", "ACC-0001", "TRANSFER_COMPLETED", "{}");

    for (int i = 0; i < 10; i++) {
      assertThrows(EventPublishException.class, () -> adapter.publish(event));
    }

    assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
  }

  @Test
  void shouldBlockKafkaCallImmediatelyWhenCircuitBreakerIsOpen() {
    circuitBreaker.transitionToOpenState();

    OutboxEvent event = OutboxEvent.create("ACCOUNT", "ACC-0002", "TRANSFER_COMPLETED", "{}");

    assertThrows(EventPublishException.class, () -> adapter.publish(event));

    verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
  }
}
