package com.fbrl.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fbrl.application.port.in.PublishPendingOutboxEventsUseCase.PublishResult;
import com.fbrl.application.port.out.EventPublisherPort;
import com.fbrl.application.port.out.LoadPendingOutboxEventsPort;
import com.fbrl.application.port.out.SaveOutboxEventPort;
import com.fbrl.domain.model.OutboxEvent;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("PublishPendingOutboxEventsService 단위 테스트")
class PublishPendingOutboxEventsServiceTest {

  @Mock private LoadPendingOutboxEventsPort loadPendingOutboxEventsPort;

  @Mock private EventPublisherPort eventPublisherPort;

  @Mock private SaveOutboxEventPort saveOutboxEventPort;

  @InjectMocks private PublishPendingOutboxEventsService publishPendingOutboxEventsService;

  private OutboxEvent pendingEvent(Long id) {
    return new OutboxEvent(
        id,
        "Account",
        "111-111",
        "TransferCompleted",
        "{\"dummy\":\"payload\"}",
        OutboxEvent.Status.PENDING,
        Instant.now());
  }

  @Test
  @DisplayName("PENDING 이벤트가 없으면 발행/저장 로직을 전혀 호출하지 않고 빈 결과를 반환한다.")
  void noPendingEvents() {
    given(loadPendingOutboxEventsPort.loadPendingEvents(100)).willReturn(List.of());

    PublishResult result = publishPendingOutboxEventsService.publishPendingEvents(100);

    assertThat(result.publishedCount()).isEqualTo(0);
    assertThat(result.failedCount()).isEqualTo(0);

    verify(eventPublisherPort, never()).publish(any());
    verify(saveOutboxEventPort, never()).save(any());
  }

  @Test
  @DisplayName("발행 성공 시 Kafka 발행이 먼저 일어난 뒤 SENT 상태로 저장된다.")
  void publishSuccess() {
    OutboxEvent event = pendingEvent(1L);

    given(loadPendingOutboxEventsPort.loadPendingEvents(100)).willReturn(List.of(event));

    PublishResult result = publishPendingOutboxEventsService.publishPendingEvents(100);

    assertThat(result.publishedCount()).isEqualTo(1);
    assertThat(result.failedCount()).isEqualTo(0);
    assertThat(event.getStatus()).isEqualTo(OutboxEvent.Status.SENT);

    InOrder inOrder = Mockito.inOrder(eventPublisherPort, saveOutboxEventPort);
    inOrder.verify(eventPublisherPort).publish(event);
    inOrder.verify(saveOutboxEventPort).save(event);
  }

  @Test
  @DisplayName("일부 이벤트 발행이 실패해도 나머지 이벤트는 계속 처리된다.")
  void publishPartialFailure() {
    OutboxEvent failingEvent = pendingEvent(1L);
    OutboxEvent succeedingEvent = pendingEvent(2L);

    given(loadPendingOutboxEventsPort.loadPendingEvents(100))
        .willReturn(List.of(failingEvent, succeedingEvent));

    willThrow(new RuntimeException("Kafka 브로커 응답 없음"))
        .given(eventPublisherPort)
        .publish(failingEvent);

    PublishResult result = publishPendingOutboxEventsService.publishPendingEvents(100);

    assertThat(result.publishedCount()).isEqualTo(1);
    assertThat(result.failedCount()).isEqualTo(1);
    assertThat(failingEvent.getStatus()).isEqualTo(OutboxEvent.Status.FAILED);
    assertThat(succeedingEvent.getStatus()).isEqualTo(OutboxEvent.Status.SENT);

    verify(eventPublisherPort).publish(succeedingEvent);
    verify(saveOutboxEventPort).save(failingEvent);
    verify(saveOutboxEventPort).save(succeedingEvent);
  }
}
