package com.fbrl.adapter.out.persistence;

import com.fbrl.application.port.out.LoadPendingOutboxEventsPort;
import com.fbrl.application.port.out.SaveOutboxEventPort;
import com.fbrl.domain.model.OutboxEvent;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxPersistenceAdapter implements SaveOutboxEventPort, LoadPendingOutboxEventsPort {
  private final OutboxEventJpaRepository outboxEventJpaRepository;

  @Override
  public OutboxEvent save(OutboxEvent outboxEvent) {
    OutboxEventJpaEntity entity = OutboxEventJpaEntity.fromDomain(outboxEvent);
    OutboxEventJpaEntity savedEntity = outboxEventJpaRepository.save(entity);
    return savedEntity.toDomain();
  }

  @Override
  public List<OutboxEvent> loadPendingEvents(int limit) {
    return outboxEventJpaRepository
        .findByStatusOrderByCreatedAtAsc(OutboxEvent.Status.PENDING, PageRequest.of(0, limit))
        .stream()
        .map(OutboxEventJpaEntity::toDomain)
        .toList();
  }
}
