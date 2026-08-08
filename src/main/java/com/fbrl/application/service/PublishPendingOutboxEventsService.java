package com.fbrl.application.service;

import com.fbrl.application.port.in.PublishPendingOutboxEventsUseCase;
import com.fbrl.application.port.out.EventPublisherPort;
import com.fbrl.application.port.out.LoadPendingOutboxEventsPort;
import com.fbrl.application.port.out.SaveOutboxEventPort;
import com.fbrl.domain.model.OutboxEvent;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublishPendingOutboxEventsService implements PublishPendingOutboxEventsUseCase {

  private final LoadPendingOutboxEventsPort loadPendingOutboxEventsPort;
  private final EventPublisherPort eventPublisherPort;
  private final SaveOutboxEventPort saveOutboxEventPort;

  @Override
  public PublishResult publishPendingEvents(int limit) {
    List<OutboxEvent> pendingEvents = loadPendingOutboxEventsPort.loadPendingEvents(limit);

    if (pendingEvents.isEmpty()) {
      return PublishResult.empty();
    }

    int publishedCount = 0;
    int failedCount = 0;

    for (OutboxEvent event : pendingEvents) {
      if (publishOne(event)) {
        publishedCount++;
      } else {
        failedCount++;
      }
    }

    log.info(
        "Outbox 폴링 사이클 완료: 총 {}건 중 발행 성공 {}건, 실패 {}건",
        pendingEvents.size(),
        publishedCount,
        failedCount);

    return new PublishResult(publishedCount, failedCount);
  }

  private boolean publishOne(OutboxEvent event) {
    try {
      eventPublisherPort.publish(event);
      event.markAsSent();
      saveOutboxEventPort.save(event);
      return true;
    } catch (Exception e) {
      log.warn(
          "Outbox 이벤트 발행 실패. eventId={}, eventType={}", event.getId(), event.getEventType(), e);
      event.markAsFailed();
      saveOutboxEventPort.save(event);
      return false;
    }
  }
}
