package com.fbrl.adapter.in.scheduler;

import com.fbrl.application.port.in.PublishPendingOutboxEventsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class OutboxPollingScheduler {
  private final PublishPendingOutboxEventsUseCase publishPendingOutboxEventsUseCase;

  @Value("${outbox.polling.batch-size:100}")
  private int batchSize;

  @Scheduled(fixedDelayString = "${outbox.polling.fixed-delay-ms:3000}")
  public void pollAndPublish() {
    var result = publishPendingOutboxEventsUseCase.publishPendingEvents(batchSize);

    if (result.publishedCount() > 0 || result.failedCount() > 0) {
      log.debug("Outbox 폴링 트리거 완료: 발행 {}건, 실패 {}건", result.publishedCount(), result.failedCount());
    }
  }
}
